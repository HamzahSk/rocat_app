package tachiyomi.data.history.novel

import kotlinx.coroutines.flow.Flow
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.data.handlers.manga.MangaDatabaseHandler
import tachiyomi.domain.history.novel.model.NovelHistory
import tachiyomi.domain.history.novel.model.NovelHistoryUpdate
import tachiyomi.domain.history.novel.model.NovelHistoryWithRelations
import tachiyomi.domain.history.novel.repository.NovelHistoryRepository

class NovelHistoryRepositoryImpl(
    private val handler: MangaDatabaseHandler,
) : NovelHistoryRepository {

    override fun getNovelHistory(query: String): Flow<List<NovelHistoryWithRelations>> {
        return handler.subscribeToList {
            novelhistoryViewQueries.novelhistory(query, NovelHistoryMapper::mapNovelHistoryWithRelations)
        }
    }

    override suspend fun getLastNovelHistory(): NovelHistoryWithRelations? {
        return handler.awaitOneOrNull {
            novelhistoryViewQueries.getLatestNovelHistory(NovelHistoryMapper::mapNovelHistoryWithRelations)
        }
    }

    override suspend fun getTotalReadDuration(): Long {
        return handler.awaitOne { novelhistoryQueries.getReadDuration() }
    }

    override suspend fun getHistoryByNovelId(novelId: Long): List<NovelHistory> {
        return handler.awaitList {
            novelhistoryQueries.getHistoryByNovelId(novelId, NovelHistoryMapper::mapNovelHistory)
        }
    }

    override suspend fun resetNovelHistory(historyId: Long) {
        try {
            handler.await { novelhistoryQueries.resetHistoryById(historyId) }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, throwable = e)
        }
    }

    override suspend fun resetHistoryByNovelId(novelId: Long) {
        try {
            handler.await { novelhistoryQueries.resetHistoryByNovelId(novelId) }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, throwable = e)
        }
    }

    override suspend fun deleteAllNovelHistory(): Boolean {
        return try {
            handler.await { novelhistoryQueries.removeAllHistory() }
            true
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, throwable = e)
            false
        }
    }

    override suspend fun upsertNovelHistory(historyUpdate: NovelHistoryUpdate) {
        try {
            handler.await {
                novelhistoryQueries.upsert(
                    historyUpdate.chapterId,
                    historyUpdate.readAt,
                    historyUpdate.sessionReadDuration,
                )
            }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, throwable = e)
        }
    }
}
