package eu.kanade.presentation.entries.novel

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import eu.kanade.presentation.entries.EntryScreenItem
import eu.kanade.presentation.entries.components.EntryToolbar
import eu.kanade.presentation.entries.components.ItemHeader
import eu.kanade.presentation.entries.novel.components.ExpandableNovelDescription
import eu.kanade.presentation.entries.novel.components.NovelActionRow
import eu.kanade.presentation.entries.novel.components.NovelChapterListItem
import eu.kanade.presentation.entries.novel.components.NovelInfoBox
import eu.kanade.presentation.util.formatChapterNumber
import eu.kanade.tachiyomi.source.novel.getNameForNovelInfo
import eu.kanade.tachiyomi.ui.entries.novel.NovelScreenModel
import tachiyomi.core.common.preference.TriState
import tachiyomi.domain.items.novelchapter.model.NovelChapter
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.VerticalFastScroller
import tachiyomi.presentation.core.components.material.PullRefresh
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Detail screen of a novel. Mirrors the layout of the manga/anime entry screens: a collapsing
 * [EntryToolbar], the [NovelInfoBox] header (blurred cover backdrop + book cover), a [NovelActionRow],
 * an expandable description and the chapter list under an [ItemHeader].
 */
@Composable
fun NovelScreen(
    state: NovelScreenModel.State.Success,
    snackbarHostState: SnackbarHostState,
    navigateUp: () -> Unit,
    onChapterClicked: (NovelChapter) -> Unit,
    onChapterReadClicked: (NovelChapter, Boolean) -> Unit,
    onAddToLibraryClicked: () -> Unit,
    onRefresh: () -> Unit,
    onContinueReading: () -> Unit,
    onFilterClicked: () -> Unit,
    onUnreadFilterChanged: (TriState) -> Unit,
    onDisplayModeChanged: (Long) -> Unit,
    onDismissFilterDialog: () -> Unit,
) {
    val chapterListState = rememberLazyListState()

    val isFirstItemVisible by remember {
        derivedStateOf { chapterListState.firstVisibleItemIndex == 0 }
    }
    val isFirstItemScrolled by remember {
        derivedStateOf { chapterListState.firstVisibleItemScrollOffset > 0 }
    }
    val titleAlpha by animateFloatAsState(
        targetValue = if (!isFirstItemVisible) 1f else 0f,
        label = "Top Bar Title",
    )
    val backgroundAlpha by animateFloatAsState(
        targetValue = if (!isFirstItemVisible || isFirstItemScrolled) 1f else 0f,
        label = "Top Bar Background",
    )

    Scaffold(
        topBar = {
            EntryToolbar(
                title = state.novel.title,
                hasFilters = state.filterActive,
                navigateUp = navigateUp,
                onClickFilter = onFilterClicked,
                onClickShare = null,
                onClickDownload = null,
                onClickEditCategory = null,
                onClickRefresh = onRefresh,
                onClickMigrate = null,
                onClickSettings = null,
                changeAnimeSkipIntro = null,
                actionModeCounter = 0,
                onCancelActionMode = {},
                onSelectAll = {},
                onInvertSelection = {},
                titleAlphaProvider = { titleAlpha },
                backgroundAlphaProvider = { backgroundAlpha },
                isManga = true,
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { contentPadding ->
        val topPadding = contentPadding.calculateTopPadding()

        PullRefresh(
            refreshing = state.isRefreshingData,
            enabled = true,
            onRefresh = onRefresh,
            indicatorPadding = PaddingValues(top = topPadding),
        ) {
            val layoutDirection = LocalLayoutDirection.current
            VerticalFastScroller(
                listState = chapterListState,
                topContentPadding = topPadding,
                endContentPadding = contentPadding.calculateEndPadding(layoutDirection),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxHeight(),
                    state = chapterListState,
                    contentPadding = PaddingValues(
                        start = contentPadding.calculateStartPadding(layoutDirection),
                        end = contentPadding.calculateEndPadding(layoutDirection),
                        bottom = contentPadding.calculateBottomPadding(),
                    ),
                ) {
                    item(
                        key = EntryScreenItem.INFO_BOX,
                        contentType = EntryScreenItem.INFO_BOX,
                    ) {
                        NovelInfoBox(
                            appBarPadding = topPadding,
                            novel = state.novel,
                            sourceName = remember { state.source.getNameForNovelInfo() },
                        )
                    }

                    item(
                        key = EntryScreenItem.ACTION_ROW,
                        contentType = EntryScreenItem.ACTION_ROW,
                    ) {
                        NovelActionRow(
                            favorite = state.novel.favorite,
                            hasChapters = state.chapters.isNotEmpty(),
                            onAddToLibraryClicked = onAddToLibraryClicked,
                            onContinueReading = onContinueReading,
                        )
                    }

                    item(
                        key = EntryScreenItem.DESCRIPTION_WITH_TAG,
                        contentType = EntryScreenItem.DESCRIPTION_WITH_TAG,
                    ) {
                        ExpandableNovelDescription(
                            defaultExpandState = state.isFromSource,
                            description = state.novel.description,
                            genre = state.novel.genre,
                        )
                    }

                    item(
                        key = EntryScreenItem.ITEM_HEADER,
                        contentType = EntryScreenItem.ITEM_HEADER,
                    ) {
                        ItemHeader(
                            enabled = true,
                            itemCount = state.chapters.size,
                            missingItemsCount = 0,
                            onClick = onFilterClicked,
                            isManga = true,
                        )
                    }

                    if (state.processedChapters.isEmpty()) {
                        item(key = "no-chapters") {
                            Text(
                                text = stringResource(MR.strings.no_chapters_error),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(
                                    horizontal = MaterialTheme.padding.medium,
                                    vertical = MaterialTheme.padding.medium,
                                ),
                            )
                        }
                    }

                    items(
                        items = state.processedChapters,
                        key = { it.id },
                    ) { chapter ->
                        NovelChapterListItem(
                            chapter = chapter,
                            title = if (state.displayMode == NovelScreenModel.DISPLAY_NUMBER &&
                                chapter.isRecognizedNumber
                            ) {
                                stringResource(
                                    MR.strings.display_mode_chapter,
                                    formatChapterNumber(chapter.chapterNumber),
                                )
                            } else {
                                chapter.name
                            },
                            onClick = { onChapterClicked(chapter) },
                            onReadClicked = { read -> onChapterReadClicked(chapter, read) },
                        )
                    }
                }
            }
        }
    }

    if (state.showFilterDialog) {
        NovelChapterSettingsDialog(
            onDismissRequest = onDismissFilterDialog,
            unreadFilter = state.unreadFilter,
            onUnreadFilterChanged = onUnreadFilterChanged,
            displayMode = state.displayMode,
            onDisplayModeChanged = onDisplayModeChanged,
        )
    }
}
