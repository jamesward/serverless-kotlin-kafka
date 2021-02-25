package skk

import io.confluent.ksql.api.client.Client
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class Setup(val client: Client) : CommandLineRunner {

    override fun run(vararg args: String?): Unit = runBlocking {
        val stackoverflowStream = """
            CREATE STREAM IF NOT EXISTS STACKOVERFLOW WITH (KAFKA_TOPIC='mytopic', VALUE_FORMAT='JSON_SR');
        """.trimIndent()
        client.executeStatement(stackoverflowStream).await()

        val stackoverflowAllStream = """
            CREATE STREAM IF NOT EXISTS STACKOVERFLOW_ALL AS
              SELECT
                1 AS ONE, FAVORITE_COUNT
              FROM
                STACKOVERFLOW;
        """.trimIndent()
        client.executeStatement(stackoverflowAllStream).await()

        val stackoverflowExplodedStream = """
            CREATE STREAM IF NOT EXISTS TAGS AS
              SELECT
                TITLE, BODY, URL, VIEW_COUNT, FAVORITE_COUNT, EXPLODE(STACKOVERFLOW.TAGS) TAG
              FROM
                STACKOVERFLOW
              EMIT CHANGES;
        """.trimIndent()
        client.executeStatement(stackoverflowExplodedStream).await()

        val tagsQuestionsTable = """
            CREATE TABLE IF NOT EXISTS TAGS_QUESTIONS AS
              SELECT
                TAG,
                COUNT(*) QUESTION_COUNT
              FROM
                TAGS
              GROUP BY TAG
              EMIT CHANGES;
        """.trimIndent()
        client.executeStatement(tagsQuestionsTable).await()

        val stackoverflowTotalsTable = """
            CREATE TABLE IF NOT EXISTS TOTALS AS
              SELECT
                ONE, SUM(FAVORITE_COUNT) AS TOTAL
              FROM
                STACKOVERFLOW_ALL
              GROUP BY ONE
              EMIT CHANGES;
        """.trimIndent()
        client.executeStatement(stackoverflowTotalsTable, mapOf("auto.offset.reset" to "earliest")).await()
    }

}

fun main(args: Array<String>) {
    runApplication<Setup>(*args).close()
}
