package skk

import kotlinx.html.*
import kotlinx.html.dom.createHTMLDocument
import org.w3c.dom.Document


object Html {

    class TEMPLATE(consumer: TagConsumer<*>) :
        HTMLTag("template", consumer, emptyMap(),
            inlineTag = true,
            emptyTag = false), HtmlInlineTag

    fun FlowContent.template(block: TEMPLATE.() -> Unit = {}) {
        TEMPLATE(consumer).visit(block)
    }

    fun TEMPLATE.li(classes : String? = null, block : LI.() -> Unit = {}) {
        LI(attributesMapOf("class", classes), consumer).visit(block)
    }

    fun page(js: String, content: FlowContent.() -> Unit = {}): HTML.() -> Unit = {
        head {
            link("/webjars/bootstrap/4.5.3/css/bootstrap.min.css", LinkRel.stylesheet)
            link("/assets/index.css", LinkRel.stylesheet)
            script(ScriptType.textJavaScript) {
                src = "/assets/$js"
            }
        }
        body {
            nav("navbar fixed-top navbar-light bg-light") {
                a("/", classes = "navbar-brand") {
                    +"Serverless Kotlin Kafka"
                }
            }

            div("container-fluid") {
                content()
            }
        }
    }

    val indexHTML = page("index.js") {
        template {
            id = "total-template"
            +"Total Favorites: {{total}}"
        }

        div {
            id = "total"
        }

        ul {
            id = "recent-questions"

            template {
                id = "recent-questions-template"

                li {
                    id = "lang-{{lang}}"

                    a("{{lang}}") {
                        +"{{lang}} = {{num}}"
                    }
                }
            }
        }
    }

    val index: Document = createHTMLDocument().html(block = indexHTML)

    fun langHTML(name: String) = page("lang.js") {
        +"Questions For `$name`"

        ul {
            id = "questions"

            template {
                id = "question-template"

                li {
                    a("{{url}}") {
                        +"{{title}}"
                    }
                    +" (favorites: {{favorite_count}}, views: {{view_count}})"
                }
            }
        }
    }

    fun lang(name: String): Document = createHTMLDocument().html(block = langHTML(name))

}
