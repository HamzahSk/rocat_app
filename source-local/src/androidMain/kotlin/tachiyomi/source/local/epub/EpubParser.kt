package tachiyomi.source.local.epub

import android.content.Context
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.novelsource.model.SNovel
import eu.kanade.tachiyomi.novelsource.util.NovelChapterNumberParser
import mihon.core.archive.EpubReader
import mihon.core.archive.epubReader
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser
import java.io.Closeable
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.format.DateTimeParseException
import java.util.Base64
import java.util.Locale

/**
 * Metadata extracted from an EPUB package document.
 */
data class EpubNovelMetadata(
    val title: String? = null,
    val author: String? = null,
    val artist: String? = null,
    val description: String? = null,
    val genre: String? = null,
    val publisher: String? = null,
    val status: Int = SNovel.UNKNOWN,
    val modifiedDate: Long = 0L,
)

/**
 * A single chapter (spine page) of an EPUB novel.
 *
 * @property url unique identifier used by [eu.kanade.tachiyomi.novelsource.NovelSource],
 *   it is the EPUB file name plus the resolved zip path of the XHTML page.
 * @property href resolved zip path of the XHTML page inside the archive.
 */
data class EpubChapter(
    val url: String,
    val href: String,
    val name: String,
    val number: Float,
)

/**
 * Parses an EPUB archive into data usable by a text based [eu.kanade.tachiyomi.novelsource.NovelSource].
 *
 * All IO happens through the mmap backed [EpubReader] so the whole archive is never loaded into
 * memory, keeping large books (< 100 MB) inside reasonable memory bounds.
 */
class EpubParser(private val reader: EpubReader) : Closeable by reader {

    private val packageHref: String by lazy { reader.getPackageHref() }

    private val packageDocument: Document by lazy {
        // OPF is XML; the XML parser keeps `<meta>` non-void so `dcterms:modified` text survives.
        reader.getInputStream(packageHref)?.use {
            Jsoup.parse(it, null, "", Parser.xmlParser())
        } ?: Jsoup.parse("")
    }

    private val packageBasePath: String
        get() = packageHref.substringBeforeLast('/', "").ifEmpty { "" }

    /**
     * Extracts the metadata declared in the package document (content.opf).
     */
    fun metadata(): EpubNovelMetadata {
        return EpubMetadataParser.parse(packageDocument)
    }

    /**
     * Returns the chapters of the novel in spine (reading) order.
     *
     * @param epubFileName the file name of the EPUB; used to build stable chapter urls.
     */
    fun chapters(epubFileName: String): List<EpubChapter> {
        val manifest = packageDocument.select("manifest > item")
            .associateBy { it.attr("id") }
        val pageHrefs = packageDocument.select("spine > itemref")
            .mapNotNull { manifest[it.attr("idref")]?.attr("href") }

        val tocTitles = parseTocTitles(packageDocument, manifest)

        return pageHrefs.mapIndexed { index, href ->
            val resolved = resolvePagePath(href)
            val name = tocTitles[resolved] ?: "Chapter ${index + 1}"
            val parsedNumber = NovelChapterNumberParser.parse("", name).toFloat()
            val number = if (parsedNumber > 0f) parsedNumber else (index + 1).toFloat()
            EpubChapter(
                url = "$epubFileName/$resolved",
                href = resolved,
                name = name,
                number = number,
            )
        }
    }

    /**
     * Returns the cleaned XHTML of a chapter. Inline images are embedded as `data:` URIs so the
     * reader can render illustrations without re-opening the archive.
     */
    fun chapterText(
        href: String,
        maxInlineImageBytes: Int = MAX_INLINE_IMAGE_BYTES,
        maxInlineImages: Int = MAX_INLINE_IMAGES,
    ): String {
        val html = reader.getInputStream(href)?.use { it.readBytes().toString(Charsets.UTF_8) } ?: return ""
        return EpubHtmlCleaner.clean(
            html = html,
            pagePath = href,
            maxInlineImageBytes = maxInlineImageBytes,
            maxInlineImages = maxInlineImages,
        ) { src, basePath ->
            reader.getInputStream(reader.resolveEntry(src, basePath))?.use { it.readBytes() }
        }
    }

