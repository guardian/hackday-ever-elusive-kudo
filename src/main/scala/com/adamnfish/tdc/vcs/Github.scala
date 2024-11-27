package com.adamnfish.tdc.vcs

import cats.MonadThrow
import cats.effect.kernel.Async
import cats.effect.{Concurrent, Resource}
import cats.syntax.all.*
import com.adamnfish.tdc.vcs.VcsInformation.DocsFile
import fs2.io.net.Network
import github4s.GithubClient
import github4s.algebras.GithubAPIs
import github4s.domain.{BlobContent, TreeDataResult}
import org.http4s.ember.client.EmberClientBuilder

import java.util.Base64

class Github[F[_]: Concurrent: Async: Network: MonadThrow](
    githubAPIs: GithubAPIs[F]
) extends VcsInformation[F] {

  override def repoDocs(
      owner: String,
      repositoryName: String,
      vcsRef: String = "main"
  ): F[List[DocsFile]] = {
    for {
      treeItems <- lookupRepoTree(owner, repositoryName, vcsRef)
      docsTreeItems = treeItems.filter(Github.isDoc)
      docsBlobsAndPaths <- Async[F].parTraverseN(10)(docsTreeItems) { tdr =>
        // lookup each docs blob by sha, using the GitHub API
        // we want to preserve the path, so we attach the path to the contents
        lookupBlob(owner, repositoryName, tdr.sha).map((tdr.path, _))
      }
      docsAndPaths <- docsBlobsAndPaths.traverse(Github.blobToDocsFile.tupled)
    } yield docsAndPaths
  }

  /** Fetches the full content tree for the repository.
    *
    * This tree contains the filesystem without the actual contents.
    */
  def lookupRepoTree(
      owner: String,
      repositoryName: String,
      gitRef: String
  ): F[List[TreeDataResult]] =
    for {
      response <- githubAPIs.gitData.getTree(
        owner,
        repositoryName,
        gitRef,
        recursive = true
      )
      tree <- MonadThrow[F].fromEither(response.result)
    } yield tree.tree

  /** Fetches a file's contents.
    *
    * We look up a specific file (and version) by its Git sha, and return its
    * contents.
    */
  def lookupBlob(
      owner: String,
      repositoryName: String,
      blobSha: String
  ): F[BlobContent] = {
    for {
      response <- githubAPIs.gitData.getBlob(owner, repositoryName, blobSha)
      blob <- MonadThrow[F].fromEither(response.result)
    } yield blob
  }
}
object Github {
  def create[F[_]: Concurrent: Async: Network: MonadThrow](
      apiKey: String
  ): Resource[F, Github[F]] = {
    for {
      client <- EmberClientBuilder
        .default[F]
        .build
      githubApis = GithubClient(client, Some(apiKey))
    } yield Github(githubApis)
  }

  def isDoc(treeDataResult: TreeDataResult): Boolean = {
    treeDataResult.`type` == "blob" &&
    VcsInformation.isDocPath(treeDataResult.path)
  }

  def blobToDocsFile[F[_] : MonadThrow](path: String, blobContent: BlobContent): F[DocsFile] = {
    (blobContent.encoding match {
      case Some("base64") =>
        MonadThrow[F].fromOption(
          blobContent.content.map(c => new String(Base64.getMimeDecoder.decode(c))),
          new RuntimeException(s"Empty file at path `$path`")
        )
      case Some("utf-8") =>
        MonadThrow[F].fromOption(
          blobContent.content,
          new RuntimeException(s"Empty file at path `$path`")
        )
      case encoding =>
        MonadThrow[F].raiseError(
          new RuntimeException(s"Unsupported $encoding for file at path `$path` (gitsha:${blobContent.sha})")
        )
    }).map(DocsFile(path, _))
  }
}