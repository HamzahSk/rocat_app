package eu.kanade.presentation.library.novel

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.util.fastAny
import eu.kanade.presentation.library.components.EntryComfortableGridItem
import eu.kanade.presentation.library.components.LanguageBadge
import eu.kanade.presentation.library.components.LazyLibraryGrid
import eu.kanade.presentation.library.components.UnviewedBadge
import eu.kanade.presentation.library.components.globalSearchItem
import eu.kanade.tachiyomi.ui.library.novel.NovelLibraryItem
import tachiyomi.domain.entries.novel.model.asNovelCover
import tachiyomi.domain.library.novel.LibraryNovel

@Composable
internal fun NovelLibraryComfortableGrid(
    items: List<NovelLibraryItem>,
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
            contentType = { "novel_library_comfortable_grid_item" },
        ) { libraryItem ->
            val novel = libraryItem.libraryNovel.novel
            EntryComfortableGridItem(
                isSelected = selection.fastAny { it.id == libraryItem.libraryNovel.id },
                title = novel.title,
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
