package eu.kanade.tachiyomi.source.novel

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.novelsource.NovelStatus
import eu.kanade.tachiyomi.novelsource.model.SNovel
import eu.kanade.tachiyomi.novelsource.model.SNovelChapter
import eu.kanade.tachiyomi.novelsource.online.ParsedNovelHttpSource
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.util.asJsoup
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

/**
 * Kotlin port of the LNReader "Royal Road" plugin (`plugins/english/royalroad.ts`).
 *
 * The selectors and parsing behaviour follow the original TypeScript implementation closely:
 * - catalogue/search/latest come from `fictions/search` pages parsed via `.fiction-list-item`.
 * - novel details + chapters come from the `window.chapters = [...]` JSON embedded in the page.
 * - chapter content comes from `.chapter-content` with `.author-note-portlet` before/after notes
 *   and the site's `display:none` spoiler class stripped.
 */
open class RoyalRoadNovelSource : ParsedNovelHttpSource() {

    override val name = "Royal Road"

    override val lang = "en"

    override val supportsLatest = true

    override val baseUrl = "https://www.royalroad.com"

    override val versionId = 1

    // --------------------------------------------------------------------------------------------
    // Catalogue / search / latest
    // --------------------------------------------------------------------------------------------

    override fun popularNovelsRequest(page: Int): Request {
        return GET(buildSearchUrl(page, latest = false, filters = emptyList()))
    }

    override fun popularNovelsSelector() = ".fiction-list-item"

    override fun popularNovelFromElement(element: Element): SNovel {
        return SNovel.create().apply {
            val link = element.selectFirst("a[href]")
            val img = element.selectFirst("img")
            url = link?.attr("abs:href")
                ?.toHttpUrlOrNull()
                ?.pathSegments
                ?.let { segments -> if (segments.size >= 2) segments.subList(0, 2).joinToString("/") else null }
                .orEmpty()
            title = img?.attr("alt")?.ifBlank { null } ?: link?.text() ?: ""
            thumbnail_url = img?.let { it.attr("abs:src").ifBlank { null } }
        }
    }

    override fun popularNovelsNextPageSelector(): String? {
        return ".pagination a:has(> i.fa-chevron-right), .pagination a:contains(Next)"
    }

    override fun searchNovelsRequest(page: Int, query: String, filters: FilterList): Request {
        val params = mutableListOf("page" to page.toString(), "title" to query, "globalFilters" to "true")
        filters.forEach { params += it.toRequestParams() }
        return GET("$baseUrl/fictions/search?${encode(params)}")
    }

    override fun searchNovelsSelector() = popularNovelsSelector()

    override fun searchNovelFromElement(element: Element) = popularNovelFromElement(element)

    override fun searchNovelsNextPageSelector() = popularNovelsNextPageSelector()

    override fun latestUpdatesRequest(page: Int): Request {
        return GET(buildSearchUrl(page, latest = true, filters = emptyList()))
    }

    override fun latestUpdatesSelector() = popularNovelsSelector()

    override fun latestUpdateFromElement(element: Element) = popularNovelFromElement(element)

    override fun latestUpdatesNextPageSelector() = popularNovelsNextPageSelector()

    override fun getFilterList(): FilterList {
        return FilterList(
            Filter.Header("Search"),
            TextFilter("Keyword (title or description)", "keyword"),
            TextFilter("Author", "author"),
            Filter.Separator(),
            SelectFilter(
                "Order by",
                arrayOf(
                    "Relevance", "Popularity", "Average Rating", "Last Update",
                    "Release Date", "Followers", "Number of Pages", "Views", "Title", "Author",
                ),
                "orderBy",
                arrayOf(
                    "relevance", "popularity", "rating", "last_update",
                    "release_date", "followers", "length", "views", "title", "author",
                ),
            ),
            SelectFilter(
                "Direction",
                arrayOf("Ascending", "Descending"),
                "dir",
                arrayOf("asc", "desc"),
                1,
            ),
            SelectFilter(
                "Status",
                arrayOf("All", "Completed", "Dropped", "Ongoing", "Hiatus", "Stub"),
                "status",
                arrayOf("ALL", "COMPLETED", "DROPPED", "ONGOING", "HIATUS", "STUB"),
            ),
            SelectFilter(
                "Type",
                arrayOf("All", "Fan Fiction", "Original"),
                "type",
                arrayOf("ALL", "fanfiction", "original"),
            ),
            Filter.Separator(),
            TextFilter("Min Pages", "minPages"),
            TextFilter("Max Pages", "maxPages"),
            TextFilter("Min Rating (0.0 - 5.0)", "minRating"),
            TextFilter("Max Rating (0.0 - 5.0)", "maxRating"),
        )
    }

    private fun buildSearchUrl(page: Int, latest: Boolean, filters: List<Filter<*>>): String {
        val params = mutableListOf("page" to page.toString())
        if (latest) {
            params += "orderBy" to "last_update"
        }
        filters.forEach { params += it.toRequestParams() }
        return "$baseUrl/fictions/search?${encode(params)}"
    }

    private fun encode(params: List<Pair<String, String>>): String {
        val url = baseUrl.toHttpUrl().newBuilder()
        params.forEach { (key, value) -> if (value.isNotEmpty()) url.addQueryParameter(key, value) }
        return url.build().query ?: ""
    }

    private fun Filter<*>.toRequestParams(): List<Pair<String, String>> {
        return when (this) {
            is TextFilter -> listOf(this.paramName to this.state)
            is SelectFilter -> listOf(this.paramName to this.values[this.state])
            else -> emptyList()
        }
    }

    // --------------------------------------------------------------------------------------------
    // Novel details
    // --------------------------------------------------------------------------------------------

