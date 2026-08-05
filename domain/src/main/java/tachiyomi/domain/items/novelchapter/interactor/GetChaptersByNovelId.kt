package tachiyomi.domain.items.novelchapter.interactor

import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.items.novelchapter.model.NovelChapter
import tachiyomi.domain.items.novelchapter.repository.NovelChapterRepository

class GetChaptersByNovelId(
    private val chapterRepository: NovelChapterRepository,
) {

    suspend fun await(novelId: Long): List<NovelChapter> {
        return try {
            chapterRepository.getChapterByNovelId(novelId, applyScanlatorFilter = false)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            emptyList()
        }
    }
}
