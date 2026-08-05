package tachiyomi.domain.entries.novel.model

import androidx.compose.runtime.Immutable
import eu.kanade.tachiyomi.novelsource.model.SNovel
import eu.kanade.tachiyomi.source.model.UpdateStrategy
import tachiyomi.core.common.preference.TriState
import java.io.Serializable
import java.time.Instant

/**
 * A novel stored in the local database. Text-based counterpart of
 * [tachiyomi.domain.entries.manga.model.Manga].
 */
@Immutable
data class Novel(
    val id: Long,
    val source: Long,
    val favorite: Boolean,
    val lastUpdate: Long,
    val nextUpdate: Long,
    val fetchInterval: Int,
    val dateAdded: Long,
    val viewerFlags: Long,
    val chapterFlags: Long,
    val coverLastModified: Long,
    val url: String,
    val title: String,
    val artist: String?,
    val author: String?,
    val description: String?,
    val genre: List<String>?,
    val status: Long,
    val thumbnailUrl: String?,
    val updateStrategy: UpdateStrategy,
    val initialized: Boolean,
    val lastModifiedAt: Long,
    val favoriteModifiedAt: Long?,
    val version: Long,
) : Serializable {

    val expectedNextUpdate: Instant?
        get() = nextUpdate
            .takeIf { status != SNovel.COMPLETED.toLong() }
            ?.let { Instant.ofEpochMilli(it) }

    val unreadFilter: TriState
        get() = when (chapterFlags and CHAPTER_UNREAD_MASK) {
            CHAPTER_SHOW_UNREAD -> TriState.ENABLED_IS
            CHAPTER_SHOW_READ -> TriState.ENABLED_NOT
            else -> TriState.DISABLED
        }

    val bookmarkedFilter: TriState
        get() = when (chapterFlags and CHAPTER_BOOKMARKED_MASK) {
            CHAPTER_SHOW_BOOKMARKED -> TriState.ENABLED_IS
            CHAPTER_SHOW_NOT_BOOKMARKED -> TriState.ENABLED_NOT
            else -> TriState.DISABLED
        }

    companion object {
        const val SHOW_ALL = 0x00000000L

        const val CHAPTER_SHOW_UNREAD = 0x00000002L
        const val CHAPTER_SHOW_READ = 0x00000004L
        const val CHAPTER_UNREAD_MASK = 0x00000006L

        const val CHAPTER_SHOW_BOOKMARKED = 0x00000020L
        const val CHAPTER_SHOW_NOT_BOOKMARKED = 0x00000040L
        const val CHAPTER_BOOKMARKED_MASK = 0x00000060L

        fun create() = Novel(
            id = -1L,
            url = "",
            title = "",
            source = -1L,
            favorite = false,
            lastUpdate = 0L,
            nextUpdate = 0L,
            fetchInterval = 0,
            dateAdded = 0L,
            viewerFlags = 0L,
            chapterFlags = 0L,
            coverLastModified = 0L,
            artist = null,
            author = null,
            description = null,
            genre = null,
            status = 0L,
            thumbnailUrl = null,
            updateStrategy = UpdateStrategy.ALWAYS_UPDATE,
            initialized = false,
            lastModifiedAt = 0L,
            favoriteModifiedAt = null,
            version = 0L,
        )
    }
}
