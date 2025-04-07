package com.adamnfish.eek.docs

import cats.*
import com.adamnfish.eek.docs.DocsEvaluator.*
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import scala.util.{Success, Try}

class AwsBedrockDocsEvaluatorParserTest extends AnyFreeSpec with Matchers {
  "parseEvaluation" - {
    "parses an example response into a docs evaluation" in {
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
      val result =
        AwsBedrockDocsEvaluator.Parser.parseEvaluation[Try](evaluation)
      result shouldEqual Success(
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
    }
  }

  "parseBedrockResponse" - {
    "parses an example response into a docs evaluation" in {
      val evaluation =
        """<thinking>
          |verbose thoughts
          |</thinking>
          |<evaluation>
          |description: Good
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
          |understandingDataBackup: Missing
          |</evaluation>""".stripMargin

      val result =
        AwsBedrockDocsEvaluator.Parser.parseBedrockResponse[Try](evaluation)
      result shouldEqual Success(
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
    }

    "parses the verbose thoughts out of a docs evaluation response" in {
      val evaluation =
        """<thinking>
          |verbose thoughts
          |</thinking>
          |<evaluation>
          |description: Good
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
          |understandingDataBackup: Missing
          |</evaluation>""".stripMargin
      val result =
        AwsBedrockDocsEvaluator.Parser.parseBedrockThinking(evaluation)
      result shouldEqual """verbose thoughts""".stripMargin
    }
  }
}
