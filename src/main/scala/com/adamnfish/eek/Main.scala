package com.adamnfish.eek

import cats.effect.{ExitCode, IO, IOApp, Resource}
import cats.syntax.all.*
import com.adamnfish.eek.docs.DocsEvaluator.DocsEvaluation
import com.adamnfish.eek.docs.DocsEvaluator.DocsEvaluation.formatDocsEvaluation
import com.adamnfish.eek.docs.{AwsBedrockDocsEvaluator, DocsEvaluator}
import com.adamnfish.eek.sourcecode.{Filesystem, Github, SourceCode}
import fs2.io.net.Network
import org.typelevel.log4cats.*
import org.typelevel.log4cats.slf4j.Slf4jFactory
import software.amazon.awssdk.regions.Region

import scala.Console.*
import scala.util.control.NonFatal

object Main extends IOApp {

  /** CLI entrypoint
    *   - parses CLI arguments
    *   - assembles the selected components
    *   - then runs the program
    */
  override def run(args: List[String]): IO[ExitCode] =
    Args
      .parse[IO](args)
      .flatMap {
        case Right(args) =>
          makeAppComponents(args)
            .use(app)
        case Left(exitCode) =>
          IO.pure(ExitCode.Success)
      }

  /** The program logic, this is the integration-tested application code.
    */
  def app(appComponents: AppComponents[IO]): IO[ExitCode] = {
    given LoggerFactory[IO] = Slf4jFactory.create[IO]
    val logger = LoggerFactory.getLogger[IO]

    val program = for {
      // lookup all the documentation files we'll be evaluating
      docs <- appComponents.sourceCode.repoDocs
      _ <- appComponents.printer.println(
        s"Evaluated docs files: ${docs.map(df => s"${CYAN}${df.path}${RESET}").mkString(", ")}"
      )
      // send the documentation files to our LLM for evaluation
      (docsEvaluation, thoughts) <- appComponents.docsEvaluator.evaluateDocs(
        docs
      )
      // print the evaluator's thought process in verbose mode
      _ <-
        if (appComponents.appFlags.verbose)
          appComponents.printer.println(
            DocsEvaluation
              .formatThoughts(thoughts, appComponents.printer.formatter)
          )
        else IO.unit
      // print the evaluation result
      _ <- appComponents.printer.println(
        formatDocsEvaluation(
          appComponents.sourceCode.summary,
          docsEvaluation,
          appComponents.printer.formatter
        )
      )
    } yield ExitCode.Success
    // if we hit any errors, print a message and log the details
    program.recoverWith { case NonFatal(err) =>
      for {
        _ <- logger.error(err)("Unhandled exception")
        _ <- IO.consoleForIO.errorln(
          "Unexpected error - check logs for full details"
        )
        _ <- IO.consoleForIO.errorln(err.getMessage)
      } yield ExitCode.Error
    }
  }

  /** Uses the provided arguments to setup the application's components. There
    * are three areas of the program that can have their implementation swapped
    * out:
    *
    * * Source code provider
    *   - GitHub with the repository reference and an optional git reference
    *     (defaults to main)
    *   - A local directory, as specified
    * * Documentation evaluator
    *   - AWS Bedrock, configured with an AWS credentials profile and optional
    *     region
    * * Output
    *   - Console (with terminal colours)
    *   - Markdown File (with Markdown formatting)
    */
  def makeAppComponents(args: Args): Resource[IO, AppComponents[IO]] =
    given LoggerFactory[IO] = Slf4jFactory.create[IO]
    for {
      githubApis: SourceCode[IO] <-
        args.sourceCodeArgs match
          case SourceCodeArgs.GitHubArgs(owner, repo, gitRef) =>
            Github.create[IO](owner, repo, gitRef)
          case SourceCodeArgs.FilesystemArgs(path) =>
            Resource.pure(new Filesystem[IO](path.getAbsolutePath))
          case SourceCodeArgs.SourceCodeArgsNotSpecified =>
            IO.raiseError(
              new RuntimeException(
                "Unexpected state - no source code provided"
              )
            ).toResource

      docsEvaluator <-
        args.docsEvaluatorArgs match
          case DocsEvaluatorArgs.AwsBedrockArgs(profile, region) =>
            AwsBedrockDocsEvaluator.create[IO](profile, Region.of(region))
          case DocsEvaluatorArgs.DocsEvaluatorArgsNotSpecified =>
            IO.raiseError(
              new RuntimeException(
                "Unexpected state - no documentation evaluator specified"
              )
            ).toResource

      printer <-
        args.outputArgs match
          case OutputArgs.ConsoleOutputArgs =>
            Resource.pure(ConsolePrinter[IO])
          case OutputArgs.MarkdownFileOutputArgs(file) =>
            MarkdownFilePrinter[IO](file)
    } yield AppComponents[IO](
      githubApis,
      docsEvaluator,
      printer,
      AppFlags(verbose = args.verbose)
    )

  case class AppComponents[F[_]](
      sourceCode: SourceCode[F],
      docsEvaluator: DocsEvaluator[F],
      printer: Printer[F],
      appFlags: AppFlags
  )
  case class AppFlags(
      verbose: Boolean
  )
}
