package skk

import kotlinx.html.*
import kotlinx.html.dom.createHTMLDocument
import kotlinx.html.dom.serialize
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.IntegerDeserializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.bind.annotation.GetMapping
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

}


data class BootstrapServers(val stringList: String)


@Configuration
class WebSocketConfig {

    @Bean
    fun simpleUrlHandlerMapping(bootstrapServers: BootstrapServers): SimpleUrlHandlerMapping {
        return SimpleUrlHandlerMapping(mapOf(
            "/total" to totalFavorites(bootstrapServers),
            "/langs" to langs(bootstrapServers),
        ), 10)
    }

    fun totalFavorites(bootstrapServers: BootstrapServers): WebSocketHandler {

        val props = mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers.stringList,
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.GROUP_ID_CONFIG to "group",
        )

        return WebSocketHandler { session: WebSocketSession ->
            val receiverOptions = ReceiverOptions.create<String, String>(props)
                .consumerProperty(ConsumerConfig.CLIENT_ID_CONFIG, session.id)
                .subscription(listOf("total"))

            val kafkaMessages = KafkaReceiver.create(receiverOptions).receive()

            val webSocketMessages = kafkaMessages.map { session.textMessage(it.value()) }

            session.send(webSocketMessages)
        }
    }


    fun langs(bootstrapServers: BootstrapServers): WebSocketHandler {

        val props = mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers.stringList,
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.GROUP_ID_CONFIG to "group",
        )

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

    val indexHTML: HTML.() -> Unit = {
        head {
            link("/webjars/bootstrap/4.5.3/css/bootstrap.min.css", LinkRel.stylesheet)
            link("/assets/index.css", LinkRel.stylesheet)
            script(ScriptType.textJavaScript) {
                src = "/assets/index.js"
            }
        }
        body {
            nav("navbar fixed-top navbar-light bg-light") {
                a("/", classes = "navbar-brand") {
                    +"Serverless Kotlin Kafka"
                }
            }

            div("container-fluid") {
                template {
                    id = "totalTemplate"
                    +"Total Favorites: {{total}}"
                }
                div {
                    id = "total"
                }
            }
        }
    }

    val index: Document = createHTMLDocument().html(block = indexHTML)

}

fun main(args: Array<String>) {
    runApplication<WebApp>(*args)
}
