package com.adamnfish.tdc.docs

import cats.syntax.all.*
import com.adamnfish.tdc.docs.DocsEvaluator.DocsEvaluation
import com.adamnfish.tdc.vcs.VcsInformation

trait DocsEvaluator[F[_]] {
  def evaluateDocs(allDocs: List[VcsInformation.DocsFile]): F[DocsEvaluation]
}
object DocsEvaluator {
  enum DocsQuality {
    case Good
    case MayNeedImprovement(summary: String)
    case Missing
  }

  case class DocsEvaluation(
      // documentation basics
      description: DocsQuality,
      howToRunLocally: DocsQuality,
      howToRunInProd: DocsQuality,
      howToDeploy: DocsQuality,
      howToTest: DocsQuality,
      // Software development lifecycle help
      howToContribute: DocsQuality,
      howToReportIssues: DocsQuality,
      howToGetHelp: DocsQuality,
      // high-level overviews
      architectureOverview: DocsQuality,
      dataFlowOverview: DocsQuality,
      // detailed technical information
      understandingCode: DocsQuality,
      understandingDependencies: DocsQuality,
      understandingTests: DocsQuality,
      understandingPerformance: DocsQuality,
      understandingSecurity: DocsQuality,
      understandingCompliance: DocsQuality,
      understandingMonitoring: DocsQuality,
      understandingLogging: DocsQuality,
      understandingErrorHandling: DocsQuality,
      // more detailed data information
      understandingDataStorage: DocsQuality,
      understandingDataProcessing: DocsQuality,
      understandingDataTransfer: DocsQuality,
      understandingDataAccess: DocsQuality,
      understandingDataRetention: DocsQuality,
      understandingDataDeletion: DocsQuality,
      understandingDataBackup: DocsQuality
  )

  object DocsEvaluation {
    def formatDocsQuality(label: String, docsQuality: DocsQuality): String =
      docsQuality match
        case DocsQuality.Good =>
          s"🟢 $label"
        case DocsQuality.MayNeedImprovement(summary) =>
          s"🟡 $label - $summary"
        case DocsQuality.Missing =>
          s"🔴 $label - not found"

    def formatDocsEvaluation(
        owner: String,
        repositoryName: String,
        docsEvaluation: DocsEvaluator.DocsEvaluation
    ): String =
      // I'd like this formatted similarly to the actual output, instead of as "source code"
      // format: off
      s"""Documentation summary for $owner/$repositoryName
         |  🔑 Key information:
         |    ${formatDocsQuality("Description", docsEvaluation.description)}
         |    ${formatDocsQuality("Running locally", docsEvaluation.howToRunLocally)}
         |    ${formatDocsQuality("Running in prod", docsEvaluation.howToRunInProd)}
         |    ${formatDocsQuality("Deploying", docsEvaluation.howToDeploy)}
         |    ${formatDocsQuality("Testing", docsEvaluation.howToTest)}
         |  💻 Software development support:
         |    ${formatDocsQuality("Contributing", docsEvaluation.howToContribute)}
         |    ${formatDocsQuality("Reporting issues", docsEvaluation.howToReportIssues)}
         |    ${formatDocsQuality("Getting help", docsEvaluation.howToGetHelp)}
         |  🌏 High-level overview:
         |    ${formatDocsQuality("Architecture", docsEvaluation.architectureOverview)}
         |    ${formatDocsQuality("Data flow overview", docsEvaluation.dataFlowOverview)}
         |  🔧 Detailed technical information:
         |    ${formatDocsQuality("Code", docsEvaluation.understandingCode)}
         |    ${formatDocsQuality("Dependencies", docsEvaluation.understandingDependencies)}
         |    ${formatDocsQuality("Tests", docsEvaluation.understandingTests)}
         |    ${formatDocsQuality("Performance", docsEvaluation.understandingPerformance)}
         |    ${formatDocsQuality("Security", docsEvaluation.understandingSecurity)}
         |    ${formatDocsQuality("Compliance", docsEvaluation.understandingCompliance)}
         |    ${formatDocsQuality("Monitoring", docsEvaluation.understandingMonitoring)}
         |    ${formatDocsQuality("Logging", docsEvaluation.understandingLogging)}
         |    ${formatDocsQuality("Error handling", docsEvaluation.understandingErrorHandling)}
         |  🪣 Detailed data information:
         |    ${formatDocsQuality("Data storage", docsEvaluation.understandingDataStorage)}
         |    ${formatDocsQuality("Data processing", docsEvaluation.understandingDataProcessing)}
         |    ${formatDocsQuality("Data transfer", docsEvaluation.understandingDataTransfer)}
         |    ${formatDocsQuality("Data access", docsEvaluation.understandingDataAccess)}
         |    ${formatDocsQuality("Data retention", docsEvaluation.understandingDataRetention)}
         |    ${formatDocsQuality("Data deletion", docsEvaluation.understandingDataDeletion)}
         |    ${formatDocsQuality("Data backup", docsEvaluation.understandingDataBackup)}
         |""".stripMargin
    // format: on

    def empty: DocsEvaluation =
      DocsEvaluation(
        description = DocsQuality.Good,
        howToRunLocally = DocsQuality.Good,
        howToRunInProd = DocsQuality.Good,
        howToDeploy = DocsQuality.Good,
        howToTest = DocsQuality.Good,
        howToContribute = DocsQuality.Good,
        howToReportIssues = DocsQuality.Good,
        howToGetHelp = DocsQuality.Good,
        architectureOverview = DocsQuality.Good,
        dataFlowOverview = DocsQuality.Good,
        understandingCode = DocsQuality.Good,
        understandingDependencies = DocsQuality.Good,
        understandingTests = DocsQuality.Good,
        understandingPerformance = DocsQuality.Good,
        understandingSecurity = DocsQuality.Good,
        understandingCompliance = DocsQuality.Good,
        understandingMonitoring = DocsQuality.Good,
        understandingLogging = DocsQuality.Good,
        understandingErrorHandling = DocsQuality.Good,
        understandingDataStorage = DocsQuality.Good,
        understandingDataProcessing = DocsQuality.Good,
        understandingDataTransfer = DocsQuality.Good,
        understandingDataAccess = DocsQuality.Good,
        understandingDataRetention = DocsQuality.Good,
        understandingDataDeletion = DocsQuality.Good,
        understandingDataBackup = DocsQuality.Good
      )
  }
}
