package eu.kanade.domain.entries.novel.model

import eu.kanade.tachiyomi.novelsource.model.SNovel
import tachiyomi.domain.entries.novel.model.Novel

/**
 * Conversions between the domain [Novel] and the source model [SNovel], mirroring the
 * `Manga`/`SManga` helpers.
 */
fun Novel.toSNovel(): SNovel = SNovel.create().also {
    it.url = url
    it.title = title
    it.artist = artist
    it.author = author
    it.description = description
    it.genre = genre.orEmpty().joinToString()
    it.status = status.toInt()
    it.thumbnail_url = thumbnailUrl
    it.initialized = initialized
}

fun Novel.copyFrom(other: SNovel): Novel {
    val author = other.author ?: author
    val artist = other.artist ?: artist
    val description = other.description ?: description
    val genres = if (other.genre != null) {
        other.getGenres()
    } else {
        genre
    }
    val thumbnailUrl = other.thumbnail_url ?: thumbnailUrl
    return this.copy(
        author = author,
        artist = artist,
        description = description,
        genre = genres,
        thumbnailUrl = thumbnailUrl,
        status = other.status.toLong(),
        updateStrategy = other.update_strategy,
        initialized = other.initialized && initialized,
    )
}

fun SNovel.toDomainNovel(sourceId: Long): Novel {
    return Novel.create().copy(
        url = url,
        title = title,
        artist = artist,
        author = author,
        description = description,
        genre = getGenres(),
        status = status.toLong(),
        thumbnailUrl = thumbnail_url,
        updateStrategy = update_strategy,
        initialized = initialized,
        source = sourceId,
    )
}
