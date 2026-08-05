package eu.kanade.tachiyomi.novelsource

import eu.kanade.tachiyomi.novelsource.model.SNovel
import eu.kanade.tachiyomi.novelsource.model.SNovelChapter
import eu.kanade.tachiyomi.util.awaitSingle
import rx.Observable

/**
 * A basic interface for creating a novel (text based) source. It is the direct counterpart of
 * [eu.kanade.tachiyomi.source.MangaSource] and [eu.kanade.tachiyomi.animesource.AnimeSource].
 *
 * The three media types can be told apart by the interface tree a source implements:
 * - Text sources implement [NovelSource] (and usually [NovelCatalogueSource]).
 * - Image sources implement [eu.kanade.tachiyomi.source.MangaSource].
 * - Video sources implement [eu.kanade.tachiyomi.animesource.AnimeSource].
 */
interface NovelSource {

    /**
     * ID for the source. Must be unique.
     */
    val id: Long

    /**
     * Name of the source.
     */
    val name: String

    val lang: String
        get() = ""

    /**
     * Get the updated details for a novel.
     *
     * @param novel the novel to update.
     * @return the updated novel.
     */
    @Suppress("DEPRECATION")
    suspend fun getNovelDetails(novel: SNovel): SNovel {
        return fetchNovelDetails(novel).awaitSingle()
    }

    /**
     * Get all the available chapters for a novel.
     *
     * @param novel the novel to update.
     * @return the chapters for the novel.
     */
    @Suppress("DEPRECATION")
    suspend fun getChapterList(novel: SNovel): List<SNovelChapter> {
        return fetchChapterList(novel).awaitSingle()
    }

    /**
     * Get the plain text (HTML) content of a chapter.
     *
     * @param chapter the chapter.
     * @return the text content of the chapter.
     */
    @Suppress("DEPRECATION")
    suspend fun getChapterText(chapter: SNovelChapter): String {
        return fetchChapterText(chapter).awaitSingle()
    }

    @Deprecated(
        "Use the non-RxJava API instead",
        ReplaceWith("getNovelDetails"),
    )
    fun fetchNovelDetails(novel: SNovel): Observable<SNovel> =
        throw IllegalStateException("Not used")

    @Deprecated(
        "Use the non-RxJava API instead",
        ReplaceWith("getChapterList"),
    )
    fun fetchChapterList(novel: SNovel): Observable<List<SNovelChapter>> =
        throw IllegalStateException("Not used")

    @Deprecated(
        "Use the non-RxJava API instead",
        ReplaceWith("getChapterText"),
    )
    fun fetchChapterText(chapter: SNovelChapter): Observable<String> =
        throw IllegalStateException("Not used")
}
