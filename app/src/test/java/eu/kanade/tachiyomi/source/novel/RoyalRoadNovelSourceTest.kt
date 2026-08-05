package eu.kanade.tachiyomi.source.novel

import eu.kanade.tachiyomi.novelsource.model.SNovel
import eu.kanade.tachiyomi.novelsource.model.SNovelChapter
import org.jsoup.Jsoup
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RoyalRoadNovelSourceTest {

    private val source = ExposedRoyalRoadSource()

    @Test
    fun `novel details are parsed from the page`() {
        val novel = source.parseDetails(Jsoup.parse(NOVEL_DETAILS_HTML, "https://www.royalroad.com"))

        assertEquals("The Long Scroll", novel.title)
        assertEquals("Sample Author", novel.author)
        assertEquals("Fantasy, Sci-fi", novel.genre)
        assertEquals(SNovel.ONGOING, novel.status)
        assertEquals("https://www.royalroad.com/covers/full/11/11111111.jpg", novel.thumbnail_url)
        assertTrue(novel.initialized)
        assertTrue(novel.description.orEmpty().contains("First paragraph."))
        assertTrue(novel.description.orEmpty().contains("---"))
        assertTrue(novel.description.orEmpty().contains("Second paragraph."))
    }

    @Test
    fun `chapters are parsed from embedded window chapters json`() {
        val chapters = source.parseChapterList(Jsoup.parse(CHAPTER_LIST_HTML))

        assertEquals(1, chapters.size)
        val chapter = chapters.first()
        assertEquals("Chapter 1: The Start", chapter.name)
        assertEquals("fiction/55426/chapter/1234567", chapter.url)
        assertEquals(1f, chapter.chapter_number)
        assertEquals(1_700_000_000_000L, chapter.date_upload)
    }

    @Test
    fun `chapter text keeps notes and strips spoilers`() {
        val text = source.parseChapterText(Jsoup.parse(CHAPTER_TEXT_HTML))

        assertTrue(text.contains("Before the chapter starts."))
        assertTrue(text.contains("First para of content."))
        assertTrue(text.contains("Second para."))
        assertTrue(text.contains("A note after the chapter."))
        assertTrue(text.contains("<hr class=\"notes-separator\">"))
        assertFalse(text.contains("Hidden spoiler content"))
    }

    private class ExposedRoyalRoadSource : RoyalRoadNovelSource() {
        fun parseDetails(document: org.jsoup.nodes.Document): SNovel =
            super.novelDetailsParse(document)

        fun parseChapterList(document: org.jsoup.nodes.Document): List<SNovelChapter> =
            super.chapterListParse(document)

        fun parseChapterText(document: org.jsoup.nodes.Document): String =
            super.chapterTextParse(document)
    }

    companion object {
        private val NOVEL_DETAILS_HTML = """
            <html>
            <head>
              <style>
                .spoiler-hidden { display: none; }
              </style>
            </head>
            <body>
              <div class="fic-header">
                <div class="fic-header-left">
                  <img class="thumbnail" src="/covers/full/11/11111111.jpg">
                  <h1>The Long Scroll</h1>
                  <div class="description">
                    <p>First paragraph.</p>
                    <hr>
                    <p>Second paragraph.</p>
                  </div>
                </div>
                <div class="fic-header-right">
                  <a href="/profile/12345">Sample Author</a>
                  <span class="tags">
                    <a href="/genres/fantasy">Fantasy</a>
                    <a href="/genres/scifi">Sci-fi</a>
                  </span>
                  <div class="fic-stat-box">
                    <span class="label-sm">Fiction</span>
                    <span class="label-sm">ONGOING</span>
                  </div>
                </div>
              </div>
            </body>
            </html>
        """.trimIndent()

        private val CHAPTER_LIST_HTML = """
            <html>
            <body>
              <script>
                window.chapters = [{"id":1,"volumeId":0,"title":"Chapter 1: The Start","date":1700000000,"order":1,"url":"/fiction/55426/the-long-scroll/chapter/1234567/chapter-1-the-start"}];
              </script>
            </body>
            </html>
        """.trimIndent()

        private val CHAPTER_TEXT_HTML = """
            <html>
            <head>
              <style>
                .spoiler-hidden { display: none; }
              </style>
            </head>
            <body>
              <div class="author-note-portlet"><p>Before the chapter starts.</p></div>
              <div class="chapter-content">
                <h2>Chapter 1: The Start</h2>
                <p class="spoiler-hidden">Hidden spoiler content that must be removed.</p>
                <p>First para of content.</p>
                <p>Second para.</p>
              </div>
              <div class="author-note-portlet"><p>A note after the chapter.</p></div>
            </body>
            </html>
        """.trimIndent()
    }
}
