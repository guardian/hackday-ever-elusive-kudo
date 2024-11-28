package com.adamnfish.eek.integration

import cats.effect.{IO, Resource}
import com.adamnfish.eek.docs.AwsBedrockDocsEvaluator
import com.adamnfish.eek.docs.DocsEvaluator.DocsEvaluation.formatDocsEvaluation
import com.adamnfish.eek.vcs.VcsInformation.DocsFile
import munit.CatsEffectSuite
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory
import software.amazon.awssdk.regions.Region

class BuildDocsEvaluator extends CatsEffectSuite {
  // this actually hits AWS Bedrock, and is useful to iterate on the prompt
  // it is disabled with `.ignore`, remove that label to use it
  // to run just this test, you can use `testOnly -- "--tests=use this to iterate on the prompt"`
  test("use this to iterate on the prompt".ignore) {
    given LoggerFactory[IO] = Slf4jFactory.create[IO]
    val resources = for {
      evaluator <- AwsBedrockDocsEvaluator
        .create[IO]("developerPlayground", Region.of("us-east-1"))
      readme <- Resource.make(
        IO(scala.io.Source.fromFile("README.md"))
      )(f => IO(f.close))
    } yield (evaluator, readme)

    resources.use { case (evaluator, readme) =>
      for {
        readmeContents <- IO.blocking(readme.getLines.mkString("\n"))
        readmeDoc = DocsFile("readme.md", readmeContents)
        docsEvaluation <- evaluator.evaluateDocs(List(readmeDoc))
        _ <- IO.println(
          formatDocsEvaluation("owner", "repo", docsEvaluation)
        )
      } yield ()
    }
  }
}
