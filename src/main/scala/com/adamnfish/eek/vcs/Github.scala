package com.adamnfish.eek.vcs

import cats.MonadThrow
import cats.effect.kernel.Async
import cats.effect.{Concurrent, Resource}
import cats.syntax.all.*
import com.adamnfish.eek.vcs.VcsInformation.DocsFile
import fs2.io.net.Network
import github4s.GithubClient
import github4s.algebras.GithubAPIs
import github4s.domain.{BlobContent, TreeDataResult}
import org.http4s.ember.client.EmberClientBuilder
import org.typelevel.log4cats.*

import java.util.Base64

class Github[F[_]: Concurrent: Async: Network: MonadThrow: LoggerFactory](
    githubAPIs: GithubAPIs[F],
    owner: String,
    repositoryName: String
) extends VcsInformation[F] {
  private val logger = LoggerFactory[F].getLogger

  override def repoDocs(vcsRef: String = "main"): F[List[DocsFile]] = {
    for {
      treeItems <- lookupRepoTree(owner, repositoryName, vcsRef)
      docsTreeItems = treeItems.filter(Github.isDoc)
      _ <- logger.debug(s"""${docsTreeItems.length} docs items""")
      docsBlobsAndPaths <- Async[F].parTraverseN(10)(docsTreeItems) { tdr =>
        // lookup each docs blob by sha, using the GitHub API
        // we want to preserve the path, so we attach the path to the contents
        lookupBlob(owner, repositoryName, tdr.sha).map((tdr.path, _))
      }
      docsAndPaths <- docsBlobsAndPaths.traverse(Github.blobToDocsFile.tupled)
      _ <- logger.trace(
        s"""Docs found
           |${docsAndPaths
            .map(docsFile => s"${docsFile.path}\n${docsFile.content}")
            .mkString("\n-----------\n")}""".stripMargin
      )
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
      _ <- logger.trace(
        s"""lookupRepoTree response: ${formatHeaders(response.headers)}"""
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
      _ <- logger.debug(
        s"""lookupBlob response: ${formatHeaders(response.headers)}"""
      )
      blob <- MonadThrow[F].fromEither(response.result)
    } yield blob
  }

  def formatHeaders(headers: Map[String, String]): String =
    headers.map { case (k, v) => s"$k $v" }.mkString(";")
}
object Github {
  def create[F[_]: Concurrent: Async: Network: MonadThrow: LoggerFactory](
      apiKey: String,
      owner: String,
      repositoryName: String
  ): Resource[F, Github[F]] = {
    for {
      client <- EmberClientBuilder
        .default[F]
        .build
      githubApis = GithubClient(client, Some(apiKey))
    } yield Github(githubApis, owner, repositoryName)
  }

  def isDoc(treeDataResult: TreeDataResult): Boolean = {
    treeDataResult.`type` == "blob" &&
    VcsInformation.isDocPath(treeDataResult.path)
  }

  def blobToDocsFile[F[_]: MonadThrow](
      path: String,
      blobContent: BlobContent
  ): F[DocsFile] = {
    (blobContent.encoding match {
      case Some("base64") =>
        MonadThrow[F].fromOption(
          blobContent.content.map(c =>
            new String(Base64.getMimeDecoder.decode(c))
          ),
          new RuntimeException(s"Empty file at path `$path`")
        )
      case Some("utf-8") =>
        MonadThrow[F].fromOption(
          blobContent.content,
          new RuntimeException(s"Empty file at path `$path`")
        )
      case encoding =>
        MonadThrow[F].raiseError(
          new RuntimeException(
            s"Unsupported $encoding for file at path `$path` (gitsha:${blobContent.sha})"
          )
        )
    }).map(DocsFile(path, _))
  }
}
