package skk

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.runBlocking
import org.apache.kafka.clients.admin.NewTopic
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.TopicBuilder
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import java.net.URI

// JSON-Schema will generated from POKO in SR by Serializer
data class Question(
  val url: String,
  val title: String,
  @JsonProperty("favorite_count") val favoriteCount: Int,
  @JsonProperty("view_count") val viewCount: Int,
  var tags: List<String>,
  var body: String
)

@Configuration
class KafkaConfig {

  @Value("\${serverless.kotlin.kafka.mytopic.replicas:1}")
  var topicReplicas: Int = 0

  @Value("\${serverless.kotlin.kafka.mytopic.partitions:3}")
  var topicPartitions: Int = 0

  @Value("\${serverless.kotlin.kafka.mytopic.name}")
  lateinit var topicName: String;

  @Bean
  fun newTopic(): NewTopic {
    return TopicBuilder.name(topicName).partitions(topicPartitions).replicas(topicReplicas).build()
  }

}

@SpringBootApplication
class Main(
  @Value("\${serverless.kotlin.kafka.so-to-ws.url}") val soToWsUrl: String,
  @Value("\${serverless.kotlin.kafka.mytopic.name}") val topicName: String,
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

      // optional escaping html so it won't break CLI tools
      question.body = question.body.escapeHTML()
      // parsing tags separated by `|`
      question.tags = question.tags.flatMap { it.split("|") }

      //sending to Kafka topic
      kafkaTemplate.send(topicName, question.url, question)
    }

    wsClient.execute(uri) { session ->
      session.receive().doOnNext(send).then()
    }.block()
  }

}

fun main(args: Array<String>) {
  runApplication<Main>(*args)
}
