package skk


import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.kjetland.jackson.jsonSchema.JsonSchemaGenerator
import io.confluent.developer.ksqldb.reactor.ReactorClient
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient
import io.confluent.kafka.schemaregistry.json.JsonSchema
import io.confluent.ksql.api.client.ClientOptions
import io.confluent.testcontainers.KsqlDbServerContainer
import org.apache.kafka.clients.admin.AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.KafkaAdmin
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
        withCreateContainerCmdModifier { it.withName("kafka") }
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
        withEnv("KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", "1")
        withEnv("KAFKA_TRANSACTION_STATE_LOG_MIN_ISR", "1")
        withEnv("KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR", "1")
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

@Component
class TestKsqlDbServerContainer(testKafkaContainer: TestKafkaContainer, testSchemaRegistryContainer: TestSchemaRegistryContainer) : KsqlDbServerContainer("0.15.0") {

    init {
        withKafka(testKafkaContainer)
        withNetwork(testKafkaContainer.network)
        withEnv("KSQL_OPTS", "-Dksql.schema.registry.url=http://schema-registry:8081")
        start()
    }

    @PreDestroy
    fun destroy() {
        stop()
    }

}

@Configuration
class TestKafkaConfigFactory {

    @Bean
    fun ksqlReactorClient(testKsqlDbServerContainer: TestKsqlDbServerContainer, testSchemaRegistryContainer: TestSchemaRegistryContainer, kafkaTopicConfig: KafkaTopicConfig): ReactorClient {

        // manual json schema registration
        val jsonMapper = jacksonObjectMapper()
        val jsonSchemaGenerator = JsonSchemaGenerator(jsonMapper)
        val jsonSchema = jsonSchemaGenerator.generateJsonSchema(Question::class.java)
        val schema = JsonSchema(jsonSchema)

        val client = CachedSchemaRegistryClient("http://" + testSchemaRegistryContainer.host + ":" + testSchemaRegistryContainer.firstMappedPort, 64)
        client.register("${kafkaTopicConfig.name}-value", schema)

        val options = ClientOptions.create()
            .setHost(testKsqlDbServerContainer.host)
            .setPort(testKsqlDbServerContainer.firstMappedPort)

        return ReactorClient.fromOptions(options)
    }

    @Bean
    fun kafkaAdmin(container: TestKafkaContainer): KafkaAdmin {
        val config = mapOf(
            BOOTSTRAP_SERVERS_CONFIG to container.bootstrapServers
        )
        return KafkaAdmin(config)
    }
}
