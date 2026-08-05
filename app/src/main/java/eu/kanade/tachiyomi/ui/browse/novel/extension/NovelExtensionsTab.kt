package eu.kanade.tachiyomi.ui.browse.novel.extension

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.screen.Screen
import eu.kanade.presentation.browse.novel.NovelExtensionsScreen
import eu.kanade.presentation.components.TabContent
import tachiyomi.i18n.aniyomi.AYMR

@Composable
fun Screen.novelExtensionsTab(
    screenModel: NovelExtensionsScreenModel,
): TabContent {
    val state by screenModel.state.collectAsState()

    return TabContent(
        titleRes = AYMR.strings.label_novel_extensions,
        searchEnabled = true,
        content = { contentPadding, _ ->
            NovelExtensionsScreen(
                state = state,
                contentPadding = contentPadding,
                searchQuery = state.searchQuery,
                onLongClickItem = { },
                onUninstallExtension = { extension ->
                    screenModel.uninstallExtension(extension)
                },
                onTrustExtension = screenModel::trustExtension,
                onRefresh = screenModel::refresh,
            )
        },
    )
}
