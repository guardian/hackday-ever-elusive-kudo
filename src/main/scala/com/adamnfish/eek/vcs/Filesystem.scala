package com.adamnfish.eek.vcs

import cats.*
import cats.data.*
import cats.syntax.all.*
import cats.effect.{Concurrent, IO}
import cats.effect.kernel.Async
import cats.effect.std.Console
import com.adamnfish.eek.vcs.Filesystem.docsForPath
import fs2.io.file.{Files, Path}
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.Logger

class Filesystem[F[_]: MonadThrow: Concurrent: Files: Console]
    extends VcsInformation[F] {

  /** Loads VCS information from the source code at the provided filesystem
    * location.
    */
  override def repoDocs(vcsRef: String): F[List[VcsInformation.DocsFile]] = {
    val rootPath = Path(vcsRef)
    Files[F]
      .walk(rootPath)
      .filter(Filesystem.filterPath(_, rootPath))
      .evalTap(Console[F].println)
      .evalMap(docsForPath(_, rootPath))
      .compile
      .toList
  }
}
object Filesystem {
  def docsForPath[F[_]: Concurrent: MonadThrow: Files](
      path: Path,
      rootPath: Path
  ): F[VcsInformation.DocsFile] = {
    for {
      content <- Files[F]
        .readUtf8(path)
        .compile
        .lastOrError
      relativePath = rootPath.relativize(path).toString
    } yield VcsInformation.DocsFile(relativePath, content)
  }

  def filterPath(path: Path, rootPath: Path): Boolean = {
    val relativePath = rootPath.relativize(path).toString
    VcsInformation.isDocPath(relativePath)
  }
}
