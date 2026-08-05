package eu.kanade.tachiyomi.ui.entries.novel

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.core.util.ifNovelSourcesLoaded
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.novelreader.NovelReaderActivity
import tachiyomi.domain.items.novelchapter.model.NovelChapter
import tachiyomi.presentation.core.screens.LoadingScreen
import eu.kanade.presentation.entries.novel.NovelScreen as NovelScreenContent

class NovelScreen(
    private val novelId: Long,
    val fromSource: Boolean = false,
) : Screen() {

    @Composable
    override fun Content() {
        if (!ifNovelSourcesLoaded()) {
            LoadingScreen()
            return
        }

        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        val screenModel =
            rememberScreenModel { NovelScreenModel(context, lifecycleOwner.lifecycle, novelId, fromSource) }

        val state by screenModel.state.collectAsStateWithLifecycle()

        if (state is NovelScreenModel.State.Loading) {
            LoadingScreen()
            return
        }

        val successState = state as NovelScreenModel.State.Success

        NovelScreenContent(
            state = successState,
            snackbarHostState = screenModel.snackbarHostState,
            navigateUp = navigator::pop,
            onChapterClicked = { chapter -> openChapter(context, screenModel.source?.id, chapter) },
            onChapterReadClicked = screenModel::markChapterRead,
            onAddToLibraryClicked = screenModel::toggleFavorite,
            onRefresh = screenModel::fetchAllFromSource,
            onContinueReading = {
                continueReading(context, screenModel.source?.id, screenModel.getNextUnreadChapter())
            },
        )
    }

    private fun continueReading(context: Context, sourceId: Long?, unreadChapter: NovelChapter?) {
        if (unreadChapter != null) openChapter(context, sourceId, unreadChapter)
    }

    private fun openChapter(context: Context, sourceId: Long?, chapter: NovelChapter) {
        val source = sourceId ?: return
        context.startActivity(
            NovelReaderActivity.newIntent(
                context = context,
                sourceId = source,
                novelId = chapter.novelId,
                chapterUrl = chapter.url,
                chapterName = chapter.name,
            ),
        )
    }
}
