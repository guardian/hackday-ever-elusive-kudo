package com.adamnfish.tdc.docs

import cats.syntax.all.*
import com.adamnfish.tdc.docs.DocsEvaluator.DocsEvaluation
import com.adamnfish.tdc.vcs.VcsInformation
import scala.Console.*

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
    def formatDocsQuality(label: String, docsQuality: DocsQuality): String =
      docsQuality match
        case DocsQuality.Good =>
          s"🟢 $label"
        case DocsQuality.MayNeedImprovement(summary) =>
          s"🟡 $label - ${CYAN}$summary${RESET}"
        case DocsQuality.Missing =>
          s"🔴 $label - ${CYAN}not found${RESET}"

    def formatDocsEvaluation(
        owner: String,
        repositoryName: String,
        docsEvaluation: DocsEvaluator.DocsEvaluation
    ): String =
      // format: off
      s"""Documentation summary for $owner/$repositoryName
         |  🔑 ${BOLD}Key information${RESET}:
         |    ${formatDocsQuality("Description", docsEvaluation.basics.description)}
         |    ${formatDocsQuality("Running locally", docsEvaluation.basics.howToRunLocally)}
         |    ${formatDocsQuality("Running in prod", docsEvaluation.basics.howToRunInProd)}
         |    ${formatDocsQuality("Deploying", docsEvaluation.basics.howToDeploy)}
         |    ${formatDocsQuality("Testing", docsEvaluation.basics.howToTest)}
         |  💻 ${BOLD}Software development support${RESET}:
         |    ${formatDocsQuality("Contributing", docsEvaluation.contributing.howToContribute)}
         |    ${formatDocsQuality("Reporting issues", docsEvaluation.contributing.howToReportIssues)}
         |    ${formatDocsQuality("Getting help", docsEvaluation.contributing.howToGetHelp)}
         |  🌏 ${BOLD}High-level overview${RESET}:
         |    ${formatDocsQuality("Architecture", docsEvaluation.architecture.architectureOverview)}
         |    ${formatDocsQuality("Data flow overview", docsEvaluation.architecture.dataFlowOverview)}
         |  🔧 ${BOLD}Detailed technical information${RESET}:
         |    ${formatDocsQuality("Code", docsEvaluation.technicalDetailEvaluation.understandingCode)}
         |    ${formatDocsQuality("Dependencies", docsEvaluation.technicalDetailEvaluation.understandingDependencies)}
         |    ${formatDocsQuality("Tests", docsEvaluation.technicalDetailEvaluation.understandingTests)}
         |    ${formatDocsQuality("Performance", docsEvaluation.technicalDetailEvaluation.understandingPerformance)}
         |    ${formatDocsQuality("Security", docsEvaluation.technicalDetailEvaluation.understandingSecurity)}
         |    ${formatDocsQuality("Monitoring", docsEvaluation.technicalDetailEvaluation.understandingMonitoring)}
         |    ${formatDocsQuality("Logging", docsEvaluation.technicalDetailEvaluation.understandingLogging)}
         |  🪣 ${BOLD}Detailed data information${RESET}:
         |    ${formatDocsQuality("Data storage", docsEvaluation.dataGovernanceEvaluation.understandingDataStorage)}
         |    ${formatDocsQuality("Data processing", docsEvaluation.dataGovernanceEvaluation.understandingDataProcessing)}
         |    ${formatDocsQuality("Data transfer", docsEvaluation.dataGovernanceEvaluation.understandingDataTransfer)}
         |    ${formatDocsQuality("Data access", docsEvaluation.dataGovernanceEvaluation.understandingDataAccess)}
         |    ${formatDocsQuality("Data retention", docsEvaluation.dataGovernanceEvaluation.understandingDataRetention)}
         |    ${formatDocsQuality("Data deletion", docsEvaluation.dataGovernanceEvaluation.understandingDataDeletion)}
         |    ${formatDocsQuality("Data backup", docsEvaluation.dataGovernanceEvaluation.understandingDataBackup)}
         |""".stripMargin
    // format: on

    def empty: DocsEvaluation =
      DocsEvaluation(
        basics = DocsBasicsEvaluation(
          description = DocsQuality.Good,
          howToRunLocally = DocsQuality.Good,
          howToRunInProd = DocsQuality.Good,
          howToDeploy = DocsQuality.Good,
          howToTest = DocsQuality.Good
        ),
        contributing = ContributingEvaluation(
          howToContribute = DocsQuality.Good,
          howToReportIssues = DocsQuality.Good,
          howToGetHelp = DocsQuality.Good
        ),
        architecture = ArchitectureEvaluation(
          architectureOverview = DocsQuality.Good,
          dataFlowOverview = DocsQuality.Good
        ),
        technicalDetailEvaluation = TechnicalDetailEvaluation(
          understandingCode = DocsQuality.Good,
          understandingDependencies = DocsQuality.Good,
          understandingTests = DocsQuality.Good,
          understandingPerformance = DocsQuality.Good,
          understandingSecurity = DocsQuality.Good,
          understandingMonitoring = DocsQuality.Good,
          understandingLogging = DocsQuality.Good
        ),
        dataGovernanceEvaluation = DataGovernanceEvaluation(
          understandingDataStorage = DocsQuality.Good,
          understandingDataProcessing = DocsQuality.Good,
          understandingDataTransfer = DocsQuality.Good,
          understandingDataAccess = DocsQuality.Good,
          understandingDataRetention = DocsQuality.Good,
          understandingDataDeletion = DocsQuality.Good,
          understandingDataBackup = DocsQuality.Good
        )
      )
  }
}
