package com.adamnfish.eek.docs

import cats.syntax.all.*
import com.adamnfish.eek.Formatter
import com.adamnfish.eek.docs.DocsEvaluator.DocsEvaluation
import com.adamnfish.eek.sourcecode.SourceCode

trait DocsEvaluator[F[_]] {
  def evaluateDocs(
      allDocs: List[SourceCode.DocsFile]
  ): F[(DocsEvaluation, String)]
}

object DocsEvaluator {
  enum DocsQuality {
    case Good
    case MayNeedImprovement(summary: String)
    case Missing
  }

  case class DocsEvaluation(
      basics: DocsBasicsEvaluation,
      contributing: ContributingEvaluation,
      architecture: ArchitectureEvaluation,
      technicalDetailEvaluation: TechnicalDetailEvaluation,
      dataGovernanceEvaluation: DataGovernanceEvaluation
  )

  case class DocsBasicsEvaluation(
      description: DocsQuality,
      howToRunLocally: DocsQuality,
      howToRunInProd: DocsQuality,
      howToDeploy: DocsQuality,
      howToTest: DocsQuality
  )

  case class ContributingEvaluation(
      howToContribute: DocsQuality,
      howToReportIssues: DocsQuality,
      howToGetHelp: DocsQuality
  )

  case class ArchitectureEvaluation(
      architectureOverview: DocsQuality,
      dataFlowOverview: DocsQuality
  )

  case class TechnicalDetailEvaluation(
      understandingCode: DocsQuality,
      understandingDependencies: DocsQuality,
      understandingTests: DocsQuality,
      understandingPerformance: DocsQuality,
      understandingSecurity: DocsQuality,
      understandingMonitoring: DocsQuality,
      understandingLogging: DocsQuality
  )

  case class DataGovernanceEvaluation(
      understandingDataStorage: DocsQuality,
      understandingDataProcessing: DocsQuality,
      understandingDataTransfer: DocsQuality,
      understandingDataAccess: DocsQuality,
      understandingDataRetention: DocsQuality,
      understandingDataDeletion: DocsQuality,
      understandingDataBackup: DocsQuality
  )

  object DocsEvaluation {
    def formatDocsQuality(
        label: String,
        docsQuality: DocsQuality,
        f: Formatter
    ): String =
      docsQuality match
        case DocsQuality.Good =>
          s"🟢 $label"
        case DocsQuality.MayNeedImprovement(summary) =>
          s"🟡 $label - ${f.informative(summary)}"
        case DocsQuality.Missing =>
          s"🔴 $label - ${f.informative("Not found")}"

