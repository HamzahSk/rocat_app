package eu.kanade.tachiyomi.novelsource.online

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.novelsource.NovelCatalogueSource
import eu.kanade.tachiyomi.novelsource.model.NovelsPage
import eu.kanade.tachiyomi.novelsource.model.SNovel
import eu.kanade.tachiyomi.novelsource.model.SNovelChapter
import eu.kanade.tachiyomi.source.model.FilterList
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import rx.Observable
import uy.kohesive.injekt.injectLazy
import java.net.URI
import java.net.URISyntaxException
import java.security.MessageDigest

/**
 * A simple implementation for novel sources from a website.
 *
 * @see <a href="https://lnreader.github.io/lnreader-plugins/">LNReader plugins</a>
 */
@Suppress("unused")
abstract class HttpNovelSource : NovelCatalogueSource {

    /**
     * Network service.
     */
    protected val network: NetworkHelper by injectLazy()

    /**
     * Base url of the website without the trailing slash, like: http://mysite.com
     */
    abstract val baseUrl: String

    /**
     * Version id used to generate the source id. If the site completely changes and urls are
     * incompatible, you may increase this value and it'll be considered as a new source.
     */
    open val versionId = 1

    /**
     * ID of the source. By default it uses a generated id using the first 16 characters (64 bits)
     * of the MD5 of the string `"${name.lowercase()}/$lang/$versionId"`.
     *
     * The generated ID sets the sign bit to `0`.
     */
    override val id by lazy { generateId(name, lang, versionId) }

    /**
     * Headers used for requests.
     */
    val headers: Headers by lazy { headersBuilder().build() }

    /**
     * Default network client for doing requests.
     */
    open val client: OkHttpClient
        get() = network.client

    /**
     * Generates a unique ID for the source based on the provided [name], [lang] and [versionId].
     */
    protected fun generateId(name: String, lang: String, versionId: Int): Long {
        val key = "${name.lowercase()}/$lang/$versionId"
        val bytes = MessageDigest.getInstance("MD5").digest(key.toByteArray())
        return (0..7).map { bytes[it].toLong() and 0xff shl 8 * (7 - it) }.reduce(Long::or) and Long.MAX_VALUE
    }

    /**
     * Headers builder for requests. Implementations can override this method for custom headers.
     */
    protected open fun headersBuilder() = Headers.Builder().apply {
        add("User-Agent", network.defaultUserAgentProvider())
    }

    /**
     * Visible name of the source.
     */
    override fun toString() = "$name (${lang.uppercase()})"

    /**
     * Returns an observable containing a page with a list of novels.
     *
     * @param page the page number to retrieve.
     */
    @Deprecated("Use the non-RxJava API instead", replaceWith = ReplaceWith("getPopularNovels"))
    override fun fetchPopularNovels(page: Int): Observable<NovelsPage> {
        return client.newCall(popularNovelsRequest(page))
            .asObservableSuccess()
            .map { response -> popularNovelsParse(response) }
    }

    /**
     * Returns the request for the popular novels given the page.
     */
    protected abstract fun popularNovelsRequest(page: Int): Request

    /**
     * Parses the response from the site and returns a [NovelsPage] object.
     */
    protected abstract fun popularNovelsParse(response: Response): NovelsPage

    /**
     * Returns an observable containing a page with a list of novels.
     *
     * @param page the page number to retrieve.
     * @param query the search query.
     * @param filters the list of filters to apply.
     */
    @Deprecated("Use the non-RxJava API instead", replaceWith = ReplaceWith("getSearchNovels"))
    override fun fetchSearchNovels(
        page: Int,
        query: String,
        filters: FilterList,
    ): Observable<NovelsPage> {
        return Observable.defer {
            try {
                client.newCall(searchNovelsRequest(page, query, filters)).asObservableSuccess()
            } catch (e: NoClassDefFoundError) {
                throw RuntimeException(e)
            }
        }
            .map { response -> searchNovelsParse(response) }
    }

    /**
     * Returns the request for the search novels given the page.
     */
    protected abstract fun searchNovelsRequest(page: Int, query: String, filters: FilterList): Request

