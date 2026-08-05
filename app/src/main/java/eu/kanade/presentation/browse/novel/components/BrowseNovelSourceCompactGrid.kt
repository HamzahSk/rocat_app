package eu.kanade.presentation.browse.novel.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import eu.kanade.presentation.browse.BrowseSourceLoadingItem
import eu.kanade.presentation.browse.InLibraryBadge
import eu.kanade.presentation.library.components.CommonEntryItemDefaults
import eu.kanade.presentation.library.components.EntryCompactGridItem
import kotlinx.coroutines.flow.StateFlow
import tachiyomi.domain.entries.novel.model.Novel
import tachiyomi.domain.entries.novel.model.asNovelCover
import tachiyomi.presentation.core.util.plus

@Composable
fun BrowseNovelSourceCompactGrid(
    novelList: LazyPagingItems<StateFlow<Novel>>,
    columns: GridCells,
    contentPadding: PaddingValues,
    onNovelClick: (Novel) -> Unit,
) {
    LazyVerticalGrid(
        columns = columns,
        contentPadding = contentPadding + PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(CommonEntryItemDefaults.GridVerticalSpacer),
        horizontalArrangement = Arrangement.spacedBy(CommonEntryItemDefaults.GridHorizontalSpacer),
    ) {
        if (novelList.loadState.prepend is LoadState.Loading) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                BrowseSourceLoadingItem()
            }
        }

        items(count = novelList.itemCount) { index ->
            val novel by novelList[index]?.collectAsState() ?: return@items
            BrowseNovelSourceCompactGridItem(
                novel = novel,
                onClick = { onNovelClick(novel) },
            )
        }

        if (novelList.loadState.refresh is LoadState.Loading || novelList.loadState.append is LoadState.Loading) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                BrowseSourceLoadingItem()
            }
        }
    }
}

@Composable
private fun BrowseNovelSourceCompactGridItem(
    novel: Novel,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = onClick,
) {
    EntryCompactGridItem(
        title = novel.title,
        coverData = novel.asNovelCover(),
        coverAlpha = if (novel.favorite) CommonEntryItemDefaults.BrowseFavoriteCoverAlpha else 1f,
        coverBadgeStart = {
            InLibraryBadge(enabled = novel.favorite)
        },
        onClick = onClick,
        onLongClick = onLongClick,
    )
}
