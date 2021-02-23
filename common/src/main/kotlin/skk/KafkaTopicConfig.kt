package skk

import org.apache.kafka.clients.admin.NewTopic
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.context.annotation.Bean
import org.springframework.kafka.config.TopicBuilder

@ConstructorBinding
@ConfigurationProperties("serverless.kotlin.kafka.mytopic")
data class KafkaTopicConfig(
    val replicas: Int = 1,
    val partitions: Int = 3,
    val name: String) {

    @Bean
    fun newTopic(): NewTopic {
        return TopicBuilder.name(name).partitions(partitions).replicas(replicas).build()
    }

}
