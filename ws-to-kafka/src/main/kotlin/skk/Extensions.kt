package skk

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
