package eu.kanade.domain.items.novelchapter.interactor

import logcat.LogPriority
import tachiyomi.core.common.util.lang.withNonCancellableContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entries.novel.model.Novel
import tachiyomi.domain.items.novelchapter.model.NovelChapter
import tachiyomi.domain.items.novelchapter.model.NovelChapterUpdate
import tachiyomi.domain.items.novelchapter.repository.NovelChapterRepository

class SetNovelReadStatus(
    private val chapterRepository: NovelChapterRepository,
) {

    private val mapper = { chapterId: Long, read: Boolean ->
        NovelChapterUpdate(
            read = read,
            lastPageRead = if (!read) 0 else null,
            id = chapterId,
        )
    }

    suspend fun await(
        read: Boolean,
        vararg chapters: NovelChapter,
    ): Result = withNonCancellableContext {
        val chaptersToUpdate = chapters.filter {
            when (read) {
                true -> !it.read
                false -> it.read || it.lastPageRead > 0
            }
        }
        if (chaptersToUpdate.isEmpty()) {
            return@withNonCancellableContext Result.NoChapters
        }

        try {
            chapterRepository.updateAllChapters(
                chaptersToUpdate.map { mapper(it.id, read) },
            )
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            return@withNonCancellableContext Result.InternalError(e)
        }

        Result.Success
    }

    suspend fun await(novelId: Long, read: Boolean): Result = withNonCancellableContext {
        await(
            read = read,
            chapters = chapterRepository
                .getChapterByNovelId(novelId, applyScanlatorFilter = false)
                .toTypedArray(),
        )
    }

    suspend fun await(novel: Novel, read: Boolean) =
        await(novel.id, read)

    sealed interface Result {
        data object Success : Result
        data object NoChapters : Result
        data class InternalError(val error: Throwable) : Result
    }
}
