package eu.kanade.tachiyomi.ui.browse.novel.extension

import android.app.Application
import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import dev.icerock.moko.resources.StringResource
import eu.kanade.domain.extension.novel.interactor.GetNovelExtensionsByType
import eu.kanade.presentation.components.SEARCH_DEBOUNCE_MILLIS
import eu.kanade.tachiyomi.extension.novel.NovelExtensionManager
import eu.kanade.tachiyomi.extension.novel.model.NovelExtension
import eu.kanade.tachiyomi.util.system.LocaleHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.time.Duration.Companion.seconds

class NovelExtensionsScreenModel(
    private val extensionManager: NovelExtensionManager = Injekt.get(),
    private val getExtensions: GetNovelExtensionsByType = Injekt.get(),
) : StateScreenModel<NovelExtensionsScreenModel.State>(State()) {

    init {
        val context = Injekt.get<Application>()
        screenModelScope.launchIO {
            combine(
                state.map { it.searchQuery }.distinctUntilChanged().debounce(SEARCH_DEBOUNCE_MILLIS),
                getExtensions.subscribe(),
            ) { query, (_updates, _installed, _available, _untrusted) ->
                val searchQuery = query ?: ""
                val queryFilter: (String) -> ((NovelExtension) -> Boolean) = { _query ->
                    filter@{ extension ->
                        val inputQuery = _query.trim()
                        if (inputQuery.isEmpty()) return@filter true
                        inputQuery.split(",").any { _input ->
                            val input = _input.trim()
                            if (input.isEmpty()) return@any false
                            when (extension) {
                                is NovelExtension.Available -> {
                                    extension.sources.any {
                                        it.name.contains(input, ignoreCase = true) ||
                                            it.baseUrl.contains(input, ignoreCase = true) ||
                                            it.id == input.toLongOrNull()
                                    } ||
                                        extension.name.contains(input, ignoreCase = true)
                                }
                                is NovelExtension.Installed -> {
                                    extension.sources.any {
                                        it.name.contains(input, ignoreCase = true) ||
                                            it.id == input.toLongOrNull()
                                    } ||
                                        extension.name.contains(input, ignoreCase = true)
                                }
                                is NovelExtension.Untrusted -> extension.name.contains(
                                    input,
                                    ignoreCase = true,
                                )
                            }
                        }
                    }
                }

                val itemsGroups: ItemGroups = mutableMapOf()

                val installed = _installed.filter(queryFilter(searchQuery))
                val untrusted = _untrusted.filter(queryFilter(searchQuery))
                if (installed.isNotEmpty() || untrusted.isNotEmpty()) {
                    itemsGroups[NovelExtensionUiModel.Header.Resource(MR.strings.ext_installed)] =
                        installed + untrusted
                }

                val languagesWithExtensions = _available
                    .filter(queryFilter(searchQuery))
                    .groupBy { it.lang }
                    .toSortedMap(LocaleHelper.comparator)
                    .map { (lang, exts) ->
                        NovelExtensionUiModel.Header.Text(
                            LocaleHelper.getSourceDisplayName(lang, context),
                        ) to exts
                    }

                if (languagesWithExtensions.isNotEmpty()) {
                    itemsGroups.putAll(languagesWithExtensions)
                }

                itemsGroups
            }
                .collectLatest {
                    mutableState.update { state ->
                        state.copy(
                            isLoading = false,
                            items = it,
                        )
                    }
                }
        }
    }

    fun search(query: String?) {
        mutableState.update {
            it.copy(searchQuery = query)
        }
    }

    fun uninstallExtension(extension: NovelExtension) {
        extensionManager.uninstallExtension(extension)
    }

    fun trustExtension(extension: NovelExtension.Untrusted) {
        screenModelScope.launchIO {
            extensionManager.trust(extension)
        }
    }

    fun refresh() {
        screenModelScope.launchIO {
            mutableState.update { it.copy(isRefreshing = true) }

            extensionManager.reload()

            // Fake slower refresh so it doesn't seem like it's not doing anything
            delay(1.seconds)

            mutableState.update { it.copy(isRefreshing = false) }
        }
    }

    @Immutable
    data class State(
        val isLoading: Boolean = true,
        val isRefreshing: Boolean = false,
        val items: ItemGroups = mutableMapOf(),
        val searchQuery: String? = null,
    ) {
        val isEmpty = items.isEmpty()
    }
}

typealias ItemGroups = MutableMap<NovelExtensionUiModel.Header, List<NovelExtension>>

object NovelExtensionUiModel {
    sealed interface Header {
        data class Resource(val textRes: StringResource) : Header
        data class Text(val text: String) : Header
    }
}
