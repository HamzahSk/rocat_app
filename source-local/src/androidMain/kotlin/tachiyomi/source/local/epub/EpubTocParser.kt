package tachiyomi.source.local.epub

import org.jsoup.nodes.Document

/**
 * Pure helpers to extract chapter titles from an EPUB navigation document.
 *
 * EPUB 3 uses a navigation document (`nav` elements), EPUB 2 uses an NCX document
 * (`navPoint` elements). Both map a page href (relative to the package) to a title.
 */
object EpubTocParser {

    /**
     * Parses the table of contents from an EPUB 3 navigation document.
     *
     * @return a map of href (fragment stripped) to display title.
     */
    fun parseTitlesFromNav(navDoc: Document): Map<String, String> {
        val titles = LinkedHashMap<String, String>()
        navDoc.select("nav").forEach { nav ->
            val type = nav.attr("epub:type")
            val role = nav.attr("role")
            if (type.contains("toc", ignoreCase = true) || role == "doc-toc" || nav.id() == "toc") {
                nav.select("a[href]").forEach { link ->
                    val href = link.attr("href").substringBefore('#').trim()
                    val text = link.text().trim()
                    if (href.isNotEmpty() && text.isNotEmpty()) {
                        titles[href] = text
                    }
                }
            }
        }
        return titles
    }

    /**
     * Parses the table of contents from an EPUB 2 NCX document.
     *
     * @return a map of href (fragment stripped) to display title.
     */
    fun parseTitlesFromNcx(ncxDoc: Document): Map<String, String> {
        val titles = LinkedHashMap<String, String>()
        ncxDoc.select("navPoint").forEach { navPoint ->
            val text = navPoint.select("navLabel > text").firstOrNull()?.text()?.trim()
            val src = navPoint.select("content").firstOrNull()?.attr("src")?.substringBefore('#')?.trim()
            if (!text.isNullOrEmpty() && !src.isNullOrEmpty()) {
                titles[src] = text
            }
        }
        return titles
    }
}
