package com.adamnfish.eek

import cats.effect.{ExitCode, IO, IOApp, Resource}
import cats.syntax.all.*
import com.adamnfish.eek.docs.{AwsBedrockDocsEvaluator, DocsEvaluator}
import com.adamnfish.eek.docs.DocsEvaluator.DocsEvaluation
import com.adamnfish.eek.docs.DocsEvaluator.DocsEvaluation.formatDocsEvaluation
import com.adamnfish.eek.sourcecode.{Filesystem, Github, SourceCode}
import fs2.io.net.Network
import org.typelevel.log4cats.*
import org.typelevel.log4cats.slf4j.Slf4jFactory
import software.amazon.awssdk.regions.Region

import scala.Console.*
import scala.util.control.NonFatal

object Main extends IOApp {

  /** CLI entrypoint
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

  def app(appComponents: AppComponents[IO]): IO[ExitCode] = {
    given LoggerFactory[IO] = Slf4jFactory.create[IO]
    val logger = LoggerFactory.getLogger[IO]

    val program = for {
      docs <- appComponents.sourceCode.repoDocs
      _ <- appComponents.printer.println(
        s"Evaluating the following docs files: ${docs.map(df => s"${CYAN}${df.path}${RESET}").mkString(", ")}"
      )
      (docsEvaluation, thoughts) <- appComponents.docsEvaluator.evaluateDocs(
        docs
      )
      _ <-
        if (appComponents.appFlags.verbose)
          appComponents.printer.println(DocsEvaluation.formatThoughts(thoughts))
        else IO.unit
      _ <- appComponents.printer.println(
        formatDocsEvaluation(appComponents.sourceCode.summary, docsEvaluation)
      )
    } yield ExitCode.Success
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
    } yield AppComponents[IO](
      githubApis,
      docsEvaluator,
      ConsolePrinter[IO],
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
