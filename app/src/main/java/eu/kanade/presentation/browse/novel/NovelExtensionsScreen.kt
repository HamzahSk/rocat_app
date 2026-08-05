package eu.kanade.presentation.browse.novel

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.browse.BaseBrowseItem
import eu.kanade.presentation.browse.manga.ExtensionHeader
import eu.kanade.presentation.browse.manga.ExtensionTrustDialog
import eu.kanade.presentation.browse.novel.components.NovelExtensionIcon
import eu.kanade.presentation.util.animateItemFastScroll
import eu.kanade.tachiyomi.extension.novel.model.NovelExtension
import eu.kanade.tachiyomi.ui.browse.novel.extension.NovelExtensionUiModel
import eu.kanade.tachiyomi.ui.browse.novel.extension.NovelExtensionsScreenModel
import eu.kanade.tachiyomi.util.system.LocaleHelper
import kotlinx.collections.immutable.persistentListOf
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.FastScrollLazyColumn
import tachiyomi.presentation.core.components.material.PullRefresh
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.components.material.topSmallPaddingValues
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen
import tachiyomi.presentation.core.util.plus
import tachiyomi.presentation.core.util.secondaryItemAlpha

@Composable
fun NovelExtensionsScreen(
    state: NovelExtensionsScreenModel.State,
    contentPadding: PaddingValues,
    searchQuery: String?,
    onLongClickItem: (NovelExtension) -> Unit,
    onUninstallExtension: (NovelExtension) -> Unit,
    onTrustExtension: (NovelExtension.Untrusted) -> Unit,
    onRefresh: () -> Unit,
) {
    PullRefresh(
        refreshing = state.isRefreshing,
        onRefresh = onRefresh,
        enabled = !state.isLoading,
    ) {
        when {
            state.isLoading -> LoadingScreen(Modifier.padding(contentPadding))
            state.isEmpty -> {
                val msg = if (!searchQuery.isNullOrEmpty()) {
                    MR.strings.no_results_found
                } else {
                    MR.strings.empty_screen
                }
                EmptyScreen(
                    stringRes = msg,
                    modifier = Modifier.padding(contentPadding),
                    actions = persistentListOf(),
                )
            }
            else -> {
                val context = LocalContext.current
                var trustState by remember { mutableStateOf<NovelExtension.Untrusted?>(null) }

                FastScrollLazyColumn(
                    contentPadding = contentPadding + topSmallPaddingValues,
                ) {
                    state.items.forEach { (header, items) ->
                        item(
                            contentType = "header",
                            key = "extensionHeader-${header.hashCode()}",
                        ) {
                            when (header) {
                                is NovelExtensionUiModel.Header.Resource -> {
                                    ExtensionHeader(
                                        textRes = header.textRes,
                                        modifier = Modifier.animateItemFastScroll(),
                                    )
                                }
                                is NovelExtensionUiModel.Header.Text -> {
                                    ExtensionHeader(
                                        text = header.text,
                                        modifier = Modifier.animateItemFastScroll(),
                                    )
                                }
                            }
                        }

                        items(
                            items = items,
                            contentType = { "item" },
                            key = { "extension-${it.pkgName}-${it.hashCode()}" },
                        ) { extension ->
                            NovelExtensionItem(
                                modifier = Modifier.animateItemFastScroll(),
                                extension = extension,
                                onClickItem = {
                                    when (it) {
                                        is NovelExtension.Installed -> {}
                                        is NovelExtension.Untrusted -> {
                                            trustState = it
                                        }
                                        is NovelExtension.Available -> {}
                                    }
                                },
                                onLongClickItem = onLongClickItem,
                                onClickItemAction = {
                                    when (it) {
                                        is NovelExtension.Installed -> onUninstallExtension(it)
                                        is NovelExtension.Untrusted -> {
                                            trustState = it
                                        }
                                        is NovelExtension.Available -> {}
                                    }
                                },
                            )
                        }
                    }
                }

                if (trustState != null) {
                    ExtensionTrustDialog(
                        onClickConfirm = {
                            onTrustExtension(trustState!!)
                            trustState = null
                        },
                        onClickDismiss = {
                            onUninstallExtension(trustState!!)
                            trustState = null
                        },
                        onDismissRequest = {
                            trustState = null
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun NovelExtensionItem(
    extension: NovelExtension,
    modifier: Modifier = Modifier,
    onClickItem: (NovelExtension) -> Unit = {},
    onLongClickItem: (NovelExtension) -> Unit = {},
    onClickItemAction: (NovelExtension) -> Unit = {},
) {
    BaseBrowseItem(
        modifier = modifier.combinedClickable(
            onClick = { onClickItem(extension) },
            onLongClick = { onLongClickItem(extension) },
        ),
        onClickItem = { onClickItem(extension) },
        onLongClickItem = { onLongClickItem(extension) },
        icon = {
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center,
            ) {
                NovelExtensionIcon(
                    extension = extension,
                    modifier = Modifier.matchParentSize(),
                )
            }
        },
        action = {
            when (extension) {
                is NovelExtension.Installed -> {
                    IconButton(onClick = { onClickItemAction(extension) }) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = stringResource(MR.strings.ext_uninstall),
                        )
                    }
                }
                is NovelExtension.Untrusted -> {
                    IconButton(onClick = { onClickItemAction(extension) }) {
                        Icon(
                            imageVector = Icons.Outlined.VerifiedUser,
                            contentDescription = stringResource(MR.strings.ext_trust),
                        )
                    }
                }
                is NovelExtension.Available -> {}
            }
        },
    ) {
        NovelExtensionItemContent(
            extension = extension,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun NovelExtensionItemContent(
    extension: NovelExtension,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(start = MaterialTheme.padding.medium),
    ) {
        Text(
            text = extension.name,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
        )
        Row(
            modifier = Modifier.secondaryItemAlpha(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
        ) {
            ProvideTextStyle(value = MaterialTheme.typography.bodySmall) {
                if (extension.lang?.isNotEmpty() == true) {
                    Text(
                        text = LocaleHelper.getSourceDisplayName(
                            extension.lang,
                            LocalContext.current,
                        ),
                    )
                }

                if (extension.versionName.isNotEmpty()) {
                    Text(
                        text = extension.versionName,
                    )
                }

                if (extension is NovelExtension.Untrusted) {
                    Text(
                        text = stringResource(MR.strings.ext_untrusted).uppercase(),
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
