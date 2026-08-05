@file:Suppress("PropertyName")

package eu.kanade.tachiyomi.novelsource.model

import eu.kanade.tachiyomi.source.model.UpdateStrategy
import java.io.Serializable

/**
 * A novel entry returned by a [eu.kanade.tachiyomi.novelsource.NovelSource].
 *
 * This is the text-based sibling of [eu.kanade.tachiyomi.source.model.SManga]. It is
 * intentionally mirror shaped so the repository layer can treat novels and manga in a
 * similar fashion while keeping the read content (text vs images) completely separate.
 */
interface SNovel : Serializable {

    var url: String

    var title: String

    var artist: String?

    var author: String?

    var description: String?

    var genre: String?

    var status: Int

    var thumbnail_url: String?

    var update_strategy: UpdateStrategy

    var initialized: Boolean

    fun getGenres(): List<String>? {
        if (genre.isNullOrBlank()) return null
        return genre?.split(", ")?.map { it.trim() }?.filterNot { it.isBlank() }?.distinct()
    }

    fun copy() = create().also {
        it.url = url
        it.title = title
        it.artist = artist
        it.author = author
        it.description = description
        it.genre = genre
        it.status = status
        it.thumbnail_url = thumbnail_url
        it.update_strategy = update_strategy
        it.initialized = initialized
    }

    companion object {
        const val UNKNOWN = 0
        const val ONGOING = 1
        const val COMPLETED = 2
        const val LICENSED = 3
        const val PUBLISHING_FINISHED = 4
        const val CANCELLED = 5
        const val ON_HIATUS = 6

        fun create(): SNovel {
            return SNovelImpl()
        }
    }
}
