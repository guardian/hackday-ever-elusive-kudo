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
  def parse[F[_]: Monad: Console](
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

  private def executeOEffects[F[_]: Monad: Console](
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

  private val builder = OParser.builder[Args]

  private val parser: OParser[Unit, Args] = {
    import builder.*
    OParser.sequence(
      programName("ever-elusive-kudo"),
      head("scopt", "4.x"),
      cmd("github")
        .text("Evaluate the documentation of a GitHub repository")
        .action((_, a) => a.copy(sourceCodeArgs = GitHubArgs("", "", "")))
        .children(
          opt[String]('o', "owner")
            .text("GitHub repository owner (the organisation or user)")
            .required()
            .validate { owner =>
              if (owner.isEmpty) failure("owner cannot be empty")
              else success
            }
            .action((owner, args) =>
              updateGitHubArgs(args, _.copy(owner = owner))
            ),
          opt[String]('r', "repo")
            .text("GitHub repository name")
            .required()
            .validate { repo =>
              if (repo.isEmpty) failure("repo cannot be empty")
              else success
            }
            .action((repo, args) =>
              updateGitHubArgs(args, _.copy(repo = repo))
            ),
          opt[String]('g', "git-ref")
            .text("Git reference to evaluate (default: main)")
            .required()
            .withFallback(() => "main")
            .validate { gitRef =>
              if (gitRef.isEmpty) failure("git-ref cannot be empty")
              else success
            }
            .action((gitRef, args) =>
              updateGitHubArgs(args, _.copy(gitRef = gitRef))
            )
        ),
      cmd("local")
        .text("Evaluate the documentation of a project checked out locally")
        .action((_, a) =>
          a.copy(sourceCodeArgs = SourceCodeArgs.FilesystemArgs(new File("")))
        )
        .children(
          opt[File]('d', "directory")
            .text("Path to the local project's directory")
            .required()
            .validate { path =>
              if (Try(path.exists()).getOrElse(false)) success
              else failure(s"path $path does not exist or cannot be accessed")
            }
            .action((path, args) =>
              args.copy(sourceCodeArgs = SourceCodeArgs.FilesystemArgs(path))
            )
        ),
      note("AWS Bedrock LLM options"),
      opt[String]('p', "profile")
        .text("AWS profile name")
        .required()
        .validate { profile =>
          if (profile.isEmpty) failure("profile cannot be empty")
          else success
        }
        .action((profile, args) =>
          updateDocsEvaluatorArgs(args, _.copy(profile = profile))
        )
        .action((profile, args) =>
          args.copy(docsEvaluatorArgs = AwsBedrockArgs(profile, ""))
        ),
      opt[String]('r', "region")
        .text("AWS region (default: us-east-1)")
        .required()
        .withFallback(() => "us-east-1")
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
        )
    )
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
  case GitHubArgs(owner: String, repo: String, gitRef: String = "main")
  case FilesystemArgs(path: File)
  case SourceCodeArgsNotSpecified
}
enum DocsEvaluatorArgs {
  case AwsBedrockArgs(profile: String, region: String = "us-east-1")
  case DocsEvaluatorArgsNotSpecified
}
