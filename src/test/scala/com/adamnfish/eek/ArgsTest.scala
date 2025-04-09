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
      "github",
      "-o", "owner",
      "-r", "repository",
      "-p", "profile"
      // format: on
    )
    val (maybeArgs, _) = Args.parseArgs(cliArgs)
    assertEquals(
      maybeArgs,
      Some(
        Args(
          GitHubArgs("owner", "repository"),
          AwsBedrockArgs("profile"),
          verbose = false
        )
      )
    )
  }

  test("parses example local filesystem, CLI args") {
    val cliArgs = Seq(
      // format: off
      "local",
      "-d", "src/main",
      "--profile", "profile-name",
      // format: on
    )
    val (maybeArgs, _) = Args.parseArgs(cliArgs)
    assertEquals(
      maybeArgs,
      Some(
        Args(
          FilesystemArgs(new File("src/main")),
          AwsBedrockArgs("profile-name"),
          verbose = false
        )
      )
    )
  }
}
