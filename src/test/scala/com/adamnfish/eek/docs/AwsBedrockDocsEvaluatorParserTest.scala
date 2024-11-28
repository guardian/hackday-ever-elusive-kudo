package com.adamnfish.eek.docs

import scala.util.{Try, Success}
import cats.*
import cats.data.*
import cats.syntax.all.*
import com.adamnfish.eek.docs.DocsEvaluator.*
import munit.FunSuite

class AwsBedrockDocsEvaluatorParserTest extends FunSuite {
  test("parses an example response into a docs evaluation") {
    val evaluation =
      """description: Good
        |howToRunLocally: Good
        |howToRunInProd: May need improvement | No clear production deployment strategy currently exists
        |howToDeploy: Missing
        |howToTest: Good
        |howToContribute: Good
        |howToReportIssues: Good
        |howToGetHelp: Good
        |architectureOverview: May need improvement | Only basic information about being a Scala CLI tool is provided
        |dataFlowOverview: Good
        |understandingCode: May need improvement | Only points to a single source file without deeper code explanation
        |understandingDependencies: Missing
        |understandingTests: Good
        |understandingPerformance: Missing
        |understandingSecurity: May need improvement | Mentions GitHub token and AWS credentials but no security details
        |understandingMonitoring: Missing
        |understandingLogging: Missing
        |understandingDataStorage: Good
        |understandingDataProcessing: Good
        |understandingDataTransfer: Good
        |understandingDataAccess: May need improvement | Mentions GitHub API access but lacks detailed access patterns
        |understandingDataRetention: Good
        |understandingDataDeletion: Good
        |understandingDataBackup: Missing""".stripMargin
    val result = AwsBedrockDocsEvaluator.Parser.parseEvaluation[Try](evaluation)
    assertEquals(
      result,
      Success(
        DocsEvaluation(
          DocsBasicsEvaluation(
            DocsQuality.Good,
            DocsQuality.Good,
            DocsQuality.MayNeedImprovement(
              "No clear production deployment strategy currently exists"
            ),
            DocsQuality.Missing,
            DocsQuality.Good
          ),
          ContributingEvaluation(
            DocsQuality.Good,
            DocsQuality.Good,
            DocsQuality.Good
          ),
          ArchitectureEvaluation(
            DocsQuality.MayNeedImprovement(
              "Only basic information about being a Scala CLI tool is provided"
            ),
            DocsQuality.Good
          ),
          TechnicalDetailEvaluation(
            DocsQuality.MayNeedImprovement(
              "Only points to a single source file without deeper code explanation"
            ),
            DocsQuality.Missing,
            DocsQuality.Good,
            DocsQuality.Missing,
            DocsQuality.MayNeedImprovement(
              "Mentions GitHub token and AWS credentials but no security details"
            ),
            DocsQuality.Missing,
            DocsQuality.Missing
          ),
          DataGovernanceEvaluation(
            DocsQuality.Good,
            DocsQuality.Good,
            DocsQuality.Good,
            DocsQuality.MayNeedImprovement(
              "Mentions GitHub API access but lacks detailed access patterns"
            ),
            DocsQuality.Good,
            DocsQuality.Good,
            DocsQuality.Missing
          )
        )
      )
    )
  }
}
