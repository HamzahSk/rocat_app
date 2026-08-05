package eu.kanade.tachiyomi.ui.library.novel

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastDistinctBy
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapNotNull
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.core.preference.PreferenceMutableState
import eu.kanade.core.preference.asState
import eu.kanade.core.util.fastFilterNot
import eu.kanade.core.util.fastPartition
import eu.kanade.domain.items.novelchapter.interactor.SetNovelReadStatus
import eu.kanade.presentation.components.SEARCH_DEBOUNCE_MILLIS
import eu.kanade.presentation.library.components.LibraryToolbarTitle
import eu.kanade.tachiyomi.source.novel.getNameForNovelInfo
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import tachiyomi.core.common.preference.CheckboxState
import tachiyomi.core.common.preference.TriState
import tachiyomi.core.common.util.lang.compareToWithCollator
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.category.novel.interactor.GetNovelCategories
import tachiyomi.domain.category.novel.interactor.GetVisibleNovelCategories
import tachiyomi.domain.category.novel.interactor.SetNovelCategories
import tachiyomi.domain.entries.novel.interactor.GetLibraryNovel
import tachiyomi.domain.entries.novel.interactor.UpdateNovel
import tachiyomi.domain.entries.novel.model.Novel
import tachiyomi.domain.entries.novel.model.NovelUpdate
import tachiyomi.domain.items.novelchapter.interactor.GetChaptersByNovelId
import tachiyomi.domain.items.novelchapter.model.NovelChapter
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.library.novel.LibraryNovel
import tachiyomi.domain.library.novel.model.NovelLibrarySort
import tachiyomi.domain.library.novel.model.sort
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.source.novel.service.NovelSourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.random.Random

/**
 * Typealias for the library novel, using the category as keys, and list of novels as values.
 */
typealias NovelLibraryMap = Map<Category, List<NovelLibraryItem>>

