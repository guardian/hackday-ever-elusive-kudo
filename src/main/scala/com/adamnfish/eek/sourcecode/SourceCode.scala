package com.adamnfish.eek.sourcecode

import cats.*
import cats.syntax.all.*
import com.adamnfish.eek.sourcecode.SourceCode.DocsFile

trait SourceCode[F[_]] {
  def repoDocs: F[List[DocsFile]]

  def summary: String
}
object SourceCode {
  case class DocsFile(path: String, content: String)

  /** A collection of checks to perform on a path. If any pass, we consider the
    * object to be "documentation"
    */
  private val docsPathPredicates: List[String => Boolean] = {
    val extensions = List(
      ".md",
      ".rst",
      ".txt",
      ".adoc"
    )
    val filePredicates = for {
      file <- List("readme", "faq")
      extension <- extensions
      predicates <- List(
        (path: String) => path.toLowerCase == file,
        (path: String) => path.toLowerCase == s"$file$extension",
        // TODO: does treeDataItem.path include the root?
        (path: String) => path.toLowerCase.endsWith(s"/$file$extension")
      )
    } yield predicates
    val docsDirPredicates =
      List((path: String) =>
        path.startsWith("docs/") && extensions.exists(path.endsWith)
      )
    docsDirPredicates ++ filePredicates
  }

  def isDocPath(path: String): Boolean = {
    docsPathPredicates.exists(p => p(path))
  }
}
