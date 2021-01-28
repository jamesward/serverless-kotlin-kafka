package skk

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.runBlocking
import org.apache.kafka.clients.admin.NewTopic
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

// todo: avro, json schema, or protobuf
data class Question(
    val url: String,
    val title: String,
    val favoriteCount: Int,
    val viewCount: Int,
    val tags: List<String>, // todo: deserialize ["tag1|tag2"]
    val body: String
)

// todo: configurable topic name
@Configuration
class KafkaConfig {

    @Bean
    fun newTopic(): NewTopic? {
        return TopicBuilder.name("mytopic").partitions(8).replicas(3).build()
    }

}

@SpringBootApplication
class Main(val kafkaTemplate: KafkaTemplate<String, String>) : CommandLineRunner {

    val wsClient = ReactorNettyWebSocketClient()
    val mapper = jacksonObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)

    override fun run(vararg args: String?): Unit = runBlocking {
        val uri = URI("wss://stackoverflow-to-ws-u7lmfx4izq-uc.a.run.app/questions")

        val send: (WebSocketMessage) -> Unit =  { message ->
            val json = message.payloadAsText
            val question = mapper.readValue<Question>(json)
            println(question.url)
            kafkaTemplate.send("mytopic", question.url, json)
        }

        wsClient.execute(uri) { session ->
            session.receive().doOnNext(send).then()
        }.block()
    }

}

fun main(args: Array<String>) {
    runApplication<Main>(*args)
}
