@file:Suppress("PropertyName")

package eu.kanade.tachiyomi.novelsource.model

import java.io.Serializable

/**
 * A chapter of a novel. Unlike image based chapters, the content of a novel chapter is
 * plain (rich) text that is fetched with [eu.kanade.tachiyomi.novelsource.NovelSource.getChapterText].
 */
interface SNovelChapter : Serializable {

    var url: String

    var name: String

    var date_upload: Long

    var chapter_number: Float

    var scanlator: String?

    fun copyFrom(other: SNovelChapter) {
        name = other.name
        url = other.url
        date_upload = other.date_upload
        chapter_number = other.chapter_number
        scanlator = other.scanlator
    }

    companion object {
        fun create(): SNovelChapter {
            return SNovelChapterImpl()
        }
    }
}
