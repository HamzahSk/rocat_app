package eu.kanade.tachiyomi.ui.novelreader

import eu.kanade.tachiyomi.source.novel.SampleNovelSource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ChapterTextExtractorTest {

    @Test
    fun `paragraphs are split on block elements`() {
        val html = """
            <div class="chapter-content">
              <h1>Chapter One</h1>
              <p>First paragraph.</p>
              <p>Second<br>line.</p>
              <hr>
              <p>Third paragraph.</p>
            </div>
        """.trimIndent()

        val paragraphs = ChapterTextExtractor.extract(html)

        assertEquals(
            listOf("Chapter One", "First paragraph.", "Second", "line.", "Third paragraph."),
            paragraphs,
        )
    }

    @Test
    fun `extracts fifty thousand words quickly`() {
        val html = SampleNovelSource.buildChapterText(SampleNovelSource.CHAPTER_WORD_COUNT)

        val start = System.nanoTime()
        val paragraphs = ChapterTextExtractor.extract(html)
        val elapsedMillis = (System.nanoTime() - start) / 1_000_000

        val wordCount = paragraphs.sumOf { it.split(Regex("\\s+")).size }
        assertTrue(paragraphs.size > 500, "expected many paragraphs, got ${paragraphs.size}")
        assertTrue(wordCount >= 50_000, "expected >= 50000 words, got $wordCount")
        assertTrue(elapsedMillis < 10_000, "extraction took too long: ${elapsedMillis}ms")
    }

    @Test
    fun `paragraph text is whitespace collapsed`() {
        val paragraphs = ChapterTextExtractor.extract("<p>  Many   spaces   here  </p>")
        assertEquals(listOf("Many spaces here"), paragraphs)
    }

    @Test
    fun `empty html yields no paragraphs`() {
        assertTrue(ChapterTextExtractor.extract("").isEmpty())
    }
}
