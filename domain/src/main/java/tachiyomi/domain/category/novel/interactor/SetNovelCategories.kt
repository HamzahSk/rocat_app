package tachiyomi.domain.category.novel.interactor

import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entries.novel.repository.NovelRepository

class SetNovelCategories(
    private val novelRepository: NovelRepository,
) {

    suspend fun await(novelId: Long, categoryIds: List<Long>) {
        try {
            novelRepository.setNovelCategories(novelId, categoryIds)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
        }
    }
}
