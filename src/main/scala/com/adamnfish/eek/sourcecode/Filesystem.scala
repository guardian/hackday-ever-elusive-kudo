package com.adamnfish.eek.sourcecode

import cats.*
import cats.data.*
import cats.syntax.all.*
import cats.effect.{Concurrent, IO}
import cats.effect.kernel.Async
import cats.effect.std.Console
import com.adamnfish.eek.sourcecode.Filesystem.docsForPath
import fs2.io.file.{Files, Path}
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.Logger

class Filesystem[F[_]: {MonadThrow, Concurrent, Files, Console}](sourceCodeRoot: String)
    extends SourceCode[F] {

  /** Loads VCS information from the source code at the provided filesystem
    * location.
    */
  override def repoDocs: F[List[SourceCode.DocsFile]] = {
    val rootPath = Path(sourceCodeRoot)
    Files[F]
      .walk(rootPath)
      .filter(Filesystem.filterPath(_, rootPath))
      .evalTap(Console[F].println)
      .evalMap(docsForPath(_, rootPath))
      .compile
      .toList
  }

  override def summary: String = sourceCodeRoot
}
object Filesystem {
  def docsForPath[F[_]: {Concurrent, MonadThrow, Files}](
      path: Path,
      rootPath: Path
  ): F[SourceCode.DocsFile] = {
    for {
      content <- Files[F]
        .readUtf8(path)
        .compile
        .lastOrError
      relativePath = rootPath.relativize(path).toString
    } yield SourceCode.DocsFile(relativePath, content)
  }

  def filterPath(path: Path, rootPath: Path): Boolean = {
    val relativePath = rootPath.relativize(path).toString
    SourceCode.isDocPath(relativePath)
  }
}
