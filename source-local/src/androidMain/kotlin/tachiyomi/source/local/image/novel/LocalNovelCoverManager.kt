package tachiyomi.source.local.image.novel

import android.content.Context
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.novelsource.model.SNovel
import eu.kanade.tachiyomi.util.storage.DiskUtil
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.source.local.epub.epubParser
import tachiyomi.source.local.io.novel.LocalNovelSourceFileSystem

actual class LocalNovelCoverManager(
    private val context: Context,
    private val fileSystem: LocalNovelSourceFileSystem,
) {

    /**
     * Looks up a previously extracted cover for the given novel.
     */
    actual fun find(novelUrl: String): UniFile? {
        return getCoverDirectory()
            ?.findFile(coverName(novelUrl))
            ?.takeIf { it.isFile }
    }

    /**
     * Extracts the cover declared in the EPUB and caches it next to the other covers so
     * [find] is cheap on later lookups.
     */
    actual fun update(
        novel: SNovel,
        epubFile: UniFile,
    ): UniFile? {
        val cover = try {
            epubFile.epubParser(context).use { it.cover() }
        } catch (e: Throwable) {
            logcat(LogPriority.ERROR, e) { "Failed to extract cover from ${epubFile.name}" }
            null
        } ?: return null

        val coverDirectory = getCoverDirectory() ?: return null
        val targetFile = coverDirectory.findFile(coverName(novel.url))
            ?: coverDirectory.createFile(coverName(novel.url))
            ?: return null

        targetFile.openOutputStream().use { output ->
            output.write(cover.first)
        }
        DiskUtil.createNoMediaFile(coverDirectory, context)

        novel.thumbnail_url = targetFile.uri.toString()
        return targetFile
    }

    private fun getCoverDirectory(): UniFile? {
        return fileSystem.getBaseDirectory()?.createDirectory(COVER_DIRECTORY)
    }

    private fun coverName(novelUrl: String): String {
        return "$novelUrl.cover.jpg"
    }

    companion object {
        private const val COVER_DIRECTORY = ".covers"
    }
}
