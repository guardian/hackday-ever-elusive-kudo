package com.adamnfish.tdc.docs

import cats.MonadThrow
import cats.effect.Resource
import cats.effect.std.Console
import cats.effect.kernel.Sync
import cats.syntax.all.*
import com.adamnfish.tdc.docs.DocsEvaluator.DocsEvaluation
import com.adamnfish.tdc.vcs.VcsInformation
import com.adamnfish.tdc.vcs.VcsInformation.DocsFile
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider
import software.amazon.awssdk.http.apache.ApacheHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient
import software.amazon.awssdk.services.bedrockruntime.model.*

import scala.jdk.CollectionConverters.*

class AwsBedrockDocsEvaluator[F[_]: Sync: MonadThrow : Console](
    bedrockRuntimeClient: BedrockRuntimeClient
) extends DocsEvaluator[F] {

  override def evaluateDocs(
      allDocs: List[VcsInformation.DocsFile]
  ): F[DocsEvaluation] = {
    val message = AwsBedrockDocsEvaluator.createMessage(allDocs)
    for {
      response <- Sync[F].blocking {
        bedrockRuntimeClient.converse {
          ConverseRequest
            .builder()
            .modelId(AwsBedrockDocsEvaluator.modelId)
            .messages(message)
            .inferenceConfig {
              InferenceConfiguration
                .builder()
                .maxTokens(512) // may not be enough!
                .temperature(0.5f)
                .topP(0.9f)
                .build()
            }
            .build()
        }
      }
      contentBlocks = response.output.message.content.asScala.toList
      content = contentBlocks.flatMap(cb => Option(cb.text())).mkString("")
      _ <- Console[F].println(content)
      // TODO: Work out the format we'll get back from Claude, and convert it to our type
    } yield DocsEvaluation.empty
  }
}
object AwsBedrockDocsEvaluator {
//  val modelId = "us.anthropic.claude-3-5-sonnet-20241022-v2:0"
  val modelId = "us.anthropic.claude-3-5-haiku-20241022-v1:0"

  def create[F[_]: Sync : Console](
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
      .content(ContentBlock.fromText(
        "Please tell me a short children's story about a rabbit that is enjoying a party, but is increasingly too tired to make the best of it. The rabbit has a nap and wakes up in time to properly enjoy the final song and cake!"
      ))
      .role(ConversationRole.USER)
      .build
  }
}
