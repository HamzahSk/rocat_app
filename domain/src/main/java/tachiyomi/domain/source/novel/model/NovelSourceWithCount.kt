package tachiyomi.domain.source.novel.model

data class NovelSourceWithCount(
    val source: Source,
    val count: Long,
) {

    val id: Long
        get() = source.id

    val name: String
        get() = source.name
}
