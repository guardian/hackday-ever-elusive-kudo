package com.adamnfish.tdc.docs

import cats.MonadThrow
import cats.effect.Resource
import cats.effect.std.Console
import cats.effect.kernel.Sync
import cats.syntax.all.*
import com.adamnfish.tdc.docs.AwsBedrockDocsEvaluator.Parser
import com.adamnfish.tdc.docs.DocsEvaluator.DocsQuality.MayNeedImprovement
import com.adamnfish.tdc.docs.DocsEvaluator.*
import com.adamnfish.tdc.vcs.VcsInformation
import com.adamnfish.tdc.vcs.VcsInformation.DocsFile
import org.typelevel.log4cats.LoggerFactory
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider
import software.amazon.awssdk.http.apache.ApacheHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient
import software.amazon.awssdk.services.bedrockruntime.model.*

import scala.jdk.CollectionConverters.*

class AwsBedrockDocsEvaluator[F[_]: Sync: MonadThrow: LoggerFactory](
    bedrockRuntimeClient: BedrockRuntimeClient
) extends DocsEvaluator[F] {
  private val logger = LoggerFactory.getLogger[F]

  override def evaluateDocs(
      allDocs: List[VcsInformation.DocsFile]
  ): F[DocsEvaluation] = {
    val message = AwsBedrockDocsEvaluator.createMessage(allDocs)
    for {
      _ <- logger.debug(
        s"""PROMPT:
           |$message"
           |""".stripMargin
      )
      response <- Sync[F].blocking {
        bedrockRuntimeClient.converse {
          ConverseRequest
            .builder()
            .modelId(AwsBedrockDocsEvaluator.modelId)
            .messages(message)
            .inferenceConfig {
              InferenceConfiguration
                .builder()
                .maxTokens(1000) // may not be enough!
                .temperature(0.5f)
                .topP(0.9f)
                .build()
            }
            .build()
        }
      }
      contentBlocks = response.output.message.content.asScala.toList
      content = contentBlocks.flatMap(cb => Option(cb.text())).mkString("")
      _ <- logger.debug(
        s"""LLM RESPONSE:
           |$content
           |""".stripMargin
      )
      evaluation <- Parser.parseBedrockResponse(content)
    } yield evaluation
  }
}

object AwsBedrockDocsEvaluator {
  //  val modelId = "us.anthropic.claude-3-5-sonnet-20241022-v2:0"
  val modelId = "us.anthropic.claude-3-5-haiku-20241022-v1:0"

  def create[F[_]: Sync: LoggerFactory](
      profileName: String,
      region: Region
  ): Resource[F, AwsBedrockDocsEvaluator[F]] = {
    for {
      // https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/http-configuration-apache.html#http-apache-config
      httpClient <- Resource.make {
        Sync[F].delay {
          ApacheHttpClient
            .builder()
            .maxConnections(100)
            .socketTimeout(java.time.Duration.ofSeconds(240))
            .build()
        }
      } { client =>
        Sync[F].delay(client.close())
      }
      bedrockClient <- Resource.make {
        Sync[F].delay {
          BedrockRuntimeClient
            .builder()
            .credentialsProvider(ProfileCredentialsProvider.create(profileName))
            .httpClient(httpClient)
            .region(region)
            .build()
        }
      } { client =>
        Sync[F].delay(client.close())
      }
    } yield AwsBedrockDocsEvaluator(bedrockClient)
  }

