package tachiyomi.domain.items.novelchapter.interactor

import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.items.novelchapter.model.NovelChapterUpdate
import tachiyomi.domain.items.novelchapter.repository.NovelChapterRepository

class UpdateNovelChapter(
    private val chapterRepository: NovelChapterRepository,
) {

    suspend fun await(chapterUpdate: NovelChapterUpdate) {
        try {
            chapterRepository.updateChapter(chapterUpdate)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
        }
    }

    suspend fun awaitAll(chapterUpdates: List<NovelChapterUpdate>) {
        try {
            chapterRepository.updateAllChapters(chapterUpdates)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
        }
    }
}
