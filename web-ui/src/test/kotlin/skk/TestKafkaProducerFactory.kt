package skk


import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.utility.DockerImageName
import reactor.kafka.receiver.ReceiverOptions
import javax.annotation.PreDestroy


@Component
class TestKafkaContainer : KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:5.4.3")) {

    init {
        withCreateContainerCmdModifier { it.withName("kafka") }
        start()
    }

    @PreDestroy
    fun destroy() {
        stop()
    }

}


@Configuration
class TestKafkaReceiverOptions {

    @Bean
    fun receiverOptions(testKafkaContainer: TestKafkaContainer): ReceiverOptions<String, String> {
        val props = mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to testKafkaContainer.bootstrapServers,
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.GROUP_ID_CONFIG to "test-group",
        )

        return ReceiverOptions.create(props)
    }

}
