package skk

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import java.net.URI


@SpringBootApplication
@EnableConfigurationProperties(KafkaTopicConfig::class)
class Main(
  @Value("\${serverless.kotlin.kafka.so-to-ws.url}") val soToWsUrl: String,
  val kafkaTopicConfig: KafkaTopicConfig,
  val kafkaTemplate: KafkaTemplate<String, Question>,
) : CommandLineRunner {

  val wsClient = ReactorNettyWebSocketClient()
  val mapper = jacksonObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)

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
