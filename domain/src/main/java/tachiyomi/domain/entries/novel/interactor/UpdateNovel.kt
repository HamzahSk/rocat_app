package tachiyomi.domain.entries.novel.interactor

import tachiyomi.domain.entries.novel.model.Novel
import tachiyomi.domain.entries.novel.model.NovelUpdate
import tachiyomi.domain.entries.novel.repository.NovelRepository
import java.time.Instant

class UpdateNovel(
    private val novelRepository: NovelRepository,
    private val novelFetchInterval: NovelFetchInterval,
) {

    suspend fun await(novelUpdate: NovelUpdate): Boolean {
        return novelRepository.updateNovel(novelUpdate)
    }

    suspend fun awaitAll(novelUpdates: List<NovelUpdate>): Boolean {
        return novelRepository.updateAllNovels(novelUpdates)
    }

    suspend fun awaitUpdateFetchInterval(
        novel: Novel,
        dateTime: java.time.ZonedDateTime = java.time.ZonedDateTime.now(),
        window: Pair<Long, Long> = novelFetchInterval.getWindow(dateTime),
    ): Boolean {
        return novelRepository.updateNovel(
            novelFetchInterval.toNovelUpdate(novel, dateTime, window),
        )
    }

    suspend fun awaitUpdateLastUpdate(novelId: Long): Boolean {
        return novelRepository.updateNovel(NovelUpdate(id = novelId, lastUpdate = Instant.now().toEpochMilli()))
    }

    suspend fun awaitUpdateFavorite(novelId: Long, favorite: Boolean): Boolean {
        val dateAdded = when (favorite) {
            true -> Instant.now().toEpochMilli()
            false -> 0
        }
        return novelRepository.updateNovel(
            NovelUpdate(id = novelId, favorite = favorite, dateAdded = dateAdded),
        )
    }
}
