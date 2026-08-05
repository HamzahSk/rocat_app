package tachiyomi.domain.items.novelchapter.interactor

import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.items.novelchapter.model.NovelChapter
import tachiyomi.domain.items.novelchapter.repository.NovelChapterRepository

class GetNovelChapter(
    private val chapterRepository: NovelChapterRepository,
) {

    suspend fun await(id: Long): NovelChapter? {
        return try {
            chapterRepository.getChapterById(id)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            null
        }
    }
}