class NovelLibraryScreenModel(
    private val getLibraryNovel: GetLibraryNovel = Injekt.get(),
    private val getCategories: GetVisibleNovelCategories = Injekt.get(),
    private val getNovelCategories: GetNovelCategories = Injekt.get(),
    private val setReadStatus: SetNovelReadStatus = Injekt.get(),
    private val updateNovel: UpdateNovel = Injekt.get(),
    private val setNovelCategories: SetNovelCategories = Injekt.get(),
    private val libraryPreferences: LibraryPreferences = Injekt.get(),
    private val sourceManager: NovelSourceManager = Injekt.get(),
    private val getChaptersByNovelId: GetChaptersByNovelId = Injekt.get(),
) : StateScreenModel<NovelLibraryScreenModel.State>(State()) {

    var activeCategoryIndex: Int by libraryPreferences.lastUsedNovelCategory().asState(
        screenModelScope,
    )

    init {
        screenModelScope.launchIO {
            combine(
                state.map { it.searchQuery }.debounce(SEARCH_DEBOUNCE_MILLIS),
                getLibraryFlow(),
            ) { searchQuery, library ->
                library
                    .applySort()
                    .mapValues { (_, value) ->
                        if (searchQuery != null) {
                            value.filter { it.matches(searchQuery) }
                        } else {
                            value
                        }
                    }
            }
                .collectLatest {
                    mutableState.update { state ->
                        state.copy(
                            isLoading = false,
                            library = it,
                        )
                    }
                }
        }

        combine(
            libraryPreferences.categoryTabs().changes(),
            libraryPreferences.categoryNumberOfItems().changes(),
            libraryPreferences.showContinueViewingButton().changes(),
        ) { a, b, c -> arrayOf(a, b, c) }
            .onEach { (showCategoryTabs, showNovelCount, showNovelContinueButton) ->
                mutableState.update { state ->
                    state.copy(
                        showCategoryTabs = showCategoryTabs,
                        showNovelCount = showNovelCount,
                        showNovelContinueButton = showNovelContinueButton,
                    )
                }
            }
            .launchIn(screenModelScope)

        getLibraryItemPreferencesFlow()
            .map { prefs ->
                listOf(
                    prefs.filterUnread,
                    prefs.filterStarted,
                    prefs.filterBookmarked,
                    prefs.filterCompleted,
                ).any { it != TriState.DISABLED }
            }
            .distinctUntilChanged()
            .onEach {
                mutableState.update { state ->
                    state.copy(hasActiveFilters = it)
                }
            }
            .launchIn(screenModelScope)
    }

    private fun NovelLibraryMap.applySort(): NovelLibraryMap {
        val sortAlphabetically: (NovelLibraryItem, NovelLibraryItem) -> Int = { i1, i2 ->
            i1.libraryNovel.novel.title.lowercase().compareToWithCollator(i2.libraryNovel.novel.title.lowercase())
        }

        fun NovelLibrarySort.comparator(): Comparator<NovelLibraryItem> = Comparator { i1, i2 ->
            when (this.type) {
                NovelLibrarySort.Type.Alphabetical -> {
                    sortAlphabetically(i1, i2)
                }
                NovelLibrarySort.Type.LastRead -> {
                    i1.libraryNovel.lastRead.compareTo(i2.libraryNovel.lastRead)
                }
                NovelLibrarySort.Type.LastUpdate -> {
                    i1.libraryNovel.novel.lastUpdate.compareTo(i2.libraryNovel.novel.lastUpdate)
                }
                NovelLibrarySort.Type.UnreadCount -> when {
                    i1.libraryNovel.unreadCount == i2.libraryNovel.unreadCount -> 0
                    i1.libraryNovel.unreadCount == 0L -> if (this.isAscending) 1 else -1
                    i2.libraryNovel.unreadCount == 0L -> if (this.isAscending) -1 else 1
                    else -> i1.libraryNovel.unreadCount.compareTo(i2.libraryNovel.unreadCount)
                }
                NovelLibrarySort.Type.TotalChapters -> {
                    i1.libraryNovel.totalChapters.compareTo(i2.libraryNovel.totalChapters)
                }
                NovelLibrarySort.Type.LatestChapter -> {
                    i1.libraryNovel.latestUpload.compareTo(i2.libraryNovel.latestUpload)
                }
                NovelLibrarySort.Type.ChapterFetchDate -> {
                    i1.libraryNovel.chapterFetchedAt.compareTo(i2.libraryNovel.chapterFetchedAt)
                }
                NovelLibrarySort.Type.DateAdded -> {
                    i1.libraryNovel.novel.dateAdded.compareTo(i2.libraryNovel.novel.dateAdded)
                }
                NovelLibrarySort.Type.Random -> {
                    error("Why Are We Still Here? Just To Suffer?")
                }
            }
        }

        return mapValues { (key, value) ->
            if (key.sort.type == NovelLibrarySort.Type.Random) {
                return@mapValues value.shuffled(Random(libraryPreferences.randomNovelSortSeed().get()))
            }

            val comparator = key.sort.comparator()
                .let { if (key.sort.isAscending) it else it.reversed() }
                .thenComparator(sortAlphabetically)

            value.sortedWith(comparator)
        }
    }

    private fun getLibraryItemPreferencesFlow(): Flow<ItemPreferences> {
        return combine(
            libraryPreferences.unreadBadge().changes(),
            libraryPreferences.languageBadge().changes(),
            libraryPreferences.filterUnread().changes(),
            libraryPreferences.filterStartedManga().changes(),
            libraryPreferences.filterBookmarkedManga().changes(),
            libraryPreferences.filterCompletedManga().changes(),
        ) {
            ItemPreferences(
                unreadBadge = it[0] as Boolean,
                languageBadge = it[1] as Boolean,
                filterUnread = it[2] as TriState,
                filterStarted = it[3] as TriState,
                filterBookmarked = it[4] as TriState,
                filterCompleted = it[5] as TriState,
            )
        }
    }

    /**
     * Get the categories and all its novels from the database.
     */
    private fun getLibraryFlow(): Flow<NovelLibraryMap> {
        val libraryNovelsFlow = combine(
            getLibraryNovel.subscribe(),
            getLibraryItemPreferencesFlow(),
        ) { libraryNovelList, prefs ->
            libraryNovelList
                .map { libraryNovel ->
                    // Display mode based on user preference: take it from global library setting or category
                    NovelLibraryItem(
                        libraryNovel,
                        unreadCount = if (prefs.unreadBadge) libraryNovel.unreadCount else 0,
                        sourceLanguage = if (prefs.languageBadge) {
                            sourceManager.getOrStub(libraryNovel.novel.source).getNameForNovelInfo()
                        } else {
                            ""
                        },
                    )
                }
                .groupBy { it.libraryNovel.category }
        }

        return combine(getCategories.subscribe(), libraryNovelsFlow) { categories, libraryNovel ->
            val displayCategories = if (libraryNovel.isNotEmpty() && !libraryNovel.containsKey(0)) {
                categories.fastFilterNot { it.isSystemCategory }
            } else {
                categories
            }

            displayCategories.associateWith { libraryNovel[it.id].orEmpty() }
        }
    }

    /**
     * Returns the common categories for the given list of novels.
     *
     * @param novels the list of novel.
     */
    private suspend fun getCommonCategories(novels: List<Novel>): Collection<Category> {
        if (novels.isEmpty()) return emptyList()
        return novels
            .map { getNovelCategories.await(it.id).toSet() }
            .reduce { set1, set2 -> set1.intersect(set2) }
    }

    /**
     * Returns the mix (non-common) categories for the given list of novels.
     *
     * @param novels the list of novel.
     */
    private suspend fun getMixCategories(novels: List<Novel>): Collection<Category> {
        if (novels.isEmpty()) return emptyList()
        val novelCategories = novels.map { getNovelCategories.await(it.id).toSet() }
        val common = novelCategories.reduce { set1, set2 -> set1.intersect(set2) }
        return novelCategories.flatten().distinct().subtract(common)
    }

    /**
     * Marks novels' chapters read status.
     */
    fun markReadSelection(read: Boolean) {
        val novels = state.value.selection.toList()
        screenModelScope.launchNonCancellable {
            novels.forEach { novel ->
                setReadStatus.await(
                    novel = novel.novel,
                    read = read,
                )
            }
        }
        clearSelection()
    }

    /**
     * Remove the selected novel.
     *
     * @param novelList the list of novel to delete.
     * @param deleteFromLibrary whether to delete novel from library.
     */
    fun removeNovels(novelList: List<Novel>, deleteFromLibrary: Boolean) {
        screenModelScope.launchNonCancellable {
            val novelToDelete = novelList.distinctBy { it.id }

            if (deleteFromLibrary) {
                updateNovel.awaitAll(
                    novelToDelete.map {
                        NovelUpdate(
                            favorite = false,
                            id = it.id,
                        )
                    },
                )
            }
        }
    }

    /**
     * Bulk update categories of novels using old and new common categories.
     *
     * @param novelList the list of novel to move.
     * @param addCategories the categories to add for all novels.
     * @param removeCategories the categories to remove in all novels.
     */
    fun setNovelCategories(
        novelList: List<Novel>,
        addCategories: List<Long>,
        removeCategories: List<Long>,
    ) {
        screenModelScope.launchNonCancellable {
            novelList.forEach { novel ->
                val categoryIds = getNovelCategories.await(novel.id)
                    .map { it.id }
                    .subtract(removeCategories.toSet())
                    .plus(addCategories)
                    .toList()

                setNovelCategories.await(novel.id, categoryIds)
            }
        }
    }

    fun getDisplayMode(): PreferenceMutableState<LibraryDisplayMode> {
        return libraryPreferences.displayMode().asState(screenModelScope)
    }

    fun getColumnsPreferenceForCurrentOrientation(isLandscape: Boolean): PreferenceMutableState<Int> {
        return (
            if (isLandscape) {
                libraryPreferences.mangaLandscapeColumns()
            } else {
                libraryPreferences.mangaPortraitColumns()
            }
            ).asState(
            screenModelScope,
        )
    }
    suspend fun getRandomLibraryItemForCurrentCategory(): NovelLibraryItem? {
        if (state.value.categories.isEmpty()) return null

        return withIOContext {
            state.value
                .getLibraryItemsByCategoryId(state.value.categories[activeCategoryIndex].id)
                ?.randomOrNull()
        }
    }

    suspend fun getNextUnreadChapter(novel: Novel): NovelChapter? {
        return getChaptersByNovelId.await(novel.id).firstOrNull { !it.read }
    }

    fun showSettingsDialog() {
        mutableState.update { it.copy(dialog = Dialog.SettingsSheet) }
    }

    fun clearSelection() {
        mutableState.update { it.copy(selection = persistentListOf()) }
    }

    fun toggleSelection(novel: LibraryNovel) {
        mutableState.update { state ->
            val newSelection = state.selection.mutate { list ->
                if (list.fastAny { it.id == novel.id }) {
                    list.removeAll { it.id == novel.id }
                } else {
                    list.add(novel)
                }
            }
            state.copy(selection = newSelection)
        }
    }

    /**
     * Selects all novels between and including the given novel and the last pressed novel from the
     * same category as the given novel
     */
    fun toggleRangeSelection(novel: LibraryNovel) {
        mutableState.update { state ->
            val newSelection = state.selection.mutate { list ->
                val lastSelected = list.lastOrNull()
                if (lastSelected?.category != novel.category) {
                    list.add(novel)
                    return@mutate
                }

                val items = state.getLibraryItemsByCategoryId(novel.category)
                    ?.fastMap { it.libraryNovel }.orEmpty()
                val lastNovelIndex = items.indexOf(lastSelected)
                val curNovelIndex = items.indexOf(novel)

                val selectedIds = list.fastMap { it.id }
                val selectionRange = when {
                    lastNovelIndex < curNovelIndex -> IntRange(lastNovelIndex, curNovelIndex)
                    curNovelIndex < lastNovelIndex -> IntRange(curNovelIndex, lastNovelIndex)
                    // We shouldn't reach this point
                    else -> return@mutate
                }
                val newSelections = selectionRange.mapNotNull { index ->
                    items[index].takeUnless { it.id in selectedIds }
                }
                list.addAll(newSelections)
            }
            state.copy(selection = newSelection)
        }
    }

    fun selectAll(index: Int) {
        mutableState.update { state ->
            val newSelection = state.selection.mutate { list ->
                val categoryId = state.categories.getOrNull(index)?.id ?: -1
                val selectedIds = list.fastMap { it.id }
                state.getLibraryItemsByCategoryId(categoryId)
                    ?.fastMapNotNull { item ->
                        item.libraryNovel.takeUnless { it.id in selectedIds }
                    }
                    ?.let { list.addAll(it) }
            }
            state.copy(selection = newSelection)
        }
    }

    fun invertSelection(index: Int) {
        mutableState.update { state ->
            val newSelection = state.selection.mutate { list ->
                val categoryId = state.categories[index].id
                val items = state.getLibraryItemsByCategoryId(categoryId)?.fastMap { it.libraryNovel }.orEmpty()
                val selectedIds = list.fastMap { it.id }
                val (toRemove, toAdd) = items.fastPartition { it.id in selectedIds }
                val toRemoveIds = toRemove.fastMap { it.id }
                list.removeAll { it.id in toRemoveIds }
                list.addAll(toAdd)
            }
            state.copy(selection = newSelection)
        }
    }

    fun search(query: String?) {
        mutableState.update { it.copy(searchQuery = query) }
    }

    fun openChangeCategoryDialog() {
        screenModelScope.launchIO {
            // Create a copy of selected novel
            val novelList = state.value.selection.map { it.novel }

            // Hide the default category because it has a different behavior than the ones from db.
            val categories = state.value.categories.filter { it.id != 0L }

            // Get indexes of the common categories to preselect.
            val common = getCommonCategories(novelList)
            // Get indexes of the mix categories to preselect.
            val mix = getMixCategories(novelList)
            val preselected = categories
                .map {
                    when (it) {
                        in common -> CheckboxState.State.Checked(it)
                        in mix -> CheckboxState.TriState.Exclude(it)
                        else -> CheckboxState.State.None(it)
                    }
                }
                .toImmutableList()
            mutableState.update { it.copy(dialog = Dialog.ChangeCategory(novelList, preselected)) }
        }
    }

    fun openDeleteNovelDialog() {
        val novelList = state.value.selection.map { it.novel }
        mutableState.update { it.copy(dialog = Dialog.DeleteNovel(novelList)) }
    }

    fun closeDialog() {
        mutableState.update { it.copy(dialog = null) }
    }

    sealed interface Dialog {
        data object SettingsSheet : Dialog
        data class ChangeCategory(
            val novel: List<Novel>,
            val initialSelection: ImmutableList<CheckboxState<Category>>,
        ) : Dialog
        data class DeleteNovel(val novel: List<Novel>) : Dialog
    }

    @Immutable
    private data class ItemPreferences(
        val unreadBadge: Boolean,
        val languageBadge: Boolean,
        val filterUnread: TriState,
        val filterStarted: TriState,
        val filterBookmarked: TriState,
        val filterCompleted: TriState,
    )

    @Immutable
    data class State(
        val isLoading: Boolean = true,
        val library: NovelLibraryMap = emptyMap(),
        val searchQuery: String? = null,
        val selection: PersistentList<LibraryNovel> = persistentListOf(),
        val hasActiveFilters: Boolean = false,
        val showCategoryTabs: Boolean = false,
        val showNovelCount: Boolean = false,
        val showNovelContinueButton: Boolean = false,
        val dialog: Dialog? = null,
    ) {
        private val libraryCount by lazy {
            library.values
                .flatten()
                .fastDistinctBy { it.libraryNovel.novel.id }
                .size
        }

        val isLibraryEmpty by lazy { libraryCount == 0 }

        val selectionMode = selection.isNotEmpty()

        val categories = library.keys.toList()

        fun getLibraryItemsByCategoryId(categoryId: Long): List<NovelLibraryItem>? {
            return library.firstNotNullOfOrNull { (k, v) -> v.takeIf { k.id == categoryId } }
        }

        fun getLibraryItemsByPage(page: Int): List<NovelLibraryItem> {
            return library.values.toTypedArray().getOrNull(page).orEmpty()
        }

        fun getNovelCountForCategory(category: Category): Int? {
            return if (showNovelCount || !searchQuery.isNullOrEmpty()) library[category]?.size else null
        }

        fun getToolbarTitle(
            defaultTitle: String,
            defaultCategoryTitle: String,
            page: Int,
        ): LibraryToolbarTitle {
            val category = categories.getOrNull(page) ?: return LibraryToolbarTitle(defaultTitle)
            val categoryName = category.let {
                if (it.isSystemCategory) defaultCategoryTitle else it.name
            }
            val title = if (showCategoryTabs) defaultTitle else categoryName
            val count = when {
                !showNovelCount -> null
                !showCategoryTabs -> getNovelCountForCategory(category)
                // Whole library count
                else -> libraryCount
            }

            return LibraryToolbarTitle(title, count)
        }
    }
}
