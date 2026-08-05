package eu.kanade.tachiyomi.ui.novelreader

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import java.util.Base64

/**
 * A single block of rendered novel chapter content.
 */
sealed interface NovelChapterContent {
    data class Text(val text: String) : NovelChapterContent
    data class Image(val bytes: ByteArray) : NovelChapterContent
}

/**
 * Converts raw chapter HTML (as returned by [eu.kanade.tachiyomi.novelsource.NovelSource.getChapterText])
 * into a list of content blocks.
 *
 * Block elements split text paragraphs, inline elements keep their text and `br`/`hr` also force a
 * new paragraph. Whitespace is collapsed so the reader can render long documents lazily. Images
 * embedded as `data:` URIs (produced by the local EPUB source) are decoded into [NovelChapterContent.Image]
 * blocks so illustrations can be rendered by the reader.
 */
object ChapterTextExtractor {

    private val BLOCK_TAGS = setOf(
        "p", "div", "h1", "h2", "h3", "h4", "h5", "h6", "li",
        "blockquote", "section", "article", "ul", "ol", "table", "hr",
    )

    /**
     * Extracts content blocks from a chapter HTML document.
     *
     * @param html the raw chapter HTML.
     * @return a list of [NovelChapterContent] blocks.
     */
    fun extract(html: String): List<NovelChapterContent> {
        val document = Jsoup.parse(html)
        val blocks = mutableListOf<NovelChapterContent>()
        val current = StringBuilder()

        fun flush() {
            val text = current.toString()
                .replace(Regex("\\s+"), " ")
                .trim()
            if (text.isNotEmpty()) {
                blocks.add(NovelChapterContent.Text(text))
            }
            current.setLength(0)
        }

        fun walk(node: Node) {
            when (node) {
                is TextNode -> current.append(node.text())
                is Element -> when (node.tagName()) {
                    "br" -> flush()
                    "img" -> {
                        // Jsoup normalizes the SVG `<image>` element to `<img>`, keeping the
                        // `xlink:href` attribute, so it is probed here as well.
                        val image = decodeImage(node, "src", "data-src", "xlink:href")
                        if (image != null) {
                            flush()
                            blocks.add(NovelChapterContent.Image(image))
                        }
                    }
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

        return blocks
    }

    private fun decodeImage(element: Element, vararg attributes: String): ByteArray? {
        for (attribute in attributes) {
            val src = element.attr(attribute)
            if (src.startsWith("data:")) {
                val commaIndex = src.indexOf(',')
                if (commaIndex > 0) {
                    val encoded = src.substring(commaIndex + 1)
                    return try {
                        Base64.getDecoder().decode(encoded)
                    } catch (e: IllegalArgumentException) {
                        null
                    }
                }
            }
        }
        return null
    }
}