    /**
     * Returns the cover image bytes and its mime type if the EPUB declares one.
     */
    fun cover(): Pair<ByteArray, String>? {
        val coverHref = reader.getCoverHref() ?: return null
        val mime = EpubHtmlCleaner.mimeForPath(coverHref) ?: return null
        val bytes = reader.getInputStream(coverHref)?.use { it.readBytes() } ?: return null
        return bytes to mime
    }

    private fun resolvePagePath(href: String): String {
        return reader.resolveEntry(href, packageBasePath)
    }

    private fun parseTocTitles(doc: Document, manifest: Map<String, Element>): Map<String, String> {
        val titles = LinkedHashMap<String, String>()

        // EPUB 3: dedicated navigation document
        val navItem = manifest.values.firstOrNull {
            it.attr("properties").contains("nav", ignoreCase = true) ||
                it.attr("href").contains("nav", ignoreCase = true)
        }
        if (navItem != null) {
            val navHref = resolvePagePath(navItem.attr("href"))
            val navDoc = reader.getInputStream(navHref)?.use { Jsoup.parse(it, null, "") }
            if (navDoc != null) {
                EpubTocParser.parseTitlesFromNav(navDoc).forEach { (href, text) ->
                    titles[resolvePagePath(href)] = text
                }
                if (titles.isNotEmpty()) return titles
            }
        }

        // EPUB 2: NCX navigation map
        val tocId = doc.select("spine").firstOrNull()?.attr("toc")
        if (!tocId.isNullOrBlank()) {
            val ncxHref = manifest[tocId]?.attr("href")
            if (ncxHref != null) {
                val ncxDoc = reader.getInputStream(resolvePagePath(ncxHref))?.use {
                    Jsoup.parse(it, null, "")
                }
                if (ncxDoc != null) {
                    EpubTocParser.parseTitlesFromNcx(ncxDoc).forEach { (href, text) ->
                        titles[resolvePagePath(href)] = text
                    }
                }
            }
        }

        return titles
    }

    companion object {
        const val MAX_INLINE_IMAGE_BYTES = 3 * 1024 * 1024
        const val MAX_INLINE_IMAGES = 100
    }
}

/**
 * Pure parsing helpers so the metadata extraction can be unit tested on the JVM.
 */
object EpubMetadataParser {

    fun parse(doc: Document): EpubNovelMetadata {
        val title = doc.getElementsByTag("dc:title").firstOrNull()?.text()?.trim()
        val author = doc.getElementsByTag("dc:creator").firstOrNull()?.text()?.trim()
        val artist = doc.getElementsByTag("dc:contributor").firstOrNull()?.text()?.trim()
        val description = doc.getElementsByTag("dc:description").firstOrNull()?.text()?.trim()
        val subjects = doc.getElementsByTag("dc:subject")
            .map { it.text().trim() }
            .filterNot { it.isEmpty() }
        val genre = subjects.joinToString(", ").ifEmpty { null }
        val publisher = doc.getElementsByTag("dc:publisher").firstOrNull()?.text()?.trim()
        var date = doc.getElementsByTag("dc:date").firstOrNull()?.text()
        if (date.isNullOrBlank()) {
            // The value may live in a `content` attribute, in the element text, or (when the
            // document was parsed with the HTML parser) in the following text node, since `<meta>`
            // is treated as a void element there.
            doc.select("meta[property=dcterms:modified]").firstOrNull()?.let { meta ->
                date = meta.attr("content")
                    .ifBlank { meta.text() }
                    .ifBlank { meta.nextSibling()?.outerHtml()?.trim() }
            }
        }
        return EpubNovelMetadata(
            title = title,
            author = author,
            artist = artist,
            description = description,
            genre = genre,
            publisher = publisher,
            modifiedDate = parseIsoDate(date.orEmpty()),
        )
    }

