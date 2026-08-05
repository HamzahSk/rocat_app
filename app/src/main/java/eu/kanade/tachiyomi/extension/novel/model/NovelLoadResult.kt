package eu.kanade.tachiyomi.extension.novel.model

sealed class NovelLoadResult {
    data class Success(val extension: NovelExtension.Installed) : NovelLoadResult()
    data class Untrusted(val extension: NovelExtension.Untrusted) : NovelLoadResult()
    data object Error : NovelLoadResult()
}