  def createMessage(allDocs: List[DocsFile]): Message = {
    // TODO: build our prompt, including the provided docs files
    Message.builder
      .content(
        ContentBlock.fromText(
        // format: off
        s"""<context>
           |Your task is to evaluate the quality of developer documentation.
           |
           |The following documentation files are drawn from a Github repository's README(s) and other documentation files, and our goal is to decide how useful these documentation files will be to other engineers on the team and around the business.
           |
           |The contents are xml-escaped, so you'll see "&lt;" instead of "<" and "&gt;" instead of ">".
           |</context>
           |
           |<documents>
           |${
          allDocs.zipWithIndex
            .map { (docsFile, i) =>
              s"""<document index="${i + 1}">
                 |<source>${docsFile.path}</source>
                 |<document_content>
                 |${escapeXml(docsFile.content)}
                 |</document_content>
                 |""".stripMargin
            }
            .mkString("\n")
          }
           |</documents>
           |
           |<instructions>
           |We'll be evaluating the documentation against the following categories, these are grouped into named sections that start with a comment //
           |
           |// documentation basics
           |- description
           |- howToRunLocally
           |- howToRunInProd
           |- howToDeploy
           |- howToTest
           |// Software development lifecycle help
           |- howToContribute
           |- howToReportIssues
           |- howToGetHelp
           |// high-level overviews
           |- architectureOverview
           |- dataFlowOverview
           |// detailed technical information
           |- understandingCode
           |- understandingDependencies
           |- understandingTests
           |- understandingPerformance
           |- understandingSecurity
           |- understandingMonitoring
           |- understandingLogging
           |// more detailed data information
           |- understandingDataStorage
           |- understandingDataProcessing
           |- understandingDataTransfer
           |- understandingDataAccess
           |- understandingDataRetention
           |- understandingDataDeletion
           |- understandingDataBackup
           |
           |Please read the provided documentation and tell us whether it addresses each category by classifying it in one of three ways:
           |- Good
           |- May need improvement | <why>
           |- Missing
           |
           |A classification of "Good" implies that the provided documentation clearly addresses this section.
           |In the case of "May need improvement", the documentation touches on this point but lacks clarity or completeness. Please provide a one sentence summary of how this could be improved.
           |Finally, if the documentation doesn't address this category at all, we'll classify it as "Missing".
           |
           |The repository's documentation will be provided in xml tags for you, with a <documentation> tag wrapping all the files, and a <file> tag for each of the individual documentation files. The file tag contains an attribute for that file's path in the repository.
           |
           |Please use <thinking> tags to consider each point in turn, before writing your final output.
           |
           |Your evaluation needs to appear in a specific format, which should appear within <evaluation> tags after you are done thinking. Please do not elaborate within the classification, stick to precisely the example format.
           |
           |</instructions>
           |<example>
           |<thinking>
           |Your summary for each point can go here...
           |</thinking>
           |<evaluation>
           |description: Good
           |howToRunLocally: Good
           |howToRunInProd: Good
           |howToDeploy: Missing
           |howToTest: May need improvement | Needs more detail on how to run the tests
           |howToContribute: Good
           |howToReportIssues: Good
           |howToGetHelp: Good
           |architectureOverview: May need improvement | does this service run in EC2, or as a Lambda function?
           |dataFlowOverview: Good
           |understandingCode: May need improvement | It would help to provide more information on the subprojects in this repository
           |understandingDependencies: Good
           |understandingTests: Good
           |understandingPerformance: Good
           |understandingSecurity: Good
           |understandingMonitoring: Missing
           |understandingLogging: Missing
           |understandingDataStorage: Good
           |understandingDataProcessing: Good
           |understandingDataTransfer: Good
           |understandingDataAccess: Missing
           |understandingDataRetention: Good
           |understandingDataDeletion: Good
           |understandingDataBackup: Good
           |</evaluation>
           |</example>
           |""".stripMargin
        // format: on
        )
      )
      .role(ConversationRole.USER)
      .build
  }

  object Parser {

    import fastparse._, NoWhitespace._

    def parseBedrockResponse[F[_]: MonadThrow](
        response: String
    ): F[DocsEvaluation] = {
      parseEvaluation(
        response.linesIterator
          .dropWhile(_ != "<evaluation>")
          .drop(1)
          .takeWhile(_ != "</evaluation>")
          .mkString("\n")
      )
    }

    private[docs] def parseEvaluation[F[_]: MonadThrow](
        evaluation: String
    ): F[DocsEvaluation] = {
      parse(
        evaluation,
        docsEvaluationParser(using _),
        verboseFailures = true
      ) match {
        case Parsed.Success(docsEvaluation, _) =>
          MonadThrow[F].pure(docsEvaluation)
        case f @ Parsed.Failure(label, index, _) =>
          MonadThrow[F].raiseError(
            new RuntimeException(
              s"Could not parse evaluation response at index $index $label ${f.msg}"
            )
          )
      }
    }

    private def docsEvaluationParser[$: P]: P[DocsEvaluation] = {
      P(
        basicsParser ~/ contributingParser ~/ architectureParser ~/ technicalDetailParser ~/ dataGovernanceParser ~/ End
      ).map {
        (basics, contributing, architecture, technicalDetail, dataGovernance) =>
          DocsEvaluation(
            basics,
            contributing,
            architecture,
            technicalDetail,
            dataGovernance
          )
      }
    }

