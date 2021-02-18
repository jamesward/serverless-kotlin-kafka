package skk


import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.utility.DockerImageName
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
    fun kafkaConfig(testKafkaContainer: TestKafkaContainer): KafkaConfig {
        return KafkaConfig(testKafkaContainer.bootstrapServers)
    }

}
