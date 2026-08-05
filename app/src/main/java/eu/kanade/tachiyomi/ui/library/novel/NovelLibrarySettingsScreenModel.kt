package eu.kanade.tachiyomi.ui.library.novel

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.domain.base.BasePreferences
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.TriState
import tachiyomi.core.common.preference.getAndSet
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.category.novel.interactor.SetNovelDisplayMode
import tachiyomi.domain.category.novel.interactor.SetSortModeForNovelCategory
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.library.novel.model.NovelLibrarySort
import tachiyomi.domain.library.service.LibraryPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class NovelLibrarySettingsScreenModel(
    val preferences: BasePreferences = Injekt.get(),
    val libraryPreferences: LibraryPreferences = Injekt.get(),
    private val setNovelDisplayMode: SetNovelDisplayMode = Injekt.get(),
    private val setSortModeForCategory: SetSortModeForNovelCategory = Injekt.get(),
) : ScreenModel {

    fun toggleFilter(preference: (LibraryPreferences) -> Preference<TriState>) {
        preference(libraryPreferences).getAndSet {
            it.next()
        }
    }

    fun setDisplayMode(mode: LibraryDisplayMode) {
        setNovelDisplayMode.await(mode)
    }

    fun setSort(
        category: Category?,
        mode: NovelLibrarySort.Type,
        direction: NovelLibrarySort.Direction,
    ) {
        screenModelScope.launchIO {
            setSortModeForCategory.await(category, mode, direction)
        }
    }
}
