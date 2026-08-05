package eu.kanade.tachiyomi.ui.browse.novel.source.browse

import androidx.compose.runtime.Composable
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.ui.browse.manga.source.browse.SourceFilterMangaDialog

/**
 * Filters for novels use the same [FilterList] model as manga (see
 * `eu.kanade.tachiyomi.source.model.Filter`), so the generic manga filter sheet is reused.
 */
@Composable
fun SourceFilterNovelDialog(
    onDismissRequest: () -> Unit,
    filters: FilterList,
    onReset: () -> Unit,
    onFilter: () -> Unit,
    onUpdate: (FilterList) -> Unit,
) {
    SourceFilterMangaDialog(
        onDismissRequest = onDismissRequest,
        filters = filters,
        onReset = onReset,
        onFilter = onFilter,
        onUpdate = onUpdate,
    )
}
