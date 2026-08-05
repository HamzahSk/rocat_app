package eu.kanade.tachiyomi.source.local

import org.jsoup.Jsoup
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tachiyomi.source.local.epub.EpubHtmlCleaner
import tachiyomi.source.local.epub.EpubMetadataParser
import tachiyomi.source.local.epub.EpubTocParser

class EpubParserTest {

    // --- Metadata ---------------------------------------------------------

    private fun epub2Package(): String = """
        <?xml version="1.0" encoding="utf-8"?>
        <package xmlns="http://www.idpf.org/2007/opf" version="2.0" unique-identifier="id">
          <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
            <dc:title>The Long Scroll</dc:title>
            <dc:creator>Sample Author</dc:creator>
            <dc:contributor>Sample Artist</dc:contributor>
            <dc:description>A test novel.</dc:description>
            <dc:subject>Fantasy</dc:subject>
            <dc:subject>Adventure</dc:subject>
            <dc:publisher>ACME Press</dc:publisher>
            <dc:date>2024-01-15T10:00:00Z</dc:date>
          </metadata>
          <manifest>
            <item id="cover" href="images/cover.jpg" media-type="image/jpeg" properties="cover-image"/>
            <item id="ncx" href="toc.ncx" media-type="application/x-dtbncx+xml"/>
            <item id="ch1" href="text/chapter1.xhtml" media-type="application/xhtml+xml"/>
            <item id="ch2" href="text/chapter2.xhtml" media-type="application/xhtml+xml"/>
          </manifest>
          <spine toc="ncx">
            <itemref idref="ch1"/>
            <itemref idref="ch2"/>
          </spine>
        </package>
    """.trimIndent()

    @Test
    fun `epub2 metadata is extracted`() {
        val metadata = EpubMetadataParser.parse(Jsoup.parse(epub2Package()))

        assertEquals("The Long Scroll", metadata.title)
        assertEquals("Sample Author", metadata.author)
        assertEquals("Sample Artist", metadata.artist)
        assertEquals("A test novel.", metadata.description)
        assertEquals("Fantasy, Adventure", metadata.genre)
        assertEquals("ACME Press", metadata.publisher)
        assertTrue(metadata.modifiedDate > 0, "expected a parsed date")
    }

    @Test
    fun `metadata falls back to dcterms modified`() {
        val packageDoc = """
            <package xmlns="http://www.idpf.org/2007/opf" version="3.0">
              <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
                <dc:title>Epub 3</dc:title>
                <meta property="dcterms:modified">2023-06-01T00:00:00Z</meta>
              </metadata>
              <manifest>
                <item id="nav" href="nav.xhtml" media-type="application/xhtml+xml" properties="nav"/>
                <item id="ch1" href="ch1.xhtml" media-type="application/xhtml+xml"/>
              </manifest>
              <spine><itemref idref="ch1"/></spine>
            </package>
        """.trimIndent()

        val metadata = EpubMetadataParser.parse(Jsoup.parse(packageDoc))

        assertEquals("Epub 3", metadata.title)
        assertNull(metadata.author)
        assertNull(metadata.genre)
        assertTrue(metadata.modifiedDate > 0)
    }

    @Test
    fun `iso date parsing handles multiple formats`() {
        assertTrue(EpubMetadataParser.parseIsoDate("2024-01-15T10:00:00Z") > 0)
        assertTrue(EpubMetadataParser.parseIsoDate("2024-01-15T10:00:00+01:00") > 0)
        assertTrue(EpubMetadataParser.parseIsoDate("2024-01-15") > 0)
        assertEquals(0L, EpubMetadataParser.parseIsoDate("not a date"))
        assertEquals(0L, EpubMetadataParser.parseIsoDate(""))
    }

    // --- Table of contents -------------------------------------------------

    private fun epub3Nav(): String = """
        <html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops">
        <head><title>Contents</title></head>
        <body>
          <nav epub:type="toc" id="toc">
            <h1>Table of contents</h1>
            <ol>
              <li><a href="text/chapter1.xhtml">Chapter 1</a></li>
              <li><a href="text/chapter2.xhtml#part">Chapter 2</a></li>
              <li><a href="text/chapter3.xhtml">Chapter 3</a></li>
            </ol>
          </nav>
          <nav epub:type="landmarks">
            <ol><li><a href="cover.xhtml">Cover</a></li></ol>
          </nav>
        </body>
        </html>
    """.trimIndent()

    @Test
    fun `epub3 nav toc titles are parsed`() {
        val titles = EpubTocParser.parseTitlesFromNav(Jsoup.parse(epub3Nav()))

        assertEquals(
            mapOf(
                "text/chapter1.xhtml" to "Chapter 1",
                "text/chapter2.xhtml" to "Chapter 2",
                "text/chapter3.xhtml" to "Chapter 3",
            ),
            titles,
        )
    }

