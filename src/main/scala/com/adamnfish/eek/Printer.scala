package com.adamnfish.eek

import cats.*
import cats.effect.std.Console
import cats.effect.{Resource, Sync}

import java.io.{File, PrintWriter}
import scala.Console.{BOLD, CYAN, RESET}

/** Printer allows us to support multiple ways for us to handle "output":
  *   - write to the console
  *   - write to a MarkdownFile
  *   - store the written content so it can be checked in integration tests
  *
  * It includes a formatter because different modes need to be laid out
  * differently. The Console supports colour and weight via control codes, while
  * Markdown uses visible characters to denote format (e.g. *)
  */
trait Printer[F[_]] {
  def println[A](a: A)(implicit S: Show[A] = Show.fromToString[A]): F[Unit]
  def formatter: Formatter
}

/** Prints the content to the terminal.
  */
class ConsolePrinter[F[_]: Console] extends Printer[F] {
  override def println[A: Show](a: A): F[Unit] = {
    Console[F].println(a)
  }
  override val formatter: Formatter = ConsoleFormatter
}
object ConsolePrinter {
  def apply[F[_]: Console] = new ConsolePrinter[F]
}

/** Prints our content to a markdown file.
  *
  * This is created using `MarkdownFilePrinter(<file>)` so that the output file
  * can be safely opened and closed.
  */
class MarkdownFilePrinter[F[_]: Sync] private (writer: PrintWriter)
    extends Printer[F] {
  override def println[A: Show](a: A): F[Unit] =
    Sync[F].blocking(writer.println(Show[A].show(a)))
  override val formatter: Formatter = MarkdownFormatter
}
object MarkdownFilePrinter {
  def apply[F[_]: Sync](file: File): Resource[F, Printer[F]] = {
    Resource
      .make {
        Sync[F].blocking(new PrintWriter(file))
      } { writer =>
        Sync[F].blocking(writer.close())
      }
      .map(new MarkdownFilePrinter(_))
  }
}

/** We support writing to the console and to a markdown file, and each of these
  * have different ways to express formatting.
  */
trait Formatter {

  /** Represent "bold" content.
    */
  def emphasised(str: String): String

  /** Content distinct from normal text without being emphasised.
    */
  def informative(str: String): String

  /** Markdown is not whitespace sensitive, so we'll need to be explicit about
    * indentation there.
    */
  def indent: String
}
object ConsoleFormatter extends Formatter {
  override def emphasised(str: String): String = s"${BOLD}$str${RESET}"
  override def informative(str: String): String = s"${CYAN}$str${RESET}"
  override def indent: String = "    "
}
object MarkdownFormatter extends Formatter {
  override def emphasised(str: String): String = s"**$str**"
  override def informative(str: String): String = s"*$str*"
  override def indent: String = "&nbsp;&nbsp;&nbsp;&nbsp;"
}
