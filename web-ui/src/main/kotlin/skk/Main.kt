package skk

import kotlinx.html.*
import kotlinx.html.dom.createHTMLDocument
import kotlinx.html.dom.serialize
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketSession
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter
import org.w3c.dom.Document
import reactor.kafka.receiver.KafkaReceiver
import reactor.kafka.receiver.ReceiverOptions


@SpringBootApplication
@RestController
class WebApp {

    @GetMapping("/")
    fun index(): String {
        return Html.index.serialize(true)
    }

    @GetMapping("/{name}")
    fun lang(@PathVariable name: String): String {
        return Html.lang(name).serialize(true)
    }

}

data class KafkaConfig(val bootstrapServers: String, val username: String? = null, val password: String? = null) {
    private val maybeAuthProps = username?.let { u ->
        password?.let { p ->
            mapOf(
                SaslConfigs.SASL_MECHANISM to "PLAIN",
                SaslConfigs.SASL_JAAS_CONFIG to "org.apache.kafka.common.security.plain.PlainLoginModule required username='$u' password='$p';",
                SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG to "https",
                CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to "SASL_SSL",
            )
        }
    }.orEmpty()

    val props = mapOf(
        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
    ) + maybeAuthProps
}

@Configuration
class KafkaConfigFactory {

    @Bean
    @ConditionalOnProperty(name = ["kafka.bootstrap.servers"])
    fun kafkaConfig(@Value("\${kafka.bootstrap.servers}") bootstrapServers: String): KafkaConfig {
        return KafkaConfig(bootstrapServers)
    }

    @Bean
    @Primary
    @ConditionalOnProperty(name = ["kafka.bootstrap.servers", "kafka.username", "kafka.password"])
    fun kafkaConfigWithAuth(
        @Value("\${kafka.bootstrap.servers}") bootstrapServers: String,
        @Value("\${kafka.username}") username: String?,
        @Value("\${kafka.password}") password: String?
    ): KafkaConfig {
        return KafkaConfig(bootstrapServers, username, password)
    }

}

@Configuration
class WebSocketConfig {

    @Bean
    fun simpleUrlHandlerMapping(kafkaConfig: KafkaConfig): SimpleUrlHandlerMapping {
        println(kafkaConfig.props)

        return SimpleUrlHandlerMapping(mapOf(
            "/total" to totalFavorites(kafkaConfig),
            "/langs" to langs(kafkaConfig),
        ), 0)
    }

    fun totalFavorites(kafkaConfig: KafkaConfig): WebSocketHandler {

        val props = mapOf(
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.GROUP_ID_CONFIG to "group",
        ) + kafkaConfig.props

        return WebSocketHandler { session: WebSocketSession ->
            val receiverOptions = ReceiverOptions.create<String, String>(props)
                .consumerProperty(ConsumerConfig.CLIENT_ID_CONFIG, session.id)
                .subscription(listOf("total"))

            val kafkaMessages = KafkaReceiver.create(receiverOptions).receive()

            val webSocketMessages = kafkaMessages.map { session.textMessage(it.value()) }

            session.send(webSocketMessages)
        }
    }


    fun langs(kafkaConfig: KafkaConfig): WebSocketHandler {

        val props = mapOf(
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.GROUP_ID_CONFIG to "group",
        ) + kafkaConfig.props

        return WebSocketHandler { session: WebSocketSession ->
            val receiverOptions = ReceiverOptions.create<String, String>(props)
                .consumerProperty(ConsumerConfig.CLIENT_ID_CONFIG, session.id)
                .subscription(listOf("langs"))

            val kafkaMessages = KafkaReceiver.create(receiverOptions).receive()

            val webSocketMessages = kafkaMessages.map { session.textMessage("${it.key()}:${it.value()}") }

            session.send(webSocketMessages)
        }
    }


    @Bean
    fun webSocketHandlerAdapter(): WebSocketHandlerAdapter {
        return WebSocketHandlerAdapter()
    }

}


object Html {

    class TEMPLATE(consumer: TagConsumer<*>) :
        HTMLTag("template", consumer, emptyMap(),
            inlineTag = true,
            emptyTag = false), HtmlInlineTag

    fun FlowContent.template(block: TEMPLATE.() -> Unit = {}) {
        TEMPLATE(consumer).visit(block)
    }

    fun TEMPLATE.li(classes : String? = null, block : LI.() -> Unit = {}) {
        LI(attributesMapOf("class", classes), consumer).visit(block)
    }

    fun page(js: String, content: FlowContent.() -> Unit = {}): HTML.() -> Unit = {
        head {
            link("/webjars/bootstrap/4.5.3/css/bootstrap.min.css", LinkRel.stylesheet)
            link("/assets/index.css", LinkRel.stylesheet)
            script(ScriptType.textJavaScript) {
                src = "/assets/$js"
            }
        }
        body {
            nav("navbar fixed-top navbar-light bg-light") {
                a("/", classes = "navbar-brand") {
                    +"Serverless Kotlin Kafka"
                }
            }

            div("container-fluid") {
                content()
            }
        }
    }

    val indexHTML = page("index.js") {
        template {
            id = "total-template"
            +"Total Favorites: {{total}}"
        }

        div {
            id = "total"
        }

        ul {
            id = "recent-questions"

            template {
                id = "recent-questions-template"

                li {
                    id = "lang-{{lang}}"

                    a("{{lang}}") {
                        +"{{lang}} = {{num}}"
                    }
                }
            }
        }
    }

    val index: Document = createHTMLDocument().html(block = indexHTML)

    fun langHTML(name: String) = page("lang.js") {
        +"Questions For `$name`"

        ul {
            id = "questions"

            template {
                id = "question-template"

                li {
                    a("{{url}}") {
                        +"{{title}}"
                    }
                    +" (favorites: {{favorite_count}}, views: {{view_count}})"
                }
            }
        }
    }

    fun lang(name: String): Document = createHTMLDocument().html(block = langHTML(name))

}

fun main(args: Array<String>) {
    runApplication<WebApp>(*args)
}
