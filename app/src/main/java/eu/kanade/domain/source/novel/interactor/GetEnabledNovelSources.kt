package eu.kanade.domain.source.novel.interactor

import eu.kanade.domain.source.service.SourcePreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import tachiyomi.domain.source.manga.model.Pin
import tachiyomi.domain.source.manga.model.Pins
import tachiyomi.domain.source.novel.model.Source
import tachiyomi.domain.source.novel.repository.NovelSourceRepository

class GetEnabledNovelSources(
    private val repository: NovelSourceRepository,
    private val preferences: SourcePreferences,
) {

    fun subscribe(): Flow<List<Source>> {
        return combine(
            preferences.pinnedNovelSources().changes(),
            preferences.enabledLanguages().changes(),
            combine(
                preferences.disabledNovelSources().changes(),
                preferences.lastUsedNovelSource().changes(),
            ) { a, b -> Pair(a, b) },
            repository.getNovelSources(),
        ) { pinnedSourceIds, enabledLanguages, (disabledSources, lastUsedSource), sources ->
            sources
                .filter { it.lang in enabledLanguages }
                .filterNot { it.id.toString() in disabledSources }
                .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
                .flatMap {
                    val flag = if ("${it.id}" in pinnedSourceIds) Pins.pinned else Pins.unpinned
                    val source = it.copy(
                        pin = flag,
                    )
                    val toFlatten = mutableListOf(source)
                    if (source.id == lastUsedSource) {
                        toFlatten.add(source.copy(isUsedLast = true, pin = source.pin - Pin.Actual))
                    }
                    toFlatten
                }
        }
            .distinctUntilChanged()
    }
}
