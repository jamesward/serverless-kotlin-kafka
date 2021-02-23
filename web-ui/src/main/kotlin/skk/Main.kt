package skk

import io.confluent.developer.ksqldb.reactor.ReactorClient
import io.confluent.ksql.api.client.ClientOptions
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.html.dom.serialize
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.event.ApplicationReadyEvent
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
import java.net.URL
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component


@SpringBootApplication
@RestController
@EnableConfigurationProperties(KafkaTopicConfig::class)
class WebApp(val reactorClient: ReactorClient) {

    @GetMapping("/")
    fun index(): String {
        return Html.index.serialize(true)
    }

    @GetMapping("/total")
    suspend fun total() = run {
        val rows = reactorClient.executeQuery("SELECT * FROM TOTALS WHERE ONE = 1;").awaitFirstOrNull()
        rows?.firstOrNull()?.getLong("TOTAL") ?: 0L
    }

    @GetMapping("/{name}")
    fun lang(@PathVariable name: String): String {
        return Html.lang(name).serialize(true)
    }

}

@Component
class KsqldbSetup(val reactorClient: ReactorClient, val kafkaTopicConfig: KafkaTopicConfig) {

    // possible race
    @EventListener(ApplicationReadyEvent::class)
    fun initialize() {
        val stackoverflowStream = "CREATE STREAM IF NOT EXISTS STACKOVERFLOW WITH (KAFKA_TOPIC='${kafkaTopicConfig.name}', VALUE_FORMAT='JSON_SR');"
        reactorClient.executeStatement(stackoverflowStream).block()

        val stackoverflowAllStream = "CREATE STREAM IF NOT EXISTS STACKOVERFLOW_ALL AS SELECT 1 AS ONE, FAVORITE_COUNT FROM STACKOVERFLOW;"
        reactorClient.executeStatement(stackoverflowAllStream).block()

        val stackoverflowExplodedStream = """
            CREATE STREAM IF NOT EXISTS TAGS AS
              SELECT
                TITLE, BODY, URL, VIEW_COUNT, FAVORITE_COUNT, EXPLODE(STACKOVERFLOW.TAGS) TAG
              FROM
                STACKOVERFLOW
              EMIT CHANGES;
        """.trimIndent()
        reactorClient.executeStatement(stackoverflowExplodedStream).block()

        val tagsQuestionsTable = """
            CREATE TABLE IF NOT EXISTS TAGS_QUESTIONS AS
              SELECT
                TAG,
                COUNT(*) QUESTION_COUNT
              FROM
                TAGS
              GROUP BY TAG
              EMIT CHANGES;
        """.trimIndent()
        reactorClient.executeStatement(tagsQuestionsTable).block()

        val stackoverflowTotalsTable = "CREATE TABLE IF NOT EXISTS TOTALS AS SELECT ONE, SUM(FAVORITE_COUNT) AS TOTAL FROM STACKOVERFLOW_ALL GROUP BY ONE EMIT CHANGES;"
        reactorClient.executeStatement(stackoverflowTotalsTable, mapOf("auto.offset.reset" to "earliest")).block()
    }

}


@Configuration
class KafkaConfigFactory {

    @Bean
    @ConditionalOnProperty(name = ["ksqldb.endpoint", "ksqldb.username", "ksqldb.password"])
    fun ksqlReactorClient(
        @Value("\${ksqldb.endpoint}") ksqldbEndpoint: URL,
        @Value("\${ksqldb.username}") ksqldbUsername: String,
        @Value("\${ksqldb.password}") ksqldbPassword: String,
    ): ReactorClient {
        val options = ClientOptions.create()
            .setHost(ksqldbEndpoint.host)
            .setPort(ksqldbEndpoint.port)
            .setUseTls(true)
            .setUseAlpn(true)
            .setBasicAuthCredentials(ksqldbUsername, ksqldbPassword)

        return ReactorClient.fromOptions(options)
    }

}

@Configuration
class WebSocketConfig {

    @Bean
    fun simpleUrlHandlerMapping(reactorClient: ReactorClient): SimpleUrlHandlerMapping {
        return SimpleUrlHandlerMapping(mapOf(
            "/langs" to langs(reactorClient),
        ), 0)
    }

    fun langs(reactorClient: ReactorClient): WebSocketHandler {
        return WebSocketHandler { session: WebSocketSession ->
            val query = "SELECT * FROM TAGS_QUESTIONS EMIT CHANGES;"

            val kafkaMessages = reactorClient.streamQuery(query)
            val webSocketMessages = kafkaMessages.map { message ->
                val lang = message.getString("TAG")
                val num = message.getInteger("QUESTION_COUNT")
                session.textMessage("$lang:$num")
            }
            session.send(webSocketMessages)
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
