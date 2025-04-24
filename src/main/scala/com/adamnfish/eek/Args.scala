package com.adamnfish.eek

import cats.*
import cats.data.*
import cats.syntax.all.*
import cats.effect.{ExitCode, IO}
import cats.effect.std.Console
import com.adamnfish.eek.DocsEvaluatorArgs.*
import com.adamnfish.eek.SourceCodeArgs.*
import scopt.{OEffect, OParser}

import java.io.File
import scala.util.Try

case class Args(
    sourceCodeArgs: SourceCodeArgs,
    docsEvaluatorArgs: DocsEvaluatorArgs,
    verbose: Boolean
)
object Args {
  def empty: Args = Args(
    SourceCodeArgsNotSpecified,
    DocsEvaluatorArgsNotSpecified,
    verbose = false
  )

  def parseArgs(args: Seq[String]): (Option[Args], List[OEffect]) = {
    OParser.runParser(parser, args, Args.empty)
  }

  /** Do the parsing, and execute the resulting effects (printing errors etc)
    */
  def parse[F[_]: {Monad, Console}](
      args: Seq[String]
  ): F[Either[ExitCode, Args]] = {
    parseArgs(args) match {
      case (Some(config), effects) =>
        // don't expect any effects here, but we'll run them just in case!
        executeOEffects(effects) *> Applicative[F].pure(Right(config))
      case (None, effects) =>
        executeOEffects(effects).flatMap {
          case Some(exitCode) =>
            Applicative[F].pure(Left(exitCode))
          case None =>
            // no exit code from effects, but failed to get args - unexpected state
            Console[F]
              .errorln("Unable to parse command line arguments")
              .as(Left(ExitCode.Error))
        }
    }
  }

  private val builder = OParser.builder[Args]

  private val GitHubRepoRegex = "([A-Za-z0-9_.-]+)/([A-Za-z0-9_.-]+)".r

  private val parser: OParser[Unit, Args] = {
    import builder.*
    OParser.sequence(
      programName("eek"),
      head("scopt", "4.x"),
      help("help").text("prints this usage text"),
      note("Source code options"),
      opt[String]("github")
        .text(
          "Evaluate the documentation of a GitHub repository"
        )
        .valueName("<owner/repo>")
        .validate {
          case GitHubRepoRegex(owner, repo) =>
            success
          case str =>
            failure(
              "Please provide github repository to in the form owner/repository"
            )
        }
        .action { case (str, args) =>
          str match {
            case GitHubRepoRegex(owner, repo) =>
              args.copy(sourceCodeArgs = GitHubArgs(owner, repo, "main"))
            case _ => args
          }
        },
      opt[String]("git-ref")
        .text("Git reference to evaluate for GitHub repository (default: main)")
        .optional()
        .validate { gitRef =>
          if (gitRef.isEmpty) failure("git-ref cannot be empty")
          else success
        }
        .action((gitRef, args) =>
          updateGitHubArgs(args, _.copy(gitRef = gitRef))
        ),
      opt[File]("local")
        .text("Evaluate the documentation of a local codebase")
        .valueName("<directory>")
        .validate { path =>
          if (Try(path.exists()).getOrElse(false)) success
          else failure(s"path $path does not exist or cannot be accessed")
        }
        .action((path, args) =>
          args.copy(sourceCodeArgs = SourceCodeArgs.FilesystemArgs(path))
        ),
      note("LLM options"),
      opt[String]('p', "bedrock-profile")
        .text(
          "Use AWS Bedrock, authenticated with the specified AWS profile name"
        )
        .valueName("<profile>")
        .validate { profile =>
          if (profile.isEmpty) failure("Bedrock profile name cannot be empty")
          else success
        }
        .action((profile, args) =>
          updateDocsEvaluatorArgs(args, _.copy(profile = profile))
        )
        .action((profile, args) =>
          args.copy(docsEvaluatorArgs = AwsBedrockArgs(profile, "us-east-1"))
        ),
      opt[String]('r', "region")
        .text("Bedrock's AWS region (default: us-east-1)")
        .optional()
        .validate { region =>
          if (region.isEmpty) failure("region cannot be empty")
          else success
        }
        .action((region, args) =>
          updateDocsEvaluatorArgs(args, _.copy(region = region))
        ),
      note("Standard options"),
      opt[Unit]('v', "verbose")
        .action((flag, args) => args.copy(verbose = true))
        .text(
          "Verbose mode will print the LLM's reasoning as well as its answer"
        ),
      checkConfig {
        case Args(
              SourceCodeArgsNotSpecified,
              DocsEvaluatorArgsNotSpecified,
              _
            ) =>
          failure(
            "Please specify --github <repo/owner> or --local <dir> and --bedrock-profile <profile>"
          )
        case Args(SourceCodeArgsNotSpecified, _, _) =>
          failure("Please specify --github <repo/owner> or --local <dir>")
        case Args(_, DocsEvaluatorArgsNotSpecified, _) =>
          failure(
            "Please use --bedrock-profile <profile> to provide an AWS profile"
          )
        case _ =>
          success
      }
    )
  }

  private def executeOEffects[F[_]: {Monad, Console}](
      effects: List[OEffect]
  ): F[Option[ExitCode]] = {
    effects.foldM[F, Option[ExitCode]](None) { (acc, effect) =>
      for {
        maybeNextExitCode <- effect match {
          case OEffect.DisplayToOut(msg) =>
            Console[F]
              .println(msg)
              .as(None)
          case OEffect.DisplayToErr(msg) =>
            Console[F]
              .errorln(msg)
              .as(None)
          case OEffect.ReportError(msg) =>
            Console[F]
              .errorln(s"Error: $msg")
              .as(Some(ExitCode.Error))
          case OEffect.ReportWarning(msg) =>
            Console[F]
              .println(s"Warning: $msg")
              .as(Some(ExitCode.Error))
          case OEffect.Terminate(existState) =>
            Applicative[F].pure(Some(ExitCode.Error))
        }
      } yield {
        List(acc, maybeNextExitCode).flatten
          .maxByOption(_.code)
      }
    }
  }

  /** Helpers for updating fields in the nested GitHubArgs object, since we do
    * this repeatedly
    */
  private def updateGitHubArgs(
      args: Args,
      update: SourceCodeArgs.GitHubArgs => SourceCodeArgs.GitHubArgs
  ): Args = {
    args.sourceCodeArgs match {
      case gha: SourceCodeArgs.GitHubArgs =>
        args.copy(sourceCodeArgs = update(gha))
      case _ => args
    }
  }

  private def updateDocsEvaluatorArgs(
      args: Args,
      update: DocsEvaluatorArgs.AwsBedrockArgs => DocsEvaluatorArgs.AwsBedrockArgs
  ): Args = {
    args.docsEvaluatorArgs match {
      case dba: AwsBedrockArgs => args.copy(docsEvaluatorArgs = update(dba))
      case _                   => args
    }
  }
}

enum SourceCodeArgs {
  case GitHubArgs(owner: String, repo: String, gitRef: String)
  case FilesystemArgs(path: File)
  case SourceCodeArgsNotSpecified
}
enum DocsEvaluatorArgs {
  case AwsBedrockArgs(profile: String, region: String)
  case DocsEvaluatorArgsNotSpecified
}
