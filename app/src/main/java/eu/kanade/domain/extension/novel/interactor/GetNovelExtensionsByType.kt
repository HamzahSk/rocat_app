package eu.kanade.domain.extension.novel.interactor

import eu.kanade.domain.extension.novel.model.NovelExtensions
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.extension.novel.NovelExtensionManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GetNovelExtensionsByType(
    private val preferences: SourcePreferences,
    private val extensionManager: NovelExtensionManager,
) {

    fun subscribe(): Flow<NovelExtensions> {
        val showNsfwSources = preferences.showNsfwSource().get()

        return combine(
            extensionManager.installedExtensionsFlow,
            extensionManager.untrustedExtensionsFlow,
        ) { _installed, _untrusted ->
            val installed = _installed
                .filter { showNsfwSources || !it.isNsfw }
                .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })

            val untrusted = _untrusted
                .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })

            NovelExtensions(
                updates = emptyList(),
                installed = installed,
                available = emptyList(),
                untrusted = untrusted,
            )
        }
    }
}
