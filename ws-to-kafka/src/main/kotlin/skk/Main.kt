package skk

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.runBlocking
import org.apache.kafka.clients.admin.NewTopic
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.kafka.config.TopicBuilder
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import java.io.IOException
import java.net.URI

class QuestionBodyDeserializer : JsonDeserializer<String>() {
  @Throws(IOException::class, JsonProcessingException::class)
  override fun deserialize(parser: JsonParser, context: DeserializationContext): String {
    return parser.text.escapeHTML()
  }
}

class QuestionTagsDeserializer : JsonDeserializer<List<String>>() {
  @Throws(IOException::class, JsonProcessingException::class)
  @Suppress("UNCHECKED_CAST")
  override fun deserialize(parser: JsonParser, context: DeserializationContext): List<String> {
    // data is [foo|bar] so we need to manually split it
    val deserializer: JsonDeserializer<Any> = context.findRootValueDeserializer(context.constructType(List::class.java))
    val maybeList = deserializer.deserialize(parser, context) as? List<String>
    return maybeList?.let { it.firstOrNull()?.split('|') } ?: emptyList()
  }
}

// JSON-Schema will generated from POKO in SR by Serializer
data class Question(
  val url: String,
  val title: String,
  @JsonProperty("favorite_count") val favoriteCount: Int,
  @JsonProperty("view_count") val viewCount: Int,
  @JsonDeserialize(using = QuestionTagsDeserializer::class) val tags: List<String>,
  @JsonDeserialize(using = QuestionBodyDeserializer::class) val body: String
)

@ConstructorBinding
@ConfigurationProperties("serverless.kotlin.kafka.mytopic")
data class KafkaTopicConfig(
  val replicas: Int = 1,
  val partitions: Int = 3,
  val name: String) {

  @Bean
  fun newTopic(): NewTopic {
    return TopicBuilder.name(name).partitions(partitions).replicas(replicas).build()
  }

}


@SpringBootApplication
@EnableConfigurationProperties(KafkaTopicConfig::class)
class Main(
  @Value("\${serverless.kotlin.kafka.so-to-ws.url}") val soToWsUrl: String,
  val kafkaTopicConfig: KafkaTopicConfig,
  val kafkaTemplate: KafkaTemplate<String, Question>,
) : CommandLineRunner {

  val wsClient = ReactorNettyWebSocketClient()
  val mapper: ObjectMapper = jacksonObjectMapper()
    .registerModule(KotlinModule())
    .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)

  override fun run(vararg args: String?): Unit = runBlocking {
    val uri = URI(soToWsUrl)

    val send: (WebSocketMessage) -> Unit = { message ->

      // parsing string to Kotlin object
      val question = mapper.readValue<Question>(message.payloadAsText)
      println(question.url)

      //sending to Kafka topic
      kafkaTemplate.send(kafkaTopicConfig.name, question.url, question)
    }

    wsClient.execute(uri) { session ->
      session.receive().doOnNext(send).then()
    }.block()
  }

}

fun main(args: Array<String>) {
  runApplication<Main>(*args)
}