    private def basicsParser[$: P]: P[DocsBasicsEvaluation] = {
      P(
        // format: off
        "description: " ~ docsQualityParser ~/
          "howToRunLocally: " ~ docsQualityParser ~/
          "howToRunInProd: " ~ docsQualityParser ~/
          "howToDeploy: " ~ docsQualityParser ~/
          "howToTest: " ~ docsQualityParser
        // format: on
      ).map {
        case (
              description,
              howToRunLocally,
              howToRunInProd,
              howToDeploy,
              howToTest
            ) =>
          DocsBasicsEvaluation(
            description,
            howToRunLocally,
            howToRunInProd,
            howToDeploy,
            howToTest
          )
      }
    }

    private def contributingParser[$: P]: P[ContributingEvaluation] = {
      P(
        // format: off
        "howToContribute: " ~ docsQualityParser ~/
          "howToReportIssues: " ~ docsQualityParser ~/
          "howToGetHelp: " ~ docsQualityParser
        // format: on
      ).map { case (howToContribute, howToReportIssues, howToGetHelp) =>
        ContributingEvaluation(
          howToContribute,
          howToReportIssues,
          howToGetHelp
        )
      }
    }

    private def architectureParser[$: P]: P[ArchitectureEvaluation] = {
      P(
        // format: off
        "architectureOverview: " ~ docsQualityParser ~/
          "dataFlowOverview: " ~ docsQualityParser
        // format: on
      ).map { case (architectureOverview, dataFlowOverview) =>
        ArchitectureEvaluation(
          architectureOverview,
          dataFlowOverview
        )
      }
    }

    private def technicalDetailParser[$: P]: P[TechnicalDetailEvaluation] = {
      P(
        // format: off
        "understandingCode: " ~ docsQualityParser ~/
          "understandingDependencies: " ~ docsQualityParser ~/
          "understandingTests: " ~ docsQualityParser ~/
          "understandingPerformance: " ~ docsQualityParser ~/
          "understandingSecurity: " ~ docsQualityParser ~/
          "understandingMonitoring: " ~ docsQualityParser ~/
          "understandingLogging: " ~ docsQualityParser
        // format: on
      ).map {
        case (
              understandingCode,
              understandingDependencies,
              understandingTests,
              understandingPerformance,
              understandingSecurity,
              understandingMonitoring,
              understandingLogging
            ) =>
          TechnicalDetailEvaluation(
            understandingCode,
            understandingDependencies,
            understandingTests,
            understandingPerformance,
            understandingSecurity,
            understandingMonitoring,
            understandingLogging
          )
      }
    }

    private def dataGovernanceParser[$: P]: P[DataGovernanceEvaluation] = {
      P(
        // format: off
        "understandingDataStorage: " ~ docsQualityParser ~/
          "understandingDataProcessing: " ~ docsQualityParser ~/
          "understandingDataTransfer: " ~ docsQualityParser ~/
          "understandingDataAccess: " ~ docsQualityParser ~/
          "understandingDataRetention: " ~ docsQualityParser ~/
          "understandingDataDeletion: " ~ docsQualityParser ~/
          "understandingDataBackup: " ~ docsQualityParser
        // format: on
      ).map {
        case (
              understandingDataStorage,
              understandingDataProcessing,
              understandingDataTransfer,
              understandingDataAccess,
              understandingDataRetention,
              understandingDataDeletion,
              understandingDataBackup
            ) =>
          DataGovernanceEvaluation(
            understandingDataStorage,
            understandingDataProcessing,
            understandingDataTransfer,
            understandingDataAccess,
            understandingDataRetention,
            understandingDataDeletion,
            understandingDataBackup
          )
      }
    }

    private def docsQualityParser[$: P]: P[DocsQuality] = {
      P((mayNeedImprovementParser | goodParser | missingParser) ~ "\n".?)
    }

    private def mayNeedImprovementParser[$: P]
        : P[DocsQuality.MayNeedImprovement] = {
      P("May need improvement | " ~ CharPred(_ != '\n').rep.!).map { chars =>
        DocsQuality.MayNeedImprovement(chars.mkString)
      }
    }

    private def goodParser[$: P]: P[DocsQuality.Good.type] = {
      P("Good").map(_ => DocsQuality.Good)
    }

    private def missingParser[$: P]: P[DocsQuality.Missing.type] = {
      P("Missing").map(_ => DocsQuality.Missing)
    }
  }

  def escapeXml(content: String): String = {
    content
//      .replace("&", "&amp;")
      .replace("<", "&lt;")
      .replace(">", "&gt;")
//      .replace("\"", "&quot;")
//      .replace("'", "&apos;")
  }
}
