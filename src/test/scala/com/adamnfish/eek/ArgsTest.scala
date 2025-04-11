package com.adamnfish.eek

import munit.{CatsEffectSuite, FunSuite}
import cats.effect.IO
import com.adamnfish.eek.DocsEvaluatorArgs.AwsBedrockArgs
import com.adamnfish.eek.SourceCodeArgs.{FilesystemArgs, GitHubArgs}

import java.io.File

class ArgsTest extends FunSuite {
  test("parses example GitHub CLI args") {
    val cliArgs = Seq(
      // format: off
      "--github", "owner/repository",
      "-p", "profile"
      // format: on
    )
    val (maybeArgs, _) = Args.parseArgs(cliArgs)
    assertEquals(
      maybeArgs,
      Some(
        Args(
          GitHubArgs("owner", "repository", "main"),
          AwsBedrockArgs("profile", "us-east-1"),
          verbose = false
        )
      )
    )
  }

  test("parses example GitHub repository with gitref provided") {
    val cliArgs = Seq(
      // format: off
      "--github", "owner/repository",
      "--git-ref", "branch-name",
      "-p", "profile"
      // format: on
    )
    val (maybeArgs, _) = Args.parseArgs(cliArgs)
    assertEquals(
      maybeArgs,
      Some(
        Args(
          GitHubArgs("owner", "repository", "branch-name"),
          AwsBedrockArgs("profile", "us-east-1"),
          verbose = false
        )
      )
    )
  }

  test("parses example local filesystem args") {
    val cliArgs = Seq(
      // format: off
      "--local", "src/main",
      "--profile", "profile-name",
      // format: on
    )
    val (maybeArgs, _) = Args.parseArgs(cliArgs)
    assertEquals(
      maybeArgs,
      Some(
        Args(
          FilesystemArgs(new File("src/main")),
          AwsBedrockArgs("profile-name", "us-east-1"),
          verbose = false
        )
      )
    )
  }

  test("parses example local filesystem args with a different bedrock region") {
    val cliArgs = Seq(
      // format: off
      "--local", "src/main",
      "--profile", "profile-name",
      "--region", "eu-west-1",
      // format: on
    )
    val (maybeArgs, _) = Args.parseArgs(cliArgs)
    assertEquals(
      maybeArgs,
      Some(
        Args(
          FilesystemArgs(new File("src/main")),
          AwsBedrockArgs("profile-name", "eu-west-1"),
          verbose = false
        )
      )
    )
  }
}
