package skk

import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.command.WaitContainerResultCallback
import com.github.dockerjava.api.model.Frame
import com.github.dockerjava.api.model.StreamType
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import java.net.URL
import javax.annotation.PreDestroy


@Component
class TestZookeeperContainer :
    GenericContainer<TestZookeeperContainer>(DockerImageName.parse("confluentinc/cp-zookeeper:6.1.0")) {

    val testNetwork: Network = Network.newNetwork()

    val port = 2181

    init {
        withNetwork(testNetwork)
        withEnv("ZOOKEEPER_CLIENT_PORT", port.toString())
        start()
    }

    @PreDestroy
    fun destroy() {
        stop()
    }

}

@Component
class TestKafkaContainer(testZookeeperContainer: TestZookeeperContainer) :
    KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:6.1.0")) {

    val port = 9092

    init {
        withNetwork(testZookeeperContainer.network)
        withExternalZookeeper(testZookeeperContainer.networkAliases.first() + ":" + testZookeeperContainer.port)
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
class TestSchemaRegistryContainer(testKafkaContainer: TestKafkaContainer) :
    GenericContainer<TestSchemaRegistryContainer>(DockerImageName.parse("confluentinc/cp-schema-registry:6.1.0")) {

    fun url() = "http://${networkAliases.first()}:${exposedPorts.first()}"

    init {
        withNetwork(testKafkaContainer.network)
        withExposedPorts(8081)
        withEnv("SCHEMA_REGISTRY_HOST_NAME", "schema-registry")
        withEnv("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS", "${testKafkaContainer.networkAliases.first()}:${testKafkaContainer.port}")
        start()
    }

    @PreDestroy
    fun destroy() {
        stop()
    }

}

@Component
class TestWsToKafkaContainer(testKafkaContainer: TestKafkaContainer, testSchemaRegistryContainer: TestSchemaRegistryContainer) :
    GenericContainer<TestWsToKafkaContainer>(DockerImageName.parse("ws-to-kafka")) {

    init {
        // todo: logger
        withNetwork(testKafkaContainer.network)
        withEnv("KAFKA_BOOTSTRAP_SERVERS", "${testKafkaContainer.networkAliases.first()}:${testKafkaContainer.port}")
        withEnv("SCHEMA_REGISTRY_URL", testSchemaRegistryContainer.url())
        withEnv("SERVERLESS_KOTLIN_KAFKA_MYTOPIC_REPLICAS", "1")
        withEnv("SERVERLESS_KOTLIN_KAFKA_MYTOPIC_PARTITIONS", "3")
        waitingFor(Wait.forLogMessage(".*Kafka startTimeMs.*", 1))
        start()
    }

    @PreDestroy
    fun destroy() {
        stop()
    }

}

@Component
class TestKsqlDbServerContainer(testKafkaContainer: TestKafkaContainer, testSchemaRegistryContainer: TestSchemaRegistryContainer, testWsToKafkaContainer: TestWsToKafkaContainer) :
    GenericContainer<TestKsqlDbServerContainer>(DockerImageName.parse("confluentinc/ksqldb-server:0.15.0")) {

    init {
        withNetwork(testKafkaContainer.network)
        withExposedPorts(8088)
        withEnv("KSQL_BOOTSTRAP_SERVERS", "${testKafkaContainer.networkAliases.first()}:${testKafkaContainer.port}")
        withEnv("KSQL_OPTS", "-Dksql.schema.registry.url=${testSchemaRegistryContainer.url()}")
        waitingFor(Wait.forLogMessage(".*INFO Server up and running.*\\n", 1))
        start()

        val ksqldbUrl = "http://${this.networkAliases.first()}:8088"

        val createKsqldbSetup = dockerClient
            .createContainerCmd("ksqldb-setup")
            .withNetworkMode(testKafkaContainer.network.id)
            .withEnv("KSQLDB_ENDPOINT=$ksqldbUrl")
            .exec()

        dockerClient.startContainerCmd(createKsqldbSetup.id).exec()

        val logger = object: ResultCallback.Adapter<Frame>() {
            override fun onNext(frame: Frame?) {
                when(frame?.streamType) {
                    // todo: logger?
                    StreamType.STDOUT, StreamType.STDERR -> print(String(frame.payload))
                }
            }
        }

        dockerClient.logContainerCmd(createKsqldbSetup.id).withStdErr(true).withStdOut(true).withFollowStream(true).withTailAll().exec(logger).awaitCompletion()

        val exit = dockerClient.waitContainerCmd(createKsqldbSetup.id).exec(WaitContainerResultCallback()).awaitStatusCode()

        if (exit > 0) {
            throw Exception("Could not run ksqldb-setup")
        }
    }

    @PreDestroy
    fun destroy() {
        stop()
    }

}


@Configuration
class TestKafkaConfigFactory {

    @Bean
    fun ksqldbConfig(testKsqlDbServerContainer: TestKsqlDbServerContainer): KsqldbConfig {
        return KsqldbConfig(URL("http", testKsqlDbServerContainer.host, testKsqlDbServerContainer.firstMappedPort, ""))
    }

    @Bean
    @Primary
    fun kafkaTopicConfig(): KafkaTopicConfig {
        return KafkaTopicConfig(1, 3, "testtopic")
    }

}
