package eu.kanade.tachiyomi.source.novel

import eu.kanade.tachiyomi.novelsource.NovelCatalogueSource
import eu.kanade.tachiyomi.novelsource.model.NovelsPage
import eu.kanade.tachiyomi.novelsource.model.SNovel
import eu.kanade.tachiyomi.novelsource.model.SNovelChapter
import eu.kanade.tachiyomi.source.model.FilterList
import kotlin.random.Random

/**
 * A fully self-contained [NovelCatalogueSource] that serves generated content instead of making
 * network requests. It exists so the text reader can be exercised end to end (and performance
 * tested) without depending on an external website.
 *
 * Chapters are long so scrolling behaviour with big documents can be validated: [CHAPTER_WORD_COUNT]
 * words per chapter by default, well above the 50.000 word performance target.
 */
class SampleNovelSource : NovelCatalogueSource {

    override val id = SAMPLE_SOURCE_ID

    override val name = "Sample Novel Source"

    override val lang = "en"

    override val supportsLatest = true

    private val novels: List<SNovel> = (1..3).map { index ->
        SNovel.create().apply {
            url = "sample/novel-$index"
            title = "The Long Scroll $index"
            author = "Sample Author"
            artist = null
            description = "A generated novel used to test the text reader. " +
                "Chapter text is produced locally so the reader can be validated without a network."
            genre = "Fantasy, Sci-fi"
            status = eu.kanade.tachiyomi.novelsource.model.SNovel.ONGOING
            thumbnail_url = null
            initialized = true
        }
    }

    override suspend fun getPopularNovels(page: Int): NovelsPage {
        return NovelsPage(novels, page < 1)
    }

    override suspend fun getSearchNovels(page: Int, query: String, filters: FilterList): NovelsPage {
        val filtered = if (query.isBlank()) {
            novels
        } else {
            novels.filter { it.title.contains(query, ignoreCase = true) }
        }
        return NovelsPage(filtered, false)
    }

    override suspend fun getLatestUpdates(page: Int): NovelsPage {
        return getPopularNovels(page)
    }

    override fun getFilterList() = FilterList()

    override suspend fun getNovelDetails(novel: SNovel): SNovel {
        return novels.firstOrNull { it.url == novel.url } ?: novel
    }

    override suspend fun getChapterList(novel: SNovel): List<SNovelChapter> {
        val base = novel.url.substringAfterLast('/')
        return (1..12).map { i ->
            SNovelChapter.create().apply {
                url = "$base/chapter-$i"
                name = when (i % 5) {
                    0 -> "Chapter $i (Extra)"
                    3 -> "Chapter $i.5"
                    else -> "Chapter $i"
                }
                chapter_number = -1f
                date_upload = System.currentTimeMillis() - (12 - i) * 86_400_000L
                scanlator = null
            }
        }
    }

    override suspend fun getChapterText(chapter: SNovelChapter): String {
        return buildChapterText(CHAPTER_WORD_COUNT)
    }

    companion object {
        /**
         * Fixed unique id so the source survives restarts (it's not derived from a website).
         */
        const val SAMPLE_SOURCE_ID = 91010101010101L

        const val CHAPTER_WORD_COUNT = 50_000

        private val WORDS = listOf(
            "the", "scroll", "lantern", "road", "night", "shadow", "light", "silence", "tale",
            "ancient", "city", "wind", "stone", "river", "forest", "star", "echo", "dream",
            "wanderer", "kingdom", "flame", "frost", "bridge", "gate", "mirror", "crown",
            "dusk", "dawn", "path", "voice", "memory", "tower", "ocean", "desert", "clock",
        )

        private val random = Random(42)

        /**
         * Builds a deterministic block of chapter text with roughly [wordCount] words.
         */
        fun buildChapterText(wordCount: Int = CHAPTER_WORD_COUNT): String {
            val sb = StringBuilder(wordCount * 7)
            sb.append("<div class=\"chapter-content\">\n")
            sb.append("<h1>The Long Road</h1>\n")
            var remaining = wordCount
            while (remaining > 0) {
                val words = minOf(remaining, random.nextInt(40, 80))
                sb.append("<p>")
                repeat(words) {
                    sb.append(WORDS[random.nextInt(WORDS.size)])
                    if (it < words - 1) {
                        sb.append(if (random.nextInt(5) == 0) ". " else " ")
                    }
                }
                sb.append(".</p>\n")
                remaining -= words
            }
            sb.append("</div>\n")
            return sb.toString()
        }
    }
}
