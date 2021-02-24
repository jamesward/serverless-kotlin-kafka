package skk

import io.confluent.ksql.api.client.Client
import kotlinx.coroutines.future.await
import kotlinx.html.dom.serialize
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketSession
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.core.publisher.toMono


@SpringBootApplication
@RestController
class WebApp(val client: Client) {

    @GetMapping("/")
    fun index(): String {
        return Html.index.serialize(true)
    }

    @GetMapping("/total")
    suspend fun total() = run {
        val rows = client.executeQuery("SELECT * FROM TOTALS WHERE ONE = 1;").await()
        rows?.firstOrNull()?.getLong("TOTAL") ?: 0L
    }

    @GetMapping("/{name}")
    fun lang(@PathVariable name: String): String {
        return Html.lang(name).serialize(true)
    }

}


@Configuration
class WebSocketConfig {

    @Bean
    fun simpleUrlHandlerMapping(client: Client): SimpleUrlHandlerMapping {
        return SimpleUrlHandlerMapping(mapOf(
            "/langs" to langs(client),
        ), 0)
    }

    fun langs(client: Client): WebSocketHandler {
        return WebSocketHandler { session: WebSocketSession ->
            val query = "SELECT * FROM TAGS_QUESTIONS EMIT CHANGES;"

            client.streamQuery(query).toMono().flatMap { kafkaMessages ->
                val webSocketMessages = kafkaMessages.toFlux().map { message ->
                    val lang = message.getString("TAG")
                    val num = message.getInteger("QUESTION_COUNT")
                    session.textMessage("$lang:$num")
                }
                session.send(webSocketMessages)
            }

        }
    }

    @Bean
    fun webSocketHandlerAdapter(): WebSocketHandlerAdapter {
        return WebSocketHandlerAdapter()
    }

}

fun main(args: Array<String>) {
    runApplication<WebApp>(*args)
}
