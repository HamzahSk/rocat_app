package eu.kanade.presentation.browse.novel.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import eu.kanade.presentation.browse.BrowseSourceLoadingItem
import eu.kanade.presentation.browse.InLibraryBadge
import eu.kanade.presentation.library.components.CommonEntryItemDefaults
import eu.kanade.presentation.library.components.EntryListItem
import kotlinx.coroutines.flow.StateFlow
import tachiyomi.domain.entries.novel.model.Novel
import tachiyomi.domain.entries.novel.model.asNovelCover
import tachiyomi.presentation.core.util.plus

@Composable
fun BrowseNovelSourceList(
    novelList: LazyPagingItems<StateFlow<Novel>>,
    entries: Int,
    topBarHeight: Int,
    contentPadding: PaddingValues,
    onNovelClick: (Novel) -> Unit,
) {
    val sourceListState = rememberLazyListState()
    BoxWithConstraints {
        val density = LocalDensity.current
        val containerHeightPx = with(density) { this@BoxWithConstraints.maxHeight.roundToPx() }

        LazyColumn(
            state = sourceListState,
            contentPadding = contentPadding + PaddingValues(vertical = 8.dp),
        ) {
            item {
                if (novelList.loadState.prepend is LoadState.Loading) {
                    BrowseSourceLoadingItem()
                }
            }

            items(count = novelList.itemCount) { index ->
                val novel by novelList[index]?.collectAsState() ?: return@items
                BrowseNovelSourceListItem(
                    novel = novel,
                    onClick = { onNovelClick(novel) },
                    entries = entries,
                    containerHeight = containerHeightPx - topBarHeight,
                )
            }

            item {
                if (novelList.loadState.refresh is LoadState.Loading ||
                    novelList.loadState.append is LoadState.Loading
                ) {
                    BrowseSourceLoadingItem()
                }
            }
        }
    }
}

@Composable
private fun BrowseNovelSourceListItem(
    novel: Novel,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = onClick,
    entries: Int,
    containerHeight: Int,
) {
    EntryListItem(
        title = novel.title,
        coverData = novel.asNovelCover(),
        coverAlpha = if (novel.favorite) CommonEntryItemDefaults.BrowseFavoriteCoverAlpha else 1f,
        badge = {
            InLibraryBadge(enabled = novel.favorite)
        },
        onClick = onClick,
        onLongClick = onLongClick,
        entries = entries,
        containerHeight = containerHeight,
    )
}
