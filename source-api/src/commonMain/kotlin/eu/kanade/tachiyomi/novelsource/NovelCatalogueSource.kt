package eu.kanade.tachiyomi.novelsource

import eu.kanade.tachiyomi.novelsource.model.NovelsPage
import eu.kanade.tachiyomi.source.model.FilterList
import rx.Observable
import tachiyomi.core.common.util.lang.awaitSingle

interface NovelCatalogueSource : NovelSource {

    /**
     * An ISO 639-1 compliant language code (two letters in lower case).
     */
    override val lang: String

    /**
     * Whether the source has support for latest updates.
     */
    val supportsLatest: Boolean

    /**
     * Get a page with a list of novels.
     *
     * @param page the page number to retrieve.
     */
    @Suppress("DEPRECATION")
    suspend fun getPopularNovels(page: Int): NovelsPage {
        return fetchPopularNovels(page).awaitSingle()
    }

    /**
     * Get a page with a list of novels matching the query.
     *
     * @param page the page number to retrieve.
     * @param query the search query.
     * @param filters the list of filters to apply.
     */
    @Suppress("DEPRECATION")
    suspend fun getSearchNovels(page: Int, query: String, filters: FilterList): NovelsPage {
        return fetchSearchNovels(page, query, filters).awaitSingle()
    }

    /**
     * Get a page with a list of latest novel updates.
     *
     * @param page the page number to retrieve.
     */
    @Suppress("DEPRECATION")
    suspend fun getLatestUpdates(page: Int): NovelsPage {
        return fetchLatestUpdates(page).awaitSingle()
    }

    /**
     * Returns the list of filters for the source.
     */
    fun getFilterList(): FilterList

    @Deprecated(
        "Use the non-RxJava API instead",
        ReplaceWith("getPopularNovels"),
    )
    fun fetchPopularNovels(page: Int): Observable<NovelsPage> =
        throw IllegalStateException("Not used")

    @Deprecated(
        "Use the non-RxJava API instead",
        ReplaceWith("getSearchNovels"),
    )
    fun fetchSearchNovels(page: Int, query: String, filters: FilterList): Observable<NovelsPage> =
        throw IllegalStateException("Not used")

    @Deprecated(
        "Use the non-RxJava API instead",
        ReplaceWith("getLatestUpdates"),
    )
    fun fetchLatestUpdates(page: Int): Observable<NovelsPage> =
        throw IllegalStateException("Not used")
}
