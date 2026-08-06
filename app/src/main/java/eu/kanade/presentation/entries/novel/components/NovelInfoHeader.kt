package eu.kanade.presentation.entries.novel.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import eu.kanade.presentation.entries.components.DotSeparatorText
import eu.kanade.presentation.entries.components.ItemCover
import eu.kanade.tachiyomi.novelsource.model.SNovel
import tachiyomi.domain.entries.novel.model.Novel
import tachiyomi.domain.entries.novel.model.asNovelCover
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.DISABLED_ALPHA
import tachiyomi.presentation.core.components.material.TextButton
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.clickableNoIndication
import tachiyomi.presentation.core.util.secondaryItemAlpha

private val whitespaceLineRegex = Regex("[\\r\\n]{2,}", setOf(RegexOption.MULTILINE))

/**
 * Header of the novel detail screen. Renders a blurred cover backdrop together with the book cover,
 * title, author, status and source name, mirroring [eu.kanade.presentation.entries.manga.components.MangaInfoBox].
 */
@Composable
fun NovelInfoBox(
    appBarPadding: Dp,
    novel: Novel,
    sourceName: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        val backdropGradientColors = listOf(
            Color.Transparent,
            MaterialTheme.colorScheme.background,
        )
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(novel.asNovelCover())
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .matchParentSize()
                .drawWithContent {
                    drawContent()
                    drawRect(brush = Brush.verticalGradient(colors = backdropGradientColors))
                }
                .blur(4.dp)
                .alpha(0.2f),
        )

        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = appBarPadding + 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ItemCover.Book(
                    modifier = Modifier
                        .sizeIn(maxWidth = 100.dp)
                        .align(Alignment.Top),
                    data = novel.asNovelCover(),
                    contentDescription = novel.title,
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    NovelContentInfo(
                        title = novel.title,
                        author = novel.author,
                        status = novel.status,
                        sourceName = sourceName,
                    )
                }
            }
        }
    }
}

/**
 * Action row of the novel detail screen. Mirrors [eu.kanade.presentation.entries.manga.components.MangaActionRow]
 * with the two novel specific actions: add/remove from library and continue reading.
 */
@Composable
fun NovelActionRow(
    favorite: Boolean,
    hasChapters: Boolean,
    onAddToLibraryClicked: () -> Unit,
    onContinueReading: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val defaultActionButtonColor = MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_ALPHA)

    Row(modifier = modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp)) {
        NovelActionButton(
            title = if (favorite) {
                stringResource(MR.strings.remove_from_library)
            } else {
                stringResource(MR.strings.add_to_library)
            },
            icon = if (favorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            color = if (favorite) MaterialTheme.colorScheme.primary else defaultActionButtonColor,
            onClick = onAddToLibraryClicked,
        )
        if (hasChapters) {
            NovelActionButton(
                title = stringResource(MR.strings.action_resume),
                icon = Icons.Filled.PlayArrow,
                color = MaterialTheme.colorScheme.primary,
                onClick = onContinueReading,
            )
        }
    }
}

/**
 * Expandable description of the novel together with its genre chips. Mirrors the visual layout of
 * [eu.kanade.presentation.entries.manga.components.ExpandableMangaDescription].
 */
@Composable
fun ExpandableNovelDescription(
    defaultExpandState: Boolean,
    description: String?,
    genre: List<String>?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        var expanded by rememberSaveable { mutableStateOf(defaultExpandState) }
        val desc = description.takeIf { !it.isNullOrBlank() }
            ?: stringResource(MR.strings.description_placeholder)
        val trimmedDescription = remember(desc) {
            desc
                .replace(whitespaceLineRegex, "\n")
                .trimEnd()
        }

        Text(
            text = if (expanded) desc else trimmedDescription,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .padding(horizontal = 16.dp)
                .clickableNoIndication { expanded = !expanded },
            maxLines = if (expanded) Int.MAX_VALUE else 4,
            overflow = TextOverflow.Ellipsis,
        )

        val genres = genre.orEmpty()
        if (genres.isNotEmpty()) {
            FlowRow(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .padding(horizontal = 16.dp)
                    .animateContentSize(animationSpec = spring())
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
            ) {
                genres.forEach { tag ->
                    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                        SuggestionChip(
                            modifier = Modifier.padding(vertical = 4.dp),
                            onClick = {},
                            label = { Text(text = tag, style = MaterialTheme.typography.bodySmall) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.NovelContentInfo(
    title: String,
    author: String?,
    status: Long,
    sourceName: String,
    textAlign: TextAlign? = LocalTextStyle.current.textAlign,
) {
    Text(
        text = title.ifBlank { stringResource(MR.strings.unknown_title) },
        style = MaterialTheme.typography.titleLarge,
        textAlign = textAlign,
    )

    Spacer(modifier = Modifier.height(2.dp))

    Row(
        modifier = Modifier.secondaryItemAlpha(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.PersonOutline,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = author?.takeIf { it.isNotBlank() }
                ?: stringResource(MR.strings.unknown_author),
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = textAlign,
        )
    }

    Spacer(modifier = Modifier.height(2.dp))

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
        ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
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
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
            DotSeparatorText()
            Text(
                text = sourceName,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun RowScope.NovelActionButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.weight(1f),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = title,
                color = color,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}
