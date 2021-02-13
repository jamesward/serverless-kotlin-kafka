package skk

import kotlinx.html.*
import kotlinx.html.dom.createHTMLDocument
import kotlinx.html.dom.serialize
import org.apache.kafka.clients.consumer.ConsumerConfig
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


@Configuration
class WebSocketConfig {

    @Bean
    fun simpleUrlHandlerMapping(wsh: WebSocketHandler): SimpleUrlHandlerMapping {
        return SimpleUrlHandlerMapping(mapOf("/websocket" to wsh), 10)
    }

    @Bean
    fun webSocketHandler(receiverOptions: ReceiverOptions<String, String>): WebSocketHandler {
        return WebSocketHandler { session: WebSocketSession ->
            val receiverOptionsWithId = receiverOptions.consumerProperty(ConsumerConfig.CLIENT_ID_CONFIG, session.id).subscription(listOf("myTopic"))

            val kafkaMessages = KafkaReceiver.create(receiverOptionsWithId).receive()

            val webSocketMessages = kafkaMessages.map { session.textMessage(it.value()) }

            session.send(webSocketMessages)
        }
    }

    @Bean
    fun webSocketHandlerAdapter(): WebSocketHandlerAdapter {
        return WebSocketHandlerAdapter()
    }

}


object Html {

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
                +"hello, world"
            }
        }
    }

    val index: Document = createHTMLDocument().html(block = indexHTML)

}

fun main(args: Array<String>) {
    runApplication<WebApp>(*args)
}
