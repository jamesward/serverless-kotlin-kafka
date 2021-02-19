package skk

import io.confluent.kafka.serializers.json.KafkaJsonSchemaSerializer
import org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaAdmin
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.stereotype.Component
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.Network
import org.testcontainers.utility.DockerImageName
import javax.annotation.PreDestroy


@Component
class TestZookeeperContainer : GenericContainer<TestZookeeperContainer>(DockerImageName.parse("confluentinc/cp-zookeeper:6.1.0")) {

    val testNetwork: Network = Network.newNetwork()

    init {
        withNetwork(testNetwork)
        withNetworkAliases("zookeeper")
        withEnv("ZOOKEEPER_CLIENT_PORT", "2181")
        start()
    }

    @PreDestroy
    fun destroy() {
        stop()
    }

}

@Component
class TestKafkaContainer(testZookeeperContainer: TestZookeeperContainer) : KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:6.1.0")) {

    init {
        withNetwork(testZookeeperContainer.network)
        withNetworkAliases("broker")
        withExternalZookeeper("zookeeper:2181")
        start()
    }

    @PreDestroy
    fun destroy() {
        stop()
    }

}

@Component
class TestSchemaRegistryContainer(testKafkaContainer: TestKafkaContainer) : GenericContainer<TestZookeeperContainer>(DockerImageName.parse("confluentinc/cp-schema-registry:6.1.0")) {

    init {
        withCreateContainerCmdModifier { it.withName("schema-registry") }
        withNetwork(testKafkaContainer.network)
        withExposedPorts(8081)
        withEnv("SCHEMA_REGISTRY_HOST_NAME", "schema-registry")
        withEnv("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS", "broker:9092")
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
    fun producerFactory(testKafkaContainer: TestKafkaContainer, testSchemaRegistryContainer: TestSchemaRegistryContainer, kafkaProperties: KafkaProperties): ProducerFactory<String, Question> {
        // todo: maybe reuse application.properties for serializers but not auth stuff?
        val config = mapOf(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to testKafkaContainer.bootstrapServers,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to KafkaJsonSchemaSerializer::class.java,
            "schema.registry.url" to "http://${testSchemaRegistryContainer.host}:${testSchemaRegistryContainer.firstMappedPort}",
        )
        return DefaultKafkaProducerFactory(config)
    }

    @Bean
    fun kafkaTemplate(testKafkaContainer: TestKafkaContainer, testSchemaRegistryContainer: TestSchemaRegistryContainer, kafkaProperties: KafkaProperties): KafkaTemplate<String, Question> {
        return KafkaTemplate(producerFactory(testKafkaContainer, testSchemaRegistryContainer, kafkaProperties))
    }

    @Bean
    fun kafkaAdmin(container: TestKafkaContainer): KafkaAdmin {
        val config = mapOf(BOOTSTRAP_SERVERS_CONFIG to container.bootstrapServers)
        return KafkaAdmin(config)
    }

}
