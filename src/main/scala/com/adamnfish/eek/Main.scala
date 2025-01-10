package com.adamnfish.eek

import caseapp.catseffect.IOCaseApp
import caseapp.*
import cats.effect.{ExitCode, IO, Resource}
import cats.syntax.all.*
import com.adamnfish.eek.docs.AwsBedrockDocsEvaluator
import com.adamnfish.eek.docs.DocsEvaluator
import com.adamnfish.eek.docs.DocsEvaluator.DocsEvaluation
import com.adamnfish.eek.docs.DocsEvaluator.DocsEvaluation.formatDocsEvaluation
import com.adamnfish.eek.vcs.{Github, VcsInformation}
import fs2.io.net.Network
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import software.amazon.awssdk.regions.Region
import org.typelevel.log4cats._
import org.typelevel.log4cats.slf4j.Slf4jFactory
import scala.Console.*

object Main extends IOCaseApp[CliArgs] {
  // CLI app's main method
  override def run(args: CliArgs, otherArgs: RemainingArgs): IO[ExitCode] = {
    // set up our dependencies
    resources(args)
      // and then execute the program
      .use(app(args, _))
  }

  def app(args: CliArgs, appComponents: AppComponents[IO]): IO[ExitCode] =
    for {
      docs <- appComponents.vcsInformation.repoDocs(args.gitRef)
      < <- appComponents.printer.println(
        s"Evaluating the following docs files: ${docs.map(df => s"${CYAN}${df.path}${RESET}").mkString(", ")}"
      )
      (docsEvaluation, thoughts) <- appComponents.docsEvaluator.evaluateDocs(
        docs
      )
      _ <-
        if (args.verbose)
          appComponents.printer.println(DocsEvaluation.formatThoughts(thoughts))
        else IO.unit
      _ <- appComponents.printer.println(
        formatDocsEvaluation(args.owner, args.repo, docsEvaluation)
      )
    } yield ExitCode.Success

  // sets up the services required by the application
  def resources(args: CliArgs): Resource[IO, AppComponents[IO]] =
    for {
      apiKey <- Resource.eval(requiredEnvVar("GITHUB_API_KEY"))
      given LoggerFactory[IO] = Slf4jFactory.create[IO]
      githubApis <- Github.create[IO](apiKey, args.owner, args.repo)
      docsEvaluator <- AwsBedrockDocsEvaluator
        .create[IO](args.profile, Region.of(args.region))
      printer = ConsolePrinter[IO]
    } yield AppComponents(githubApis, docsEvaluator, printer)

  case class AppComponents[F[_]](
      vcsInformation: VcsInformation[F],
      docsEvaluator: DocsEvaluator[F],
      printer: Printer[F]
  )

  // helper to load an ENV var or fail with a message
  def requiredEnvVar(name: String): IO[String] = {
    for {
      envOpt <- IO.envForIO.get(name)
      env <- IO.fromOption(envOpt)(
        IllegalStateException(s"Could not load required ENV variable '$name'")
      )
    } yield env
  }
}

@AppName("Ever-elusive kudo")
@ProgName("eek")
case class CliArgs(
    @HelpMessage("Repository owner")
    owner: String,
    @HelpMessage("Repository name")
    repo: String,
    @HelpMessage("AWS profile name")
    profile: String,
    @HelpMessage("Git reference (branch, tag, or sha, defaults to 'main')")
    gitRef: String = "main",
    @HelpMessage("AWS region (defaults to 'us-east-1' for Bedrock usage)")
    region: String = "us-east-1",
    @HelpMessage("Verbose output will show the thinking behind the evaluation")
    @Name("v")
    verbose: Boolean = false
)
