package eu.kanade.tachiyomi.ui.novelreader

import eu.kanade.tachiyomi.source.novel.SampleNovelSource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ChapterTextExtractorTest {

    private fun List<NovelChapterContent>.texts(): List<String> {
        return mapNotNull { (it as? NovelChapterContent.Text)?.text }
    }

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

        val blocks = ChapterTextExtractor.extract(html)

        assertEquals(
            listOf("Chapter One", "First paragraph.", "Second", "line.", "Third paragraph."),
            blocks.texts(),
        )
        assertTrue(blocks.all { it is NovelChapterContent.Text })
    }

    @Test
    fun `inline data images become image blocks`() {
        val png = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        val dataUri = "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(png)

        val blocks = ChapterTextExtractor.extract(
            "<p>Before.</p><p><img src=\"$dataUri\" /></p><p>After.</p>",
        )

        assertEquals(3, blocks.size)
        assertEquals("Before.", (blocks[0] as NovelChapterContent.Text).text)
        assertEquals("After.", (blocks[2] as NovelChapterContent.Text).text)
        val image = blocks[1] as NovelChapterContent.Image
        assertEquals(png.toList(), image.bytes.toList())
    }

    @Test
    fun `svg image xlink href data uri becomes an image block`() {
        val jpg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte())
        val dataUri = "data:image/jpeg;base64," + java.util.Base64.getEncoder().encodeToString(jpg)

        val blocks = ChapterTextExtractor.extract(
            "<p><image xlink:href=\"$dataUri\" /></p>",
        )

        assertEquals(1, blocks.size)
        assertEquals(jpg.toList(), (blocks[0] as NovelChapterContent.Image).bytes.toList())
    }

    @Test
    fun `external images are dropped`() {
        val blocks = ChapterTextExtractor.extract(
            "<p>Text <img src=\"https://example.com/img.png\" /> more text.</p>",
        )

        assertEquals(listOf("Text more text."), blocks.texts())
    }

    @Test
    fun `extracts fifty thousand words quickly`() {
        val html = SampleNovelSource.buildChapterText(SampleNovelSource.CHAPTER_WORD_COUNT)

        val start = System.nanoTime()
        val blocks = ChapterTextExtractor.extract(html)
        val elapsedMillis = (System.nanoTime() - start) / 1_000_000

        val wordCount = blocks.texts().sumOf { it.split(Regex("\\s+")).size }
        assertTrue(blocks.size > 500, "expected many blocks, got ${blocks.size}")
        assertTrue(wordCount >= 50_000, "expected >= 50000 words, got $wordCount")
        assertTrue(elapsedMillis < 10_000, "extraction took too long: ${elapsedMillis}ms")
    }

    @Test
    fun `paragraph text is whitespace collapsed`() {
        val blocks = ChapterTextExtractor.extract("<p>  Many   spaces   here  </p>")
        assertEquals(listOf("Many spaces here"), blocks.texts())
    }

    @Test
    fun `empty html yields no paragraphs`() {
        assertTrue(ChapterTextExtractor.extract("").isEmpty())
    }
}
