package tachiyomi.source.local.entries.novel

import android.content.Context
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.novelsource.NovelCatalogueSource
import eu.kanade.tachiyomi.novelsource.NovelSource
import eu.kanade.tachiyomi.novelsource.model.NovelsPage
import eu.kanade.tachiyomi.novelsource.model.SNovel
import eu.kanade.tachiyomi.novelsource.model.SNovelChapter
import eu.kanade.tachiyomi.source.UnmeteredSource
import eu.kanade.tachiyomi.source.model.FilterList
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.storage.extension
import tachiyomi.core.common.storage.nameWithoutExtension
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.entries.novel.model.Novel
import tachiyomi.i18n.aniyomi.AYMR
import tachiyomi.source.local.epub.epubParser
import tachiyomi.source.local.filter.novel.NovelOrderBy
import tachiyomi.source.local.image.novel.LocalNovelCoverManager
import tachiyomi.source.local.io.novel.LocalNovelSourceFileSystem

/**
 * Text based local source that turns `.epub` files stored in the local novel directory into
 * [eu.kanade.tachiyomi.novelsource.model.SNovel] entries. Each EPUB maps to a single novel and
 * its spine pages map to the novel chapters.
 *
 * Mirrors [tachiyomi.source.local.entries.manga.LocalMangaSource] for the text media type.
 */
actual class LocalNovelSource(
    private val context: Context,
    private val fileSystem: LocalNovelSourceFileSystem,
    private val coverManager: LocalNovelCoverManager,
) : NovelCatalogueSource, UnmeteredSource {

    override val id: Long = ID

    override val name: String = context.stringResource(AYMR.strings.local_novel_source)

    override val lang: String = "other"

    override val supportsLatest: Boolean = false

    override fun toString(): String = name

    private val popularFilters: FilterList = FilterList(NovelOrderBy.Popular(context))

    private val latestFilters: FilterList = FilterList(NovelOrderBy.Latest(context))

    // Browse related

    override suspend fun getPopularNovels(page: Int): NovelsPage {
        return getSearchNovels(page, "", popularFilters)
    }

    override suspend fun getLatestUpdates(page: Int): NovelsPage {
        return getSearchNovels(page, "", latestFilters)
    }

    override suspend fun getSearchNovels(
        page: Int,
        query: String,
        filters: FilterList,
    ): NovelsPage = withIOContext {
        var files = fileSystem.getFilesInBaseDirectory()
            // Ignore the hidden cover cache directory and any hidden files
            .filter { it.isFile && !it.name.orEmpty().startsWith('.') }
            .filter { it.extension.equals("epub", ignoreCase = true) }
            .filter { query.isBlank() || it.name.orEmpty().contains(query, ignoreCase = true) }

        filters.forEach { filter ->
            when (filter) {
                is NovelOrderBy.Popular -> {
                    files = if (filter.state!!.ascending) {
                        files.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name.orEmpty() })
                    } else {
                        files.sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.name.orEmpty() })
                    }
                }
                is NovelOrderBy.Latest -> {
                    files = if (filter.state!!.ascending) {
                        files.sortedBy(UniFile::lastModified)
                    } else {
                        files.sortedByDescending(UniFile::lastModified)
                    }
                }
                else -> {
                    /* Do nothing */
                }
            }
        }

        val novels = files.map { file ->
            SNovel.create().apply {
                title = file.nameWithoutExtension.orEmpty()
                url = file.name.orEmpty()

                // Use a previously extracted cover if one exists
                coverManager.find(url)?.let {
                    thumbnail_url = it.uri.toString()
                }

                initialized = true
            }
        }

        NovelsPage(novels, false)
    }

    // Novel details related

    override suspend fun getNovelDetails(novel: SNovel): SNovel = withIOContext {
        val file = fileSystem.getNovelFile(novel.url)
            ?: throw Exception("${novel.url} is not a valid novel file")

        file.epubParser(context).use { parser ->
            val metadata = parser.metadata()

            metadata.title?.let { novel.title = it }
            metadata.author?.let { novel.author = it }
            metadata.artist?.let { novel.artist = it }
            metadata.description?.let { novel.description = it }
            metadata.genre?.let { novel.genre = it }
            novel.status = metadata.status
            novel.initialized = true

            // Extract and cache the cover so it shows up in the library
            if (novel.thumbnail_url.isNullOrBlank()) {
                coverManager.update(novel, file)
            }
        }

        novel
    }

    // Chapters

    override suspend fun getChapterList(novel: SNovel): List<SNovelChapter> = withIOContext {
        val file = fileSystem.getNovelFile(novel.url)
            ?: throw Exception("${novel.url} is not a valid novel file")

        file.epubParser(context).use { parser ->
            val metadata = parser.metadata()
            parser.chapters(novel.url).map { chapter ->
                SNovelChapter.create().apply {
                    url = chapter.url
                    name = chapter.name
                    chapter_number = chapter.number
                    date_upload = file.lastModified()
                    scanlator = metadata.publisher
                }
            }
        }
    }

    override suspend fun getChapterText(chapter: SNovelChapter): String = withIOContext {
        val (fileName, href) = chapter.url.split('/', limit = 2)
        val file = fileSystem.getNovelFile(fileName)
            ?: throw Exception("$fileName is not a valid novel file")

        file.epubParser(context).use { parser ->
            parser.chapterText(href)
        }
    }

    // Filters

    override fun getFilterList(): FilterList {
        return FilterList(NovelOrderBy.Popular(context))
    }

    companion object {
        const val ID = 1L

        const val HELP_URL = "https://aniyomi.org/help/guides/local-novel/"
    }
}

fun Novel.isLocalNovel(): Boolean = source == LocalNovelSource.ID

fun NovelSource.isLocalNovel(): Boolean = id == LocalNovelSource.ID
