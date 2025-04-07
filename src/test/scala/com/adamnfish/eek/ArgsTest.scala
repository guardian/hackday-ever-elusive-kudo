package com.adamnfish.eek

import munit.CatsEffectSuite
import cats.effect.IO
import com.adamnfish.eek.DocsEvaluatorArgs.AwsBedrockArgs
import com.adamnfish.eek.SourceCodeArgs.{GitHubArgs, FilesystemArgs}
import java.io.File

class ArgsTest extends CatsEffectSuite {
  test("parses example GitHub CLI args") {
    val args = Seq(
      // format: off
      "github",
      "-o", "owner",
      "-r", "repository",
      "-p", "profile"
      // format: on
    )
    for {
      args <- Args.parseArgs[IO](args)
    } yield assertEquals(
      args,
      Args(
        GitHubArgs("owner", "repository"),
        AwsBedrockArgs("profile"),
        verbose = false
      )
    )
  }

  test("parses example local filesystem, CLI args") {
    val args = Seq(
      // format: off
      "local",
      "-d", "src/main",
      "--profile", "profile-name",
      // format: on
    )
    for {
      args <- Args.parseArgs[IO](args)
    } yield assertEquals(
      args,
      Args(
        FilesystemArgs(new File("src/main")),
        AwsBedrockArgs("profile-name"),
        verbose = false
      )
    )
  }
}
