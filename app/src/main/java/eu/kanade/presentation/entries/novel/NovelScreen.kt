package eu.kanade.presentation.entries.novel

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.relativeDateTimeText
import eu.kanade.presentation.entries.components.ItemCover
import eu.kanade.tachiyomi.novelsource.model.SNovel
import eu.kanade.tachiyomi.ui.entries.novel.NovelScreenModel
import tachiyomi.domain.entries.novel.model.asNovelCover
import tachiyomi.domain.items.novelchapter.model.NovelChapter
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.secondaryItemAlpha

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
) {
    Scaffold(
        topBar = { scrollBehavior ->
            AppBar(
                title = state.novel.title,
                navigateUp = navigateUp,
                actions = {
                    if (state.isRefreshingData) {
                        Box(
                            modifier = Modifier.padding(end = MaterialTheme.padding.small),
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                            )
                        }
                    } else {
                        IconButton(onClick = onRefresh) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = null,
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { paddingValues ->
        NovelScreenContent(
            state = state,
            modifier = Modifier.padding(paddingValues),
            onChapterClicked = onChapterClicked,
            onChapterReadClicked = onChapterReadClicked,
            onAddToLibraryClicked = onAddToLibraryClicked,
            onContinueReading = onContinueReading,
        )
    }
}

@Composable
private fun NovelScreenContent(
    state: NovelScreenModel.State.Success,
    onChapterClicked: (NovelChapter) -> Unit,
    onChapterReadClicked: (NovelChapter, Boolean) -> Unit,
    onAddToLibraryClicked: () -> Unit,
    onContinueReading: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxWidth()) {
        item {
            NovelInfoHeader(
                state = state,
                onAddToLibraryClicked = onAddToLibraryClicked,
                onContinueReading = onContinueReading,
            )
        }
        item {
            Text(
                text = stringResource(MR.strings.chapters),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(
                    start = MaterialTheme.padding.medium,
                    end = MaterialTheme.padding.medium,
                    top = MaterialTheme.padding.medium,
                    bottom = MaterialTheme.padding.small,
                ),
            )
        }
        if (state.chapters.isEmpty()) {
            item {
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
        items(state.chapters, key = { it.id }) { chapter ->
            NovelChapterListItem(
                chapter = chapter,
                onClick = { onChapterClicked(chapter) },
                onReadClicked = { read -> onChapterReadClicked(chapter, read) },
            )
        }
    }
}

@Composable
private fun NovelInfoHeader(
    state: NovelScreenModel.State.Success,
    onAddToLibraryClicked: () -> Unit,
    onContinueReading: () -> Unit,
) {
    val novel = state.novel
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.padding.medium),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
    ) {
        Spacer(Modifier.height(MaterialTheme.padding.small))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium),
        ) {
            ItemCover.Book(
                data = novel.asNovelCover(),
                contentDescription = novel.title,
                modifier = Modifier.width(112.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
            ) {
                Text(
                    text = novel.title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                novel.author?.takeIf { it.isNotBlank() }?.let { author ->
                    Text(
                        text = author,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                NovelStatusLabel(status = novel.status)
            }
        }
        novel.genre?.takeIf { it.isNotEmpty() }?.let { genres ->
            Text(
                text = genres.joinToString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        novel.description?.takeIf { it.isNotBlank() }?.let { description ->
            ExpandableNovelDescription(description = description)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
        ) {
            FilledTonalButton(
                onClick = onAddToLibraryClicked,
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    imageVector = if (novel.favorite) {
                        Icons.Outlined.Favorite
                    } else {
                        Icons.Outlined.FavoriteBorder
                    },
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(MaterialTheme.padding.small))
                Text(
                    text = if (novel.favorite) {
                        stringResource(MR.strings.remove_from_library)
                    } else {
                        stringResource(MR.strings.add_to_library)
                    },
                )
            }
        }
        val isReading = state.chapters.fastAny { it.read }
        if (state.chapters.isNotEmpty()) {
            FilledTonalButton(
                onClick = onContinueReading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Outlined.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(MaterialTheme.padding.small))
                Text(
                    text = stringResource(if (isReading) MR.strings.action_resume else MR.strings.action_start),
                )
            }
        }
    }
}

@Composable
private fun NovelStatusLabel(status: Long) {
    Row(
        modifier = Modifier.secondaryItemAlpha(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = when (status) {
                SNovel.ONGOING.toLong() -> Icons.Outlined.Schedule
                SNovel.COMPLETED.toLong() -> Icons.Outlined.DoneAll
                SNovel.LICENSED.toLong() -> Icons.Outlined.Done
                SNovel.PUBLISHING_FINISHED.toLong() -> Icons.Outlined.Done
                SNovel.CANCELLED.toLong() -> Icons.Outlined.Close
                SNovel.ON_HIATUS.toLong() -> Icons.Outlined.Pause
                else -> Icons.Outlined.Block
            },
            contentDescription = null,
            modifier = Modifier
                .padding(end = 4.dp)
                .size(16.dp),
        )
        Text(
            text = when (status) {
                SNovel.ONGOING.toLong() -> stringResource(MR.strings.ongoing)
                SNovel.COMPLETED.toLong() -> stringResource(MR.strings.completed)
                SNovel.LICENSED.toLong() -> stringResource(MR.strings.licensed)
                SNovel.PUBLISHING_FINISHED.toLong() -> stringResource(MR.strings.publishing_finished)
                SNovel.CANCELLED.toLong() -> stringResource(MR.strings.cancelled)
                SNovel.ON_HIATUS.toLong() -> stringResource(MR.strings.on_hiatus)
                else -> stringResource(MR.strings.unknown)
            },
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ExpandableNovelDescription(description: String) {
    var expanded by remember { mutableStateOf(false) }

    Text(
        text = description,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(vertical = MaterialTheme.padding.small),
        maxLines = if (expanded) Int.MAX_VALUE else 4,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun NovelChapterListItem(
    chapter: NovelChapter,
    onClick: () -> Unit,
    onReadClicked: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = MaterialTheme.padding.medium,
                vertical = MaterialTheme.padding.small,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { onReadClicked(!chapter.read) }) {
            Icon(
                imageVector = if (chapter.read) {
                    Icons.Outlined.CheckCircle
                } else {
                    Icons.Outlined.RadioButtonUnchecked
                },
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (chapter.read) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = chapter.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (chapter.dateUpload > 0L) {
                Text(
                    text = relativeDateTimeText(chapter.dateUpload),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        )
    }
    HorizontalDivider()
}
