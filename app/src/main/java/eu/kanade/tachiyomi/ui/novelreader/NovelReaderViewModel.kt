package eu.kanade.tachiyomi.ui.novelreader

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import eu.kanade.domain.items.novelchapter.interactor.SetNovelReadStatus
import eu.kanade.tachiyomi.novelsource.model.SNovelChapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.history.novel.model.NovelHistoryUpdate
import tachiyomi.domain.history.novel.repository.NovelHistoryRepository
import tachiyomi.domain.items.novelchapter.interactor.GetNovelChapterByUrlAndNovelId
import tachiyomi.domain.items.novelchapter.interactor.UpdateNovelChapter
import tachiyomi.domain.items.novelchapter.model.NovelChapterUpdate
import tachiyomi.domain.source.novel.service.NovelSourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Date
import kotlin.math.abs

/**
 * Loads a novel chapter's text from its [NovelSource] and keeps the local database in sync:
 * it marks the chapter read, upserts a history entry and persists the scroll position.
 */
class NovelReaderViewModel(
    private val sourceId: Long,
    private val novelId: Long,
    private val chapterUrl: String,
    private val chapterName: String,
    private val sourceManager: NovelSourceManager = Injekt.get(),
    private val getNovelChapterByUrlAndNovelId: GetNovelChapterByUrlAndNovelId = Injekt.get(),
    private val setNovelReadStatus: SetNovelReadStatus = Injekt.get(),
    private val updateNovelChapter: UpdateNovelChapter = Injekt.get(),
    private val historyRepository: NovelHistoryRepository = Injekt.get(),
) : ViewModel() {

    var uiState by mutableStateOf(NovelReaderUiState())
        private set

    private val sessionStart = System.currentTimeMillis()

    private var dbChapterId: Long? = null

    private var latestScrollIndex = 0L

    private var lastPersistedIndex = 0L

    /**
     * Loads the chapter text and marks it read once it is available.
     */
    fun loadChapter() {
        uiState = uiState.copy(isLoading = true, error = null)
        viewModelScope.launch {
            val source = sourceManager.get(sourceId)
            if (source == null) {
                uiState = uiState.copy(isLoading = false, error = "Source is not available")
                return@launch
            }

            val chapter = SNovelChapter.create().apply {
                url = chapterUrl
                name = chapterName
            }
            try {
                val html = withContext(Dispatchers.Default) { source.getChapterText(chapter) }
                val blocks = withContext(Dispatchers.Default) { ChapterTextExtractor.extract(html) }
                uiState = uiState.copy(isLoading = false, blocks = blocks)
                markChapterRead()
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e)
                uiState = uiState.copy(isLoading = false, error = e.message ?: "Failed to load chapter")
            }
        }
    }

    /**
     * Persists the reading position based on the first visible item of the reader list.
     * Writes are throttled by [SCROLL_PERSIST_THRESHOLD] to avoid hitting the database on
     * every scroll event.
     */
    fun onScrolled(firstVisibleItemIndex: Int) {
        if (novelId <= 0L) return
        val index = firstVisibleItemIndex.toLong()
        latestScrollIndex = index
        if (abs(index - lastPersistedIndex) < SCROLL_PERSIST_THRESHOLD) return
        persistPosition(index)
    }

    override fun onCleared() {
        super.onCleared()
        // Make sure the final reading position survives, even if the user exits quickly.
        if (novelId <= 0L) return
        val index = latestScrollIndex
        viewModelScope.launch(NonCancellable) {
            persistPositionInternal(index)
        }
    }

    private fun persistPosition(index: Long) {
        lastPersistedIndex = index
        viewModelScope.launch {
            persistPositionInternal(index)
        }
    }

    private suspend fun persistPositionInternal(index: Long) {
        val chapterId = getChapterIdOrNull() ?: return
        updateNovelChapter.await(
            NovelChapterUpdate(
                lastPageRead = index,
                id = chapterId,
            ),
        )
    }

    private suspend fun getChapterIdOrNull(): Long? {
        dbChapterId?.let { return it }
        if (novelId <= 0L) return null
        val chapterId = getNovelChapterByUrlAndNovelId.await(chapterUrl, novelId)?.id ?: return null
        dbChapterId = chapterId
        return chapterId
    }

    private fun markChapterRead() {
        if (novelId <= 0L) return
        viewModelScope.launch {
            val chapter = getNovelChapterByUrlAndNovelId.await(chapterUrl, novelId) ?: return@launch
            if (!chapter.read) {
                setNovelReadStatus.await(read = true, chapter)
            }
            historyRepository.upsertNovelHistory(
                NovelHistoryUpdate(
                    chapterId = chapter.id,
                    readAt = Date(),
                    sessionReadDuration = System.currentTimeMillis() - sessionStart,
                ),
            )
        }
    }

    companion object {
        private const val SCROLL_PERSIST_THRESHOLD = 5L
    }
}

data class NovelReaderUiState(
    val blocks: List<NovelChapterContent>? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

class NovelReaderViewModelFactory(
    private val sourceId: Long,
    private val novelId: Long,
    private val chapterUrl: String,
    private val chapterName: String,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        return NovelReaderViewModel(
            sourceId = sourceId,
            novelId = novelId,
            chapterUrl = chapterUrl,
            chapterName = chapterName,
        ) as T
    }
}
