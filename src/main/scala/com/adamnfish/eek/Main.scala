package com.adamnfish.eek

import cats.effect.IOApp
import cats.effect.{ExitCode, IO, Resource}
import cats.syntax.all.*
import com.adamnfish.eek.docs.AwsBedrockDocsEvaluator
import com.adamnfish.eek.docs.DocsEvaluator
import com.adamnfish.eek.docs.DocsEvaluator.DocsEvaluation
import com.adamnfish.eek.docs.DocsEvaluator.DocsEvaluation.formatDocsEvaluation
import com.adamnfish.eek.sourcecode.{Filesystem, Github, SourceCode}
import fs2.io.net.Network
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import software.amazon.awssdk.regions.Region
import org.typelevel.log4cats.*
import org.typelevel.log4cats.slf4j.Slf4jFactory

import scala.Console.*

object Main extends IOApp {
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

  def app(appComponents: AppComponents[IO]): IO[ExitCode] =
    for {
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
            IO.raiseError(new RuntimeException("TODO")).toResource

      docsEvaluator <-
        args.docsEvaluatorArgs match
          case DocsEvaluatorArgs.AwsBedrockArgs(profile, region) =>
            AwsBedrockDocsEvaluator.create[IO](profile, Region.of(region))
          case DocsEvaluatorArgs.DocsEvaluatorArgsNotSpecified =>
            IO.raiseError(new RuntimeException("TODO")).toResource
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