    fun parseIsoDate(date: String): Long {
        if (date.isBlank()) return 0L
        val trimmed = date.trim()
        // Try the full ISO 8601 date-time form first.
        try {
            val normalized = if (trimmed.endsWith("Z", ignoreCase = true)) {
                trimmed.dropLast(1) + "+00:00"
            } else {
                trimmed
            }
            if (normalized.contains('T')) {
                return Instant.parse(normalized).toEpochMilli()
            }
        } catch (e: DateTimeParseException) {
            // Fall through to lenient formats.
        }
        return try {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(trimmed)?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
}

/**
 * Cleans raw EPUB XHTML into reader friendly HTML: removes boilerplate tags and embeds inline
 * images as base64 `data:` URIs so illustrations render without holding the archive open.
 *
 * The [loadImage] callback resolves a relative image path against the page base path and returns
 * the image bytes (or null when the asset can't be loaded). It is injected so the cleaner stays
 * testable without an [EpubReader].
 */
object EpubHtmlCleaner {

    private val IMAGE_MIME_TYPES = mapOf(
        "jpg" to "image/jpeg",
        "jpeg" to "image/jpeg",
        "png" to "image/png",
        "gif" to "image/gif",
        "webp" to "image/webp",
        "bmp" to "image/bmp",
        "avif" to "image/avif",
    )

    fun clean(
        html: String,
        pagePath: String,
        maxInlineImageBytes: Int,
        maxInlineImages: Int,
        loadImage: (src: String, basePath: String) -> ByteArray?,
    ): String {
        val doc = Jsoup.parse(html)
        doc.select("head, script, style, link, meta, title, noscript").forEach { it.remove() }
        doc.select("[hidden], .hidden, .d-none, .noprint").forEach { it.remove() }
        doc.select("[style]").forEach { element ->
            val style = element.attr("style")
            if (
                style.contains("display:none", ignoreCase = true) ||
                style.contains("display: none", ignoreCase = true) ||
                style.contains("visibility:hidden", ignoreCase = true) ||
                style.contains("visibility: hidden", ignoreCase = true)
            ) {
                element.remove()
            }
        }

        val basePath = pagePath.substringBeforeLast('/', "")
        var inlineCount = 0
        doc.select("img, image").forEach { element ->
            if (inlineImage(element, basePath, maxInlineImageBytes, maxInlineImages, inlineCount, loadImage)) {
                inlineCount++
            }
        }

        return doc.body()?.html().orEmpty()
    }

    fun mimeForPath(path: String): String? {
        return IMAGE_MIME_TYPES[path.substringAfterLast('.').lowercase()]
    }

    private fun inlineImage(
        element: Element,
        basePath: String,
        maxBytes: Int,
        maxImages: Int,
        inlineCount: Int,
        loadImage: (src: String, basePath: String) -> ByteArray?,
    ): Boolean {
        // Jsoup normalizes the SVG `<image>` element to `<img>`, keeping the `xlink:href`
        // attribute, so it is probed as a source fallback.
        val src = element.attr("src")
            .ifBlank { element.attr("data-src") }
            .ifBlank { element.attr("xlink:href") }
        if (src.isBlank() || inlineCount >= maxImages) {
            element.remove()
            return false
        }

        val mime = mimeForPath(src.substringAfterLast('.').let { if ('?' in it) it.substringBefore('?') else it })
        if (mime == null) {
            element.remove()
            return false
        }

        val bytes = loadImage(src, basePath)
        if (bytes == null || bytes.isEmpty() || bytes.size > maxBytes) {
            element.remove()
            return false
        }

        val dataUri = "data:$mime;base64,${Base64.getEncoder().encodeToString(bytes)}"
        element.removeAttr("srcset").removeAttr("data-src")
        element.attr("src", dataUri)
        return true
    }
}

/**
 * Opens an [EpubParser] for a local EPUB file.
 */
fun UniFile.epubParser(context: Context): EpubParser {
    return EpubParser(epubReader(context))
}