    /**
     * Parses the response from the site and returns a [NovelsPage] object.
     */
    protected abstract fun searchNovelsParse(response: Response): NovelsPage

    /**
     * Returns an observable containing a page with a list of latest novel updates.
     *
     * @param page the page number to retrieve.
     */
    @Deprecated("Use the non-RxJava API instead", replaceWith = ReplaceWith("getLatestUpdates"))
    override fun fetchLatestUpdates(page: Int): Observable<NovelsPage> {
        return client.newCall(latestUpdatesRequest(page))
            .asObservableSuccess()
            .map { response -> latestUpdatesParse(response) }
    }

    /**
     * Returns the request for latest novels given the page.
     */
    protected abstract fun latestUpdatesRequest(page: Int): Request

    /**
     * Parses the response from the site and returns a [NovelsPage] object.
     */
    protected abstract fun latestUpdatesParse(response: Response): NovelsPage

    /**
     * Get the updated details for a novel.
     *
     * @param novel the novel to be updated.
     * @return the updated novel.
     */
    override suspend fun getNovelDetails(novel: SNovel): SNovel {
        return client.newCall(novelDetailsRequest(novel)).awaitSuccess()
            .let { response -> novelDetailsParse(response).apply { initialized = true } }
    }

    /**
     * Returns the request for the details of a novel.
     */
    open fun novelDetailsRequest(novel: SNovel): Request {
        return GET(baseUrl + novel.url, headers)
    }

    /**
     * Parses the response from the site and returns the details of a novel.
     */
    protected abstract fun novelDetailsParse(response: Response): SNovel

    /**
     * Get all the available chapters for a novel.
     *
     * @param novel the novel to update.
     * @return the chapters for the novel.
     */
    override suspend fun getChapterList(novel: SNovel): List<SNovelChapter> {
        return client.newCall(chapterListRequest(novel)).awaitSuccess()
            .let { response -> chapterListParse(response) }
    }

    /**
     * Returns the request for updating the chapter list.
     */
    protected open fun chapterListRequest(novel: SNovel): Request {
        return GET(baseUrl + novel.url, headers)
    }

    /**
     * Parses the response from the site and returns a list of chapters.
     */
    protected abstract fun chapterListParse(response: Response): List<SNovelChapter>

    /**
     * Get the plain text content of a chapter.
     *
     * @param chapter the chapter.
     * @return the text content of the chapter.
     */
    override suspend fun getChapterText(chapter: SNovelChapter): String {
        return client.newCall(chapterTextRequest(chapter)).awaitSuccess()
            .let { response -> chapterTextParse(response) }
    }

    /**
     * Returns the request for getting the chapter text.
     */
    protected open fun chapterTextRequest(chapter: SNovelChapter): Request {
        return GET(baseUrl + chapter.url, headers)
    }

    /**
     * Parses the response from the site and returns the text content of a chapter.
     */
    protected abstract fun chapterTextParse(response: Response): String

    /**
     * Assigns the url of the chapter without the scheme and domain.
     */
    fun SNovelChapter.setUrlWithoutDomain(url: String) {
        this.url = getUrlWithoutDomain(url)
    }

    /**
     * Assigns the url of the novel without the scheme and domain.
     */
    fun SNovel.setUrlWithoutDomain(url: String) {
        this.url = getUrlWithoutDomain(url)
    }

    /**
     * Returns the url of the given string without the scheme and domain.
     */
    private fun getUrlWithoutDomain(orig: String): String {
        return try {
            val uri = URI(orig.replace(" ", "%20"))
            var out = uri.path
            if (uri.query != null) {
                out += "?" + uri.query
            }
            if (uri.fragment != null) {
                out += "#" + uri.fragment
            }
            out
        } catch (e: URISyntaxException) {
            orig
        }
    }

    /**
     * Called before inserting a new chapter into database. Use it if you need to override chapter
     * fields, like the title or the chapter number. Do not change anything to [novel].
     *
     * @param chapter the chapter to be added.
     * @param novel the novel of the chapter.
     */
    open fun prepareNewNovelChapter(chapter: SNovelChapter, novel: SNovel) {}

    /**
     * Returns the list of filters for the source.
     */
    override fun getFilterList() = FilterList()
}