    override fun novelDetailsParse(document: Document): SNovel {
        val statusSpan = document.select("span[class*=label-sm]")
        val statusValue = NovelStatus.fromString(
            statusSpan.getOrNull(1)?.text() ?: statusSpan.firstOrNull()?.text(),
        )

        return SNovel.create().apply {
            url = document.location().substringAfter(baseUrl).trimStart('/')
            title = document.selectFirst("h1")?.text()?.trim().orEmpty()
            author = document.selectFirst("a[href^=/profile/]")?.text()?.trim()
            description = document.selectFirst("div.description")?.descriptionText()
            genre = document.selectFirst("span[class*=tags]")
                ?.select("a[href]")
                ?.mapNotNull { it.text().trim().ifEmpty { null } }
                ?.distinct()
                ?.joinToString(", ")
            thumbnail_url = document.selectFirst("img[class*=thumbnail]")
                ?.attr("abs:src")
                ?.takeIf { it.isNotEmpty() }
            status = statusValue
            initialized = true
        }
    }

    /**
     * Extracts the description text of a novel, converting structural tags into newlines the same
     * way the LNReader plugin does (`br` and block ends -> blank line, `hr` -> separator).
     */
    private fun Element.descriptionText(): String {
        val sb = StringBuilder()
        fun walk(node: Node) {
            when (node) {
                is TextNode -> sb.append(node.text())
                is Element -> when (node.tagName()) {
                    "br" -> sb.append("\n\n")
                    "hr" -> sb.append("\n\n---\n\n")
                    else -> {
                        if (node.isBlock) sb.append("\n\n")
                        node.childNodes().forEach(::walk)
                        if (node.isBlock) sb.append("\n\n")
                    }
                }
                else -> Unit
            }
        }
        childNodes().forEach(::walk)
        return sb.toString()
            .replace("&nbsp;", " ")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }

    // --------------------------------------------------------------------------------------------
    // Chapters
    // --------------------------------------------------------------------------------------------

    override fun chapterListSelector() = "script"

    override fun chapterListParse(response: okhttp3.Response): List<SNovelChapter> {
        return chapterListParse(response.asJsoup())
    }

    internal fun chapterListParse(document: Document): List<SNovelChapter> {
        val json = document.select("script").asSequence()
            .mapNotNull { script ->
                Regex("window\\.chapters\\s*=\\s*(\\[.*?\\]);", RegexOption.DOT_MATCHES_ALL)
                    .find(script.html())?.groupValues?.get(1)
            }
            .firstOrNull() ?: return emptyList()

        return try {
            Json.Default.parseToJsonElement(json).jsonArray.mapNotNull { element ->
                val chapter = element.jsonObject
                val url = chapter["url"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val parts = url.split("/")
                if (parts.size < 6) return@mapNotNull null
                SNovelChapter.create().apply {
                    name = chapter["title"]?.jsonPrimitive?.contentOrNull?.ifEmpty { url } ?: url
                    this.url = "${parts[1]}/${parts[2]}/${parts[4]}/${parts[5]}"
                    chapter_number = (chapter["order"]?.jsonPrimitive?.doubleOrNull ?: -1.0).toFloat()
                    val date = chapter["date"]?.jsonPrimitive?.longOrNull ?: 0L
                    date_upload = if (date > 0L && date < 1_000_000_000_000L) date * 1000 else date
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun chapterFromElement(element: Element): SNovelChapter {
        throw UnsupportedOperationException("Chapters are parsed from the embedded JSON")
    }

    override fun chapterTextParse(document: Document): String {
        // The site hides spoilers / some notes behind a class that has `display: none` in CSS.
        val hiddenClass = CHAPTER_HIDDEN_CLASS_REGEX.find(document.outerHtml())
            ?.groupValues?.get(1)?.trim()
        if (!hiddenClass.isNullOrBlank()) {
            document.select(".$hiddenClass").forEach { it.remove() }
        }

        val allElements = document.allElements
        val content = document.selectFirst(".chapter-content")
        val contentPos = content?.let(allElements::indexOf) ?: -1

        val beforeNotes = mutableListOf<String>()
        val afterNotes = mutableListOf<String>()
        document.select(".author-note-portlet").forEach { note ->
            if (contentPos >= 0 && allElements.indexOf(note) < contentPos) {
                beforeNotes += note.outerHtml()
            } else {
                afterNotes += note.outerHtml()
            }
        }

        val contentHtml = content?.outerHtml()?.trim().orEmpty()

        return listOf(
            beforeNotes.joinToString("").takeIf { it.isNotEmpty() },
            contentHtml.takeIf { it.isNotEmpty() },
            afterNotes.joinToString("").takeIf { it.isNotEmpty() },
        )
            .filterNotNull()
            .joinToString("\n<hr class=\"notes-separator\">\n")
    }

    // --------------------------------------------------------------------------------------------
    // Filters
    // --------------------------------------------------------------------------------------------

    private class TextFilter(
        name: String,
        val paramName: String,
        state: String = "",
    ) : Filter.Text(name, state)

    private class SelectFilter(
        name: String,
        @Suppress("UNUSED_PARAMETER") labels: Array<String>,
        val paramName: String,
        values: Array<String>,
        state: Int = 0,
    ) : Filter.Select<String>(name, values, state)

    private fun String.toHttpUrlOrNull() = try {
        toHttpUrl()
    } catch (e: Exception) {
        null
    }

    companion object {
        private val CHAPTER_HIDDEN_CLASS_REGEX =
            Regex("<style>\\s+\\s*\\.([^,{]+?)\\s*\\{[^{}]*?display:\\s*none;", RegexOption.DOT_MATCHES_ALL)
    }
}
