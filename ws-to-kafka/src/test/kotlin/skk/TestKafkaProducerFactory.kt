package skk

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
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
class TestKafkaProducerFactory {

    @Bean
    fun kafkaConfig(testKafkaContainer: TestKafkaContainer): KafkaConfig {
        return KafkaConfig(testKafkaContainer.bootstrapServers)
    }

    @Bean
    fun schemaRegistryConfig(testSchemaRegistryContainer: TestSchemaRegistryContainer): SchemaRegistryConfig {
        return SchemaRegistryConfig("http://${testSchemaRegistryContainer.host}:${testSchemaRegistryContainer.firstMappedPort}")
    }

    @Bean
    @Primary
    fun kafkaTopicConfig(): KafkaTopicConfig {
        return KafkaTopicConfig(1, 3, "mytopic")
    }

}
