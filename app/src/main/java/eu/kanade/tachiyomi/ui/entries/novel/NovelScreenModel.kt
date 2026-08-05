package eu.kanade.tachiyomi.ui.entries.novel

import android.content.Context
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.domain.entries.novel.interactor.SyncNovelChaptersWithSource
import eu.kanade.domain.entries.novel.model.copyFrom
import eu.kanade.domain.entries.novel.model.toSNovel
import eu.kanade.domain.items.novelchapter.interactor.SetNovelReadStatus
import eu.kanade.presentation.util.formattedMessage
import eu.kanade.tachiyomi.novelsource.NovelSource
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import logcat.LogPriority
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entries.novel.interactor.GetNovelWithChapters
import tachiyomi.domain.entries.novel.interactor.UpdateNovel
import tachiyomi.domain.entries.novel.model.Novel
import tachiyomi.domain.entries.novel.model.toNovelUpdate
import tachiyomi.domain.entries.novel.repository.NovelRepository
import tachiyomi.domain.items.novelchapter.model.NoChaptersException
import tachiyomi.domain.items.novelchapter.model.NovelChapter
import tachiyomi.domain.source.novel.service.NovelSourceManager
import tachiyomi.i18n.MR
import tachiyomi.source.local.entries.novel.isLocalNovel
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Screen model for the novel detail page. Loads a [Novel] together with its chapters, fetches
 * missing details/chapters from the source when opening from a source list and exposes actions
 * to add/remove the novel from the library and toggle the read status of chapters.
 */
class NovelScreenModel(
    private val context: Context,
    private val lifecycle: Lifecycle,
    private val novelId: Long,
    private val isFromSource: Boolean,
    private val getNovelWithChapters: GetNovelWithChapters = Injekt.get(),
    private val updateNovel: UpdateNovel = Injekt.get(),
    private val syncNovelChaptersWithSource: SyncNovelChaptersWithSource = Injekt.get(),
    private val setNovelReadStatus: SetNovelReadStatus = Injekt.get(),
    private val novelRepository: NovelRepository = Injekt.get(),
    private val sourceManager: NovelSourceManager = Injekt.get(),
    val snackbarHostState: SnackbarHostState = SnackbarHostState(),
) : StateScreenModel<NovelScreenModel.State>(State.Loading) {

    private val successState: State.Success?
        get() = state.value as? State.Success

    val novel: Novel?
        get() = successState?.novel

    val source: NovelSource?
        get() = successState?.source

    private val isFavorited: Boolean
        get() = novel?.favorite ?: false

    private inline fun updateSuccessState(func: (State.Success) -> State.Success) {
        mutableState.update {
            when (it) {
                State.Loading -> it
                is State.Success -> func(it)
            }
        }
    }

    init {
        screenModelScope.launchIO {
            getNovelWithChapters.subscribe(novelId)
                .flowWithLifecycle(lifecycle)
                .collectLatest { (novel, chapters) ->
                    updateSuccessState { it.copy(novel = novel, chapters = chapters) }
                }
        }

        screenModelScope.launchIO {
            val novel = getNovelWithChapters.awaitNovel(novelId)
            val chapters = getNovelWithChapters.awaitChapters(novelId)
            val source = sourceManager.getOrStub(novel.source)

            mutableState.update {
                State.Success(
                    novel = novel,
                    source = source,
                    isFromSource = isFromSource,
                    chapters = chapters,
                    isRefreshingData = false,
                )
            }

            if (screenModelScope.isActive) {
                val needRefreshInfo = !novel.initialized || isFromSource
                val needRefreshChapter = chapters.isEmpty() && !novel.isLocalNovel()

                if (needRefreshInfo) {
                    fetchNovelFromSource()
                }
                if (needRefreshChapter) {
                    fetchChaptersFromSource()
                }
            }
        }
    }

    fun fetchAllFromSource() {
        screenModelScope.launch {
            updateSuccessState { it.copy(isRefreshingData = true) }
            fetchNovelFromSource()
            fetchChaptersFromSource()
            updateSuccessState { it.copy(isRefreshingData = false) }
        }
    }

    private suspend fun fetchNovelFromSource() {
        val state = successState ?: return
        try {
            withIOContext {
                val networkNovel = state.source.getNovelDetails(state.novel.toSNovel())
                val updatedNovel = state.novel.copyFrom(networkNovel).copy(initialized = true)
                updateNovel.await(updatedNovel.toNovelUpdate())
            }
        } catch (e: Throwable) {
            logcat(LogPriority.ERROR, e)
            screenModelScope.launch {
                snackbarHostState.showSnackbar(message = with(context) { e.formattedMessage })
            }
        }
    }

    private suspend fun fetchChaptersFromSource() {
        val state = successState ?: return
        try {
            withIOContext {
                val chapters = state.source.getChapterList(state.novel.toSNovel())
                syncNovelChaptersWithSource.await(
                    chapters,
                    state.novel,
                    state.source,
                )
            }
        } catch (e: Throwable) {
            val message = if (e is NoChaptersException) {
                context.stringResource(MR.strings.no_chapters_error)
            } else {
                logcat(LogPriority.ERROR, e)
                with(context) { e.formattedMessage }
            }

            screenModelScope.launch {
                snackbarHostState.showSnackbar(message = message)
            }
            val newNovel = novelRepository.getNovelById(novelId)
            updateSuccessState { it.copy(novel = newNovel, isRefreshingData = false) }
        }
    }

    fun toggleFavorite() {
        screenModelScope.launchIO {
            updateNovel.awaitUpdateFavorite(novelId, !isFavorited)
        }
    }

    fun markChapterRead(chapter: NovelChapter, read: Boolean) {
        screenModelScope.launchIO {
            setNovelReadStatus.await(read = read, chapter)
        }
    }

    fun getNextUnreadChapter(): NovelChapter? {
        return successState?.chapters?.firstOrNull { !it.read }
    }

    sealed interface State {
        @Immutable
        data object Loading : State

        @Immutable
        data class Success(
            val novel: Novel,
            val source: NovelSource,
            val isFromSource: Boolean,
            val chapters: List<NovelChapter>,
            val isRefreshingData: Boolean = false,
        ) : State {
            val unreadCount: Int
                get() = chapters.count { !it.read }
        }
    }
}