    private fun ncx(): String = """
        <ncx xmlns="http://www.daisy.org/z3986/2005/ncx/" version="2005-1">
          <navMap>
            <navPoint id="np1" playOrder="1">
              <navLabel><text>Chapter 1</text></navLabel>
              <content src="text/chapter1.xhtml"/>
            </navPoint>
            <navPoint id="np2" playOrder="2">
              <navLabel><text>Chapter 2</text></navLabel>
              <content src="text/chapter2.xhtml#part"/>
            </navPoint>
          </navMap>
        </ncx>
    """.trimIndent()

    @Test
    fun `epub2 ncx titles are parsed`() {
        val titles = EpubTocParser.parseTitlesFromNcx(Jsoup.parse(ncx()))

        assertEquals(
            mapOf(
                "text/chapter1.xhtml" to "Chapter 1",
                "text/chapter2.xhtml" to "Chapter 2",
            ),
            titles,
        )
    }

    // --- HTML cleaning ------------------------------------------------------

    private val pngBytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)

    @Test
    fun `boilerplate tags and hidden content are removed`() {
        val html = """
            <html>
              <head><title>Chapter</title><style>p { color: red }</style></head>
              <body>
                <script>window.bad()</script>
                <p style="display:none">Hidden</p>
                <p class="hidden">Also hidden</p>
                <p>Visible</p>
              </body>
            </html>
        """.trimIndent()

        val cleaned = EpubHtmlCleaner.clean(html, "OEBPS/Text/ch1.xhtml", 1024, 10) { _, _ -> null }

        assertFalse(cleaned.contains("<head"))
        assertFalse(cleaned.contains("<style"))
        assertFalse(cleaned.contains("<script"))
        assertFalse(cleaned.contains("Hidden"))
        assertTrue(cleaned.contains("Visible"))
    }

    @Test
    fun `inline images become data uris`() {
        val html = """
            <html>
              <body>
                <p>Before</p>
                <p><img src="../images/fig.png" alt="figure"/></p>
                <p>After</p>
              </body>
            </html>
        """.trimIndent()

        val cleaned = EpubHtmlCleaner.clean(html, "OEBPS/Text/ch1.xhtml", 1024, 10) { src, basePath ->
            assertEquals("../images/fig.png", src)
            assertEquals("OEBPS/Text", basePath)
            pngBytes
        }

        assertTrue(cleaned.contains("data:image/png;base64,"), "expected a data uri, got: $cleaned")
    }

    @Test
    fun `svg image uses xlink href`() {
        val html = """
            <html>
              <body>
                <p>Icon</p>
                <p><image xlink:href="../images/icon.png" /></p>
              </body>
            </html>
        """.trimIndent()

        val cleaned = EpubHtmlCleaner.clean(html, "OEBPS/Text/ch1.xhtml", 1024, 10) { _, _ -> pngBytes }

        assertTrue(cleaned.contains("data:image/png;base64,"))
    }

    @Test
    fun `oversized images are dropped`() {
        val html = "<html><body><p><img src=\"images/big.png\"/></p><p>Text</p></body></html>"

        val cleaned = EpubHtmlCleaner.clean(html, "OEBPS/ch1.xhtml", maxInlineImageBytes = 4, maxInlineImages = 10) {
                _,
                _,
            ->
            pngBytes
        }

        assertFalse(cleaned.contains("data:image"))
        assertTrue(cleaned.contains("Text"))
    }

    @Test
    fun `image limit caps the number of embedded images`() {
        val html = """
            <html><body>
              <p><img src="images/a.png"/></p>
              <p><img src="images/b.png"/></p>
            </body></html>
        """.trimIndent()

        val cleaned = EpubHtmlCleaner.clean(html, "OEBPS/ch1.xhtml", 1024, maxInlineImages = 1) { _, _ -> pngBytes }

        val occurrences = Regex("data:image/png;base64,").findAll(cleaned).count()
        assertEquals(1, occurrences)
    }

    @Test
    fun `mime type is resolved from the extension`() {
        assertEquals("image/jpeg", EpubHtmlCleaner.mimeForPath("images/cover.jpg"))
        assertEquals("image/jpeg", EpubHtmlCleaner.mimeForPath("images/cover.JPEG"))
        assertEquals("image/png", EpubHtmlCleaner.mimeForPath("OEBPS/Images/fig.png"))
        assertNull(EpubHtmlCleaner.mimeForPath("styles/main.css"))
        assertNull(EpubHtmlCleaner.mimeForPath("chapter.xhtml"))
    }
}
