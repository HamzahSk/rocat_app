package eu.kanade.domain.extension.novel.model

import eu.kanade.tachiyomi.extension.novel.model.NovelExtension

data class NovelExtensions(
    val updates: List<NovelExtension.Installed>,
    val installed: List<NovelExtension.Installed>,
    val available: List<NovelExtension.Available>,
    val untrusted: List<NovelExtension.Untrusted>,
)
