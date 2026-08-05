package eu.kanade.presentation.library.novel

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.util.fastAny
import eu.kanade.presentation.library.components.EntryCompactGridItem
import eu.kanade.presentation.library.components.LanguageBadge
import eu.kanade.presentation.library.components.LazyLibraryGrid
import eu.kanade.presentation.library.components.UnviewedBadge
import eu.kanade.presentation.library.components.globalSearchItem
import eu.kanade.tachiyomi.ui.library.novel.NovelLibraryItem
import tachiyomi.domain.entries.novel.model.asNovelCover
import tachiyomi.domain.library.novel.LibraryNovel

@Composable
internal fun NovelLibraryCompactGrid(
    items: List<NovelLibraryItem>,
    showTitle: Boolean,
    columns: Int,
    contentPadding: PaddingValues,
    selection: List<LibraryNovel>,
    onClick: (LibraryNovel) -> Unit,
    onLongClick: (LibraryNovel) -> Unit,
    onClickContinueReading: ((LibraryNovel) -> Unit)?,
    searchQuery: String?,
    onGlobalSearchClicked: () -> Unit,
) {
    LazyLibraryGrid(
        modifier = Modifier.fillMaxSize(),
        columns = columns,
        contentPadding = contentPadding,
    ) {
        globalSearchItem(searchQuery, onGlobalSearchClicked)

        items(
            items = items,
            contentType = { "novel_library_compact_grid_item" },
        ) { libraryItem ->
            val novel = libraryItem.libraryNovel.novel
            EntryCompactGridItem(
                isSelected = selection.fastAny { it.id == libraryItem.libraryNovel.id },
                title = novel.title.takeIf { showTitle },
                coverData = novel.asNovelCover(),
                coverBadgeStart = {
                    UnviewedBadge(count = libraryItem.unreadCount)
                },
                coverBadgeEnd = {
                    LanguageBadge(
                        isLocal = false,
                        sourceLanguage = libraryItem.sourceLanguage,
                    )
                },
                onLongClick = { onLongClick(libraryItem.libraryNovel) },
                onClick = { onClick(libraryItem.libraryNovel) },
                onClickContinueViewing = if (onClickContinueReading != null && libraryItem.unreadCount > 0) {
                    { onClickContinueReading(libraryItem.libraryNovel) }
                } else {
                    null
                },
            )
        }
    }
}
