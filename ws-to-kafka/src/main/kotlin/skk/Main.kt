package skk

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.runBlocking
import org.apache.kafka.clients.admin.NewTopic
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.TopicBuilder
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaAdmin
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import java.net.URI


@SpringBootApplication
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

@Configuration
class KafkaSetup {

  @Bean
  fun newTopic(kafkaTopicConfig: KafkaTopicConfig): NewTopic {
    return TopicBuilder.name(kafkaTopicConfig.name).partitions(kafkaTopicConfig.partitions).replicas(kafkaTopicConfig.replicas).build()
  }

  @Bean
  fun kafkaAdmin(kafkaConfig: KafkaConfig): KafkaAdmin {
    return KafkaAdmin(kafkaConfig.config)
  }

  @Bean
  fun producerFactory(kafkaProperties: KafkaProperties, kafkaConfig: KafkaConfig, schemaRegistryConfig: SchemaRegistryConfig): ProducerFactory<String, Question> {
    val config = kafkaProperties.buildProducerProperties() + kafkaConfig.config + schemaRegistryConfig.config
    return DefaultKafkaProducerFactory(config)
  }

  @Bean
  fun kafkaTemplate(producerFactory: ProducerFactory<String, Question>): KafkaTemplate<String, Question> {
    return KafkaTemplate(producerFactory)
  }

}

fun main(args: Array<String>) {
  runApplication<Main>(*args)
}
