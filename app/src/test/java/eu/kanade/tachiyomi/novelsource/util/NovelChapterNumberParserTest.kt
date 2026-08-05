package eu.kanade.tachiyomi.novelsource.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NovelChapterNumberParserTest {

    @Test
    fun `simple chapter number`() {
        assertEquals(1.0, NovelChapterNumberParser.parse("The Novel", "The Novel Chapter 1"))
    }

    @Test
    fun `decimal chapter number`() {
        assertEquals(2.5, NovelChapterNumberParser.parse("The Novel", "Chapter 2.5"), 1e-9)
    }

    @Test
    fun `volume prefix is stripped`() {
        assertEquals(3.0, NovelChapterNumberParser.parse("The Novel", "Volume 1 Chapter 3"))
        assertEquals(4.0, NovelChapterNumberParser.parse("The Novel", "The Novel, Volume 1, Chapter 4"))
    }

    @Test
    fun `extra omake and special suffixes`() {
        assertEquals(1.99, NovelChapterNumberParser.parse("The Novel", "Chapter 1 extra"), 1e-9)
        assertEquals(2.98, NovelChapterNumberParser.parse("The Novel", "Chapter 2 omake"), 1e-9)
        assertEquals(3.97, NovelChapterNumberParser.parse("The Novel", "Chapter 3 special"), 1e-9)
    }

    @Test
    fun `dotted alpha postfix`() {
        assertEquals(5.2, NovelChapterNumberParser.parse("The Novel", "Chapter 5.b"), 1e-9)
        assertEquals(6.1, NovelChapterNumberParser.parse("The Novel", "Chapter 6.a"), 1e-9)
    }

    @Test
    fun `falls back to any number`() {
        assertEquals(9.0, NovelChapterNumberParser.parse("The Novel", "Story arc 9"))
    }

    @Test
    fun `unparseable title returns fallback`() {
        assertEquals(-1.0, NovelChapterNumberParser.parse("The Novel", "Some random title"))
    }

    @Test
    fun `explicit chapter number wins`() {
        assertEquals(12.0, NovelChapterNumberParser.parse("The Novel", "Anything", 12.0))
    }

    @Test
    fun `case insensitive title removal`() {
        assertEquals(7.0, NovelChapterNumberParser.parse("THE NOVEL", "the novel chapter 7"))
    }
}
