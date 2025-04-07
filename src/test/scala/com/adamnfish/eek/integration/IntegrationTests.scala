package com.adamnfish.eek.integration

import cats.syntax.all.*
import cats.effect.{IO, Ref}
import cats.effect.kernel.Sync
import cats.{Applicative, Functor, Show}
import com.adamnfish.eek.Main
import com.adamnfish.eek.Main.{AppComponents, AppFlags}
import com.adamnfish.eek.Printer
import com.adamnfish.eek.sourcecode.SourceCode
import com.adamnfish.eek.sourcecode.SourceCode.DocsFile
import com.adamnfish.eek.docs.DocsEvaluator
import com.adamnfish.eek.docs.DocsEvaluator.DocsEvaluation
import com.adamnfish.eek.Args
import com.adamnfish.eek.SourceCodeArgs.*
import com.adamnfish.eek.DocsEvaluatorArgs.*

import scala.Console.{CYAN, RESET}
import munit.CatsEffectSuite

class IntegrationTests extends CatsEffectSuite {
  test("Prints the evaluation returned by the DocsEvaluator") {
    // the dummy CLI arguments for our test invocation
    val flags = AppFlags(verbose = false)

    // mock data for our test components
    val docsFiles = List(DocsFile("README.md", "Example content"))
    val evaluation = DocsEvaluation.empty.copy()

    for {
      // create test versions of our dependencies, with mocked data
      printer <- TestPrinter.make[IO]
      appComponents = AppComponents[IO](
        sourceCode = new TestVcsInfo[IO]("test VCS info", docsFiles),
        docsEvaluator = new TestDocsEvaluator[IO](
          evaluation,
          "The thinking behind the evaluation (will not be included in output this time)"
        ),
        printer = printer,
        flags
      )

      // run the app with our test components and the CLI args we set up above
      _ <- Main.app(appComponents)

      // the test printer captured all the "println" content, let's compare that to our expected value
      printed <- printer.getPrinted
      expected =
        // format: off
        s"""Evaluating the following docs files: ${CYAN}README.md${RESET}
           |${DocsEvaluation.formatDocsEvaluation("test VCS info", evaluation)}""".stripMargin
        // format: on
    } yield assertEquals(printed.trim, expected.trim)
  }

  test("Also prints the Evaluator's 'thinking' when in verbose mode".ignore) {}

  test(
    "Provides the VCS components' documentation files to the DocsEvaluator".ignore
  ) {}

  // Mock services for tests, just return the data they are instantiated with

  /** This VcsInformation implementation just returns the provided docs files.
    */
  class TestVcsInfo[F[_]: Applicative](label: String, docs: List[DocsFile])
      extends SourceCode[F]:
    override def repoDocs: F[List[DocsFile]] =
      Applicative[F].pure(docs)
    override def summary: String = label

  /** Dummy implementation that always returns the provided evaluation /
    * thoughts
    */
  class TestDocsEvaluator[F[_]: Applicative](
      evaluation: DocsEvaluation,
      thoughts: String
  ) extends DocsEvaluator[F]:
    override def evaluateDocs(
        allDocs: List[DocsFile]
    ): F[(DocsEvaluator.DocsEvaluation, String)] =
      Applicative[F].pure((evaluation, thoughts))

  /** A printer that captures all the output that would have been printed to the
    * console. This allows us to test the contents of the program's output in an
    * integration test.
    */
  class TestPrinter[F[_]: Sync](printed: Ref[F, String]) extends Printer[F]:
    override def println[A](a: A)(implicit S: Show[A]): F[Unit] =
      printed.update { prev =>
        s"$prev${S.show(a)}\n"
      }
    def getPrinted: F[String] = printed.get
  object TestPrinter:
    def make[F[_]: Sync: Functor]: F[TestPrinter[F]] =
      Ref.of[F, String]("").map(new TestPrinter[F](_))
}
