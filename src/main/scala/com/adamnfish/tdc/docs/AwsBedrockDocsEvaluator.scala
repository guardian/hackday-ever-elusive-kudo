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
           |We'll be evaluating the documentation against the following categories.
           |
           |# Documentation basics
           |
           |- description
           |    Does the documentation give a brief overview of what the codebase does, and why?
           |- howToRunLocally
           |    We would like to see a comprehensive description of how to run the system locally.
           |- howToRunInProd
           |    We'd like an overview of the real production environment.
           |- howToDeploy
           |    We generally assume deployments use our CI/CD platform "riff raff", but does this repository automatically deploy Pull Requests
           |    If any other information is provided, is it enough to explain the process?
           |- howToTest
           |    How would an engineer go about running the application's tests?
           |    Are any unusual factors properly explained?
           |
           |# Contributing
           |
           |This section is to help an engineer navigate the process of making changes, and getting support from others while doing so.
           |
           |- howToContribute
           |    TODO: this should probably go (it lives in CONTRIBUTING.md)
           |- howToReportIssues
           |    If a team name is provided, this is enough. Otherwise we'd like to see something GitHub issues or Trello as a way to make the maintainers aware of problems.
           |- howToGetHelp
           |    If a team name is provided, this is enough. If not, are there instructions on how someone contributing to the codebase can get technical support?
           |
           |# High-level overviews
           |
           |These overviews provide a information on this system and its place in the business.
           |
           |- architectureOverview
           |    We expect to have a summary of this service's components and how they interact with other systems.
           |    Please also check for any architecture diagrams embedded in the documentation.
           |- dataFlowOverview
           |    A description of where the data powering this system comes from, how it is processed in this system, and where any outputs go.
           |
           |# Detailed technical information
           |
           |This information exists to help an engineer that needs to do software development on the project, either to add a feature or maintain what is there.
           |
           |- understandingCode
           |    Does the README give an overview of the code's structure, in a way that would help an engineer arriving fresh to the repository?
           |- understandingDependencies
           |    Which software dependency system(s) does the repo use, and how are dependencies kept up to date?
           |- understandingTests
           |    Expanding on the above point about how to run the tests, is there any technical detail on how the tests are written?
           |- understandingPerformance
           |    Is this application performance-critical, and are there any specific performance issues to keep in mind when working on this repository?
           |- understandingSecurity
           |    What security considerations does this repository have? This might include IAM management, handling API keys, properly securing data, or things to think about in the code itself.
           |- understandingMonitoring
           |    Can an engineer get a sense of how the application is performing in production?
           |- understandingLogging
           |    Diving into a bit more detail on the above monitoring point, where can any logs for this system be found?
           |
           |# Data governance
           |
           |the following sections relate to the data this system handles in its operation. If the system does not handle any data, then there's no need for the documentation to go into more detail here.
           |
           |- understandingDataStorage
           |    Is it clear where and how this system stores data?
           |- understandingDataProcessing
           |    Does the documentation explain how the system processes any data inputs?
           |- understandingDataTransfer
           |    Expanding on the data flow overview above, is there more detailed information on any external systems where data gets sent?
           |- understandingDataAccess
           |    Are any access controls in place on data that this system stores?
           |- understandingDataRetention
           |    Has any consideration been given to how long data is kept in this system?
           |- understandingDataDeletion
           |    If data is not kept forever, how is it deleted?
           |- understandingDataBackup
           |    How do we ensure the integrity of any important system data?
           |
           |
           |Please read the provided documentation and tell us whether it addresses each category by classifying it in one of three ways:
           |- Good
           |- May need improvement | <why>
           |- Missing
           |
           |A classification of "Good" implies that the provided documentation clearly addresses this section, or that it does not apply.
           |In the case of "May need improvement", the documentation touches on this point but lacks clarity or completeness. Please provide a one sentence summary of how this could be improved.
           |Finally, if the documentation doesn't address this category at all, we'll classify it as "Missing".
           |
           |The repository's documentation will be provided in xml tags for you, with a <documentation> tag wrapping all the files, and a <file> tag for each of the individual documentation files. The file tag contains an attribute for that file's path in the repository.
           |
           |Please use <thinking> tags to consider each point in turn in your own words before writing your final output. You should at least give a summary of how the documentation meets each of the above sections, here.
           |
           |Your actual evaluation needs to appear in a specific format, which should appear within <evaluation> tags after you are done thinking. Please do not elaborate within the classification, stick to precisely the example format.
           |
           |</instructions>
           |<example>
           |<thinking>
           |# Documentation basics
           |...
           |
           |# Contributing
           |...
           |
           |# High-level overviews
           |...
           |
           |# Detailed technical information
           |...
           |
           |# Data governance
           |...
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
