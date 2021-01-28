package skk

import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.stereotype.Component
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.utility.DockerImageName
import javax.annotation.PreDestroy

@Component
class TestKafkaContainer : KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:5.4.3")) {

    init {
        start()
    }

    @PreDestroy
    fun destroy() {
        stop()
    }

}

@Configuration
@EnableKafka
class TestKafkaProducerFactory {



    @Bean
    fun producerFactory(container: TestKafkaContainer): ProducerFactory<String, String> {
        val config = mapOf(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to container.bootstrapServers,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
        )
        return DefaultKafkaProducerFactory(config)
    }

    @Bean
    fun kafkaTemplate(container: TestKafkaContainer): KafkaTemplate<String, String> {
        return KafkaTemplate(producerFactory(container))
    }

}
