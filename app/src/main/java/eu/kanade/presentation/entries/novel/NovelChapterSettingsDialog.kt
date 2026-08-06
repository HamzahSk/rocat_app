package eu.kanade.presentation.entries.novel

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.kanade.presentation.components.TabbedDialogPaddings
import eu.kanade.tachiyomi.ui.entries.novel.NovelScreenModel
import tachiyomi.core.common.preference.TriState
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.RadioItem
import tachiyomi.presentation.core.components.TriStateItem
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Settings dialog for the chapter list of a novel. Offers a filter for unread chapters and a
 * display mode (source title vs chapter number), mirroring the chapter settings of manga.
 */
@Composable
fun NovelChapterSettingsDialog(
    onDismissRequest: () -> Unit,
    unreadFilter: TriState,
    onUnreadFilterChanged: (TriState) -> Unit,
    displayMode: Long,
    onDisplayModeChanged: (Long) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = stringResource(MR.strings.action_filter)) },
        text = {
            Column(
                modifier = Modifier.padding(vertical = TabbedDialogPaddings.Vertical),
            ) {
                TriStateItem(
                    label = stringResource(MR.strings.action_filter_unread),
                    state = unreadFilter,
                    onClick = onUnreadFilterChanged,
                )
                Text(
                    text = stringResource(MR.strings.action_display_mode),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = MaterialTheme.padding.medium),
                )
                RadioItem(
                    label = stringResource(MR.strings.show_title),
                    selected = displayMode == NovelScreenModel.DISPLAY_NAME,
                    onClick = { onDisplayModeChanged(NovelScreenModel.DISPLAY_NAME) },
                )
                RadioItem(
                    label = stringResource(MR.strings.show_chapter_number),
                    selected = displayMode == NovelScreenModel.DISPLAY_NUMBER,
                    onClick = { onDisplayModeChanged(NovelScreenModel.DISPLAY_NUMBER) },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(MR.strings.action_close))
            }
        },
    )
}