    def formatDocsEvaluation(
        summary: String,
        docsEvaluation: DocsEvaluator.DocsEvaluation,
        f: Formatter
    ): String =
      // format: off
      s"""Documentation summary for ${f.emphasised(summary)}
         |🔑 ${f.emphasised("Key information")}:
         |${f.indent}${formatDocsQuality("Description", docsEvaluation.basics.description, f)}
         |${f.indent}${formatDocsQuality("Running locally", docsEvaluation.basics.howToRunLocally, f)}
         |${f.indent}${formatDocsQuality("Running in PROD", docsEvaluation.basics.howToRunInProd, f)}
         |${f.indent}${formatDocsQuality("Deploying", docsEvaluation.basics.howToDeploy, f)}
         |${f.indent}${formatDocsQuality("Testing", docsEvaluation.basics.howToTest, f)}
         |💻 ${f.emphasised("Software development support")}:
         |${f.indent}${formatDocsQuality("Contributing", docsEvaluation.contributing.howToContribute, f)}
         |${f.indent}${formatDocsQuality("Reporting issues", docsEvaluation.contributing.howToReportIssues, f)}
         |${f.indent}${formatDocsQuality("Getting help", docsEvaluation.contributing.howToGetHelp, f)}
         |🌏 ${f.emphasised("High-level overview")}:
         |${f.indent}${formatDocsQuality("Architecture", docsEvaluation.architecture.architectureOverview, f)}
         |${f.indent}${formatDocsQuality("Data flow overview", docsEvaluation.architecture.dataFlowOverview, f)}
         |🔧 ${f.emphasised("Detailed technical information")}:
         |${f.indent}${formatDocsQuality("Code", docsEvaluation.technicalDetailEvaluation.understandingCode, f)}
         |${f.indent}${formatDocsQuality("Dependencies", docsEvaluation.technicalDetailEvaluation.understandingDependencies, f)}
         |${f.indent}${formatDocsQuality("Tests", docsEvaluation.technicalDetailEvaluation.understandingTests, f)}
         |${f.indent}${formatDocsQuality("Performance", docsEvaluation.technicalDetailEvaluation.understandingPerformance, f)}
         |${f.indent}${formatDocsQuality("Security", docsEvaluation.technicalDetailEvaluation.understandingSecurity, f)}
         |${f.indent}${formatDocsQuality("Monitoring", docsEvaluation.technicalDetailEvaluation.understandingMonitoring, f)}
         |${f.indent}${formatDocsQuality("Logging", docsEvaluation.technicalDetailEvaluation.understandingLogging, f)}
         |🪣 ${f.emphasised("Detailed data information")}:
         |${f.indent}${formatDocsQuality("Data storage", docsEvaluation.dataGovernanceEvaluation.understandingDataStorage, f)}
         |${f.indent}${formatDocsQuality("Data processing", docsEvaluation.dataGovernanceEvaluation.understandingDataProcessing, f)}
         |${f.indent}${formatDocsQuality("Data transfer", docsEvaluation.dataGovernanceEvaluation.understandingDataTransfer, f)}
         |${f.indent}${formatDocsQuality("Data access", docsEvaluation.dataGovernanceEvaluation.understandingDataAccess, f)}
         |${f.indent}${formatDocsQuality("Data retention", docsEvaluation.dataGovernanceEvaluation.understandingDataRetention, f)}
         |${f.indent}${formatDocsQuality("Data deletion", docsEvaluation.dataGovernanceEvaluation.understandingDataDeletion, f)}
         |${f.indent}${formatDocsQuality("Data backup", docsEvaluation.dataGovernanceEvaluation.understandingDataBackup, f)}
         |""".stripMargin
    // format: on

    def formatThoughts(thoughts: String, f: Formatter): String = {
      // format: off
      s"""${f.emphasised("Evaluation reasoning")} ${f.informative("(included because the `verbose` flag has been set)")}
         |$thoughts
         |${f.emphasised("End of evaluation reasoning")}
         |""".stripMargin
      // format: on
    }

    def empty: DocsEvaluation =
      DocsEvaluation(
        basics = DocsBasicsEvaluation(
          description = DocsQuality.Missing,
          howToRunLocally = DocsQuality.Missing,
          howToRunInProd = DocsQuality.Missing,
          howToDeploy = DocsQuality.Missing,
          howToTest = DocsQuality.Missing
        ),
        contributing = ContributingEvaluation(
          howToContribute = DocsQuality.Missing,
          howToReportIssues = DocsQuality.Missing,
          howToGetHelp = DocsQuality.Missing
        ),
        architecture = ArchitectureEvaluation(
          architectureOverview = DocsQuality.Missing,
          dataFlowOverview = DocsQuality.Missing
        ),
        technicalDetailEvaluation = TechnicalDetailEvaluation(
          understandingCode = DocsQuality.Missing,
          understandingDependencies = DocsQuality.Missing,
          understandingTests = DocsQuality.Missing,
          understandingPerformance = DocsQuality.Missing,
          understandingSecurity = DocsQuality.Missing,
          understandingMonitoring = DocsQuality.Missing,
          understandingLogging = DocsQuality.Missing
        ),
        dataGovernanceEvaluation = DataGovernanceEvaluation(
          understandingDataStorage = DocsQuality.Missing,
          understandingDataProcessing = DocsQuality.Missing,
          understandingDataTransfer = DocsQuality.Missing,
          understandingDataAccess = DocsQuality.Missing,
          understandingDataRetention = DocsQuality.Missing,
          understandingDataDeletion = DocsQuality.Missing,
          understandingDataBackup = DocsQuality.Missing
        )
      )
  }
}
