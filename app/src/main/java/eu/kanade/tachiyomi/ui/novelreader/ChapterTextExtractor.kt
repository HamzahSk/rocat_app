package eu.kanade.tachiyomi.ui.novelreader

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

/**
 * Converts raw chapter HTML (as returned by [eu.kanade.tachiyomi.novelsource.NovelSource.getChapterText])
 * into a list of plain text paragraphs.
 *
 * Block elements split paragraphs, inline elements keep their text and `br`/`hr` also force a new
 * paragraph. Whitespace is collapsed so the reader can render long documents lazily.
 */
object ChapterTextExtractor {

    private val BLOCK_TAGS = setOf(
        "p", "div", "h1", "h2", "h3", "h4", "h5", "h6", "li",
        "blockquote", "section", "article", "ul", "ol", "table", "hr",
    )

    /**
     * Extracts paragraphs from a chapter HTML document.
     *
     * @param html the raw chapter HTML.
     * @return a list of plain text paragraphs.
     */
    fun extract(html: String): List<String> {
        val document = Jsoup.parse(html)
        val paragraphs = mutableListOf<String>()
        val current = StringBuilder()

        fun flush() {
            val text = current.toString()
                .replace(Regex("\\s+"), " ")
                .trim()
            if (text.isNotEmpty()) {
                paragraphs.add(text)
            }
            current.setLength(0)
        }

        fun walk(node: Node) {
            when (node) {
                is TextNode -> current.append(node.text())
                is Element -> when (node.tagName()) {
                    "br" -> flush()
                    in BLOCK_TAGS -> {
                        flush()
                        node.childNodes().forEach(::walk)
                        flush()
                    }
                    else -> {
                        node.childNodes().forEach(::walk)
                        current.append(' ')
                    }
                }
                else -> Unit
            }
        }

        document.childNodes().forEach(::walk)
        flush()

        return paragraphs
    }
}
