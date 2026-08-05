package eu.kanade.tachiyomi.novelsource.online

import eu.kanade.tachiyomi.novelsource.model.NovelsPage
import eu.kanade.tachiyomi.novelsource.model.SNovel
import eu.kanade.tachiyomi.novelsource.model.SNovelChapter
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * A simple implementation for novel sources from a website using Jsoup, an HTML parser.
 *
 * This mirrors the LNReader plugin contract: a source only declares CSS selectors and maps the
 * matched [Element]s into [SNovel] / [SNovelChapter] / chapter text, while the HTTP + HTML
 * plumbing is handled here.
 */
@Suppress("unused")
abstract class ParsedNovelHttpSource : HttpNovelSource() {

    /**
     * Parses the response from the site and returns a [NovelsPage] object.
     */
    override fun popularNovelsParse(response: Response): NovelsPage {
        val document = response.asJsoup()

        val novels = document.select(popularNovelsSelector()).map { element ->
            popularNovelFromElement(element)
        }

        val hasNextPage = popularNovelsNextPageSelector()?.let { selector ->
            document.select(selector).first()
        } != null

        return NovelsPage(novels, hasNextPage)
    }

    /**
     * Returns the Jsoup selector that returns a list of [Element] corresponding to each novel.
     */
    protected abstract fun popularNovelsSelector(): String

    /**
     * Returns a novel from the given [element]. Most sites only show the title and the url, it's
     * totally fine to fill only those two values.
     */
    protected abstract fun popularNovelFromElement(element: Element): SNovel

    /**
     * Returns the Jsoup selector that returns the <a> tag linking to the next page, or null if
     * there's no next page.
     */
    protected abstract fun popularNovelsNextPageSelector(): String?

    /**
     * Parses the response from the site and returns a [NovelsPage] object.
     */
    override fun searchNovelsParse(response: Response): NovelsPage {
        val document = response.asJsoup()

        val novels = document.select(searchNovelsSelector()).map { element ->
            searchNovelFromElement(element)
        }

        val hasNextPage = searchNovelsNextPageSelector()?.let { selector ->
            document.select(selector).first()
        } != null

        return NovelsPage(novels, hasNextPage)
    }

    /**
     * Returns the Jsoup selector that returns a list of [Element] corresponding to each novel.
     */
    protected abstract fun searchNovelsSelector(): String

    /**
     * Returns a novel from the given [element].
     */
    protected abstract fun searchNovelFromElement(element: Element): SNovel

    /**
     * Returns the Jsoup selector that returns the <a> tag linking to the next page, or null if
     * there's no next page.
     */
    protected abstract fun searchNovelsNextPageSelector(): String?

    /**
     * Parses the response from the site and returns a [NovelsPage] object.
     */
    override fun latestUpdatesParse(response: Response): NovelsPage {
        val document = response.asJsoup()

        val novels = document.select(latestUpdatesSelector()).map { element ->
            latestUpdateFromElement(element)
        }

        val hasNextPage = latestUpdatesNextPageSelector()?.let { selector ->
            document.select(selector).first()
        } != null

        return NovelsPage(novels, hasNextPage)
    }

    /**
     * Returns the Jsoup selector that returns a list of [Element] corresponding to each novel.
     */
    protected abstract fun latestUpdatesSelector(): String

    /**
     * Returns a novel from the given [element].
     */
    protected abstract fun latestUpdateFromElement(element: Element): SNovel

    /**
     * Returns the Jsoup selector that returns the <a> tag linking to the next page, or null if
     * there's no next page.
     */
    protected abstract fun latestUpdatesNextPageSelector(): String?

    /**
     * Parses the response from the site and returns the details of a novel.
     */
    override fun novelDetailsParse(response: Response): SNovel {
        return novelDetailsParse(response.asJsoup())
    }

    /**
     * Returns the details of the novel from the given [document].
     */
    protected abstract fun novelDetailsParse(document: Document): SNovel

    /**
     * Parses the response from the site and returns a list of chapters.
     */
    override fun chapterListParse(response: Response): List<SNovelChapter> {
        val document = response.asJsoup()
        return document.select(chapterListSelector()).map { chapterFromElement(it) }
    }

    /**
     * Returns the Jsoup selector that returns a list of [Element] corresponding to each chapter.
     */
    protected abstract fun chapterListSelector(): String

    /**
     * Returns a chapter from the given element.
     */
    protected abstract fun chapterFromElement(element: Element): SNovelChapter

    /**
     * Parses the response from the site and returns the text content of a chapter.
     */
    override fun chapterTextParse(response: Response): String {
        return chapterTextParse(response.asJsoup())
    }

    /**
     * Returns the text content of the chapter from the [document].
     */
    protected abstract fun chapterTextParse(document: Document): String
}
