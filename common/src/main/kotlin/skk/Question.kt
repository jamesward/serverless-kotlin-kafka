package skk

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import java.io.IOException

// from https://github.com/ktorio/ktor/blob/master/ktor-utils/common/src/io/ktor/util/Text.kt#L10
fun String.escapeHTML(): String {
    val text = this@escapeHTML
    if (text.isEmpty()) return text

    return buildString(length) {
        for (element in text) {
            when (element) {
                '\'' -> append("&#x27;")
                '\"' -> append("&quot;")
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                else -> append(element)
            }
        }
    }
}

class QuestionBodyDeserializer : JsonDeserializer<String>() {
    @Throws(IOException::class, JsonProcessingException::class)
    override fun deserialize(parser: JsonParser, context: DeserializationContext): String {
        return parser.text.escapeHTML()
    }
}

class QuestionTagsDeserializer : JsonDeserializer<List<String>>() {
    @Throws(IOException::class, JsonProcessingException::class)
    @Suppress("UNCHECKED_CAST")
    override fun deserialize(parser: JsonParser, context: DeserializationContext): List<String> {
        // data is [foo|bar] so we need to manually split it
        val deserializer: JsonDeserializer<Any> = context.findRootValueDeserializer(context.constructType(List::class.java))
        val maybeList = deserializer.deserialize(parser, context) as? List<String>
        return maybeList?.let { it.firstOrNull()?.split('|') } ?: emptyList()
    }
}

// JSON-Schema will generated from POKO in SR by Serializer
data class Question(
    val url: String,
    val title: String,
    @JsonProperty("favorite_count") val favoriteCount: Int,
    @JsonProperty("view_count") val viewCount: Int,
    @JsonDeserialize(using = QuestionTagsDeserializer::class) val tags: List<String>,
    @JsonDeserialize(using = QuestionBodyDeserializer::class) val body: String
)
