package tachiyomi.data.entries.novel

import kotlinx.coroutines.flow.Flow
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.data.MangaUpdateStrategyColumnAdapter
import tachiyomi.data.StringListColumnAdapter
import tachiyomi.data.handlers.manga.MangaDatabaseHandler
import tachiyomi.domain.entries.novel.model.Novel
import tachiyomi.domain.entries.novel.model.NovelUpdate
import tachiyomi.domain.entries.novel.repository.NovelRepository
import tachiyomi.domain.library.novel.LibraryNovel
import java.time.LocalDate
import java.time.ZoneId

class NovelRepositoryImpl(
    private val handler: MangaDatabaseHandler,
) : NovelRepository {

    override suspend fun getNovelById(id: Long): Novel {
        return handler.awaitOne { novelsQueries.getNovelById(id, NovelMapper::mapNovel) }
    }

    override suspend fun getNovelByIdAsFlow(id: Long): Flow<Novel> {
        return handler.subscribeToOne { novelsQueries.getNovelById(id, NovelMapper::mapNovel) }
    }

    override suspend fun getNovelByUrlAndSourceId(url: String, sourceId: Long): Novel? {
        return handler.awaitOneOrNull {
            novelsQueries.getNovelByUrlAndSource(
                url,
                sourceId,
                NovelMapper::mapNovel,
            )
        }
    }

    override fun getNovelByUrlAndSourceIdAsFlow(url: String, sourceId: Long): Flow<Novel?> {
        return handler.subscribeToOneOrNull {
            novelsQueries.getNovelByUrlAndSource(
                url,
                sourceId,
                NovelMapper::mapNovel,
            )
        }
    }

    override suspend fun getNovelFavorites(): List<Novel> {
        return handler.awaitList { novelsQueries.getFavorites(NovelMapper::mapNovel) }
    }

    override suspend fun getReadNovelsNotInLibrary(): List<Novel> {
        return handler.awaitList { novelsQueries.getReadNovelsNotInLibrary(NovelMapper::mapNovel) }
    }

    override suspend fun getLibraryNovels(): List<LibraryNovel> {
        return handler.awaitList { novellibraryViewQueries.novellibrary(NovelMapper::mapLibraryNovel) }
    }

    override fun getLibraryNovelsAsFlow(): Flow<List<LibraryNovel>> {
        return handler.subscribeToList { novellibraryViewQueries.novellibrary(NovelMapper::mapLibraryNovel) }
    }

    override fun getNovelFavoritesBySourceId(sourceId: Long): Flow<List<Novel>> {
        return handler.subscribeToList { novelsQueries.getFavoriteBySourceId(sourceId, NovelMapper::mapNovel) }
    }

    override suspend fun getDuplicateLibraryNovel(id: Long, title: String): List<Novel> {
        return handler.awaitList {
            novelsQueries.getDuplicateLibraryNovel(title, id, NovelMapper::mapNovel)
        }
    }

    override suspend fun getUpcomingNovels(statuses: Set<Long>): Flow<List<Novel>> {
        val epochMillis = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000
        return handler.subscribeToList {
            novelsQueries.getUpcomingNovels(epochMillis, statuses, NovelMapper::mapNovel)
        }
    }

    override suspend fun resetNovelViewerFlags(): Boolean {
        return try {
            handler.await { novelsQueries.resetViewerFlags() }
            true
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            false
        }
    }

    override suspend fun setNovelCategories(novelId: Long, categoryIds: List<Long>) {
        handler.await(inTransaction = true) {
            novels_categoriesQueries.deleteNovelCategoryByNovelId(novelId)
            categoryIds.map { categoryId ->
                novels_categoriesQueries.insert(novelId, categoryId)
            }
        }
    }

    override suspend fun insertNovel(novel: Novel): Long? {
        return handler.awaitOneOrNullExecutable(inTransaction = true) {
            novelsQueries.insert(
                source = novel.source,
                url = novel.url,
                artist = novel.artist,
                author = novel.author,
                description = novel.description,
                genre = novel.genre,
                title = novel.title,
                status = novel.status,
                thumbnailUrl = novel.thumbnailUrl,
                favorite = novel.favorite,
                lastUpdate = novel.lastUpdate,
                nextUpdate = novel.nextUpdate,
                calculateInterval = novel.fetchInterval.toLong(),
                initialized = novel.initialized,
                viewerFlags = novel.viewerFlags,
                chapterFlags = novel.chapterFlags,
                coverLastModified = novel.coverLastModified,
                dateAdded = novel.dateAdded,
                updateStrategy = novel.updateStrategy,
                version = novel.version,
            )
            novelsQueries.selectLastInsertedRowId()
        }
    }

    override suspend fun updateNovel(update: NovelUpdate): Boolean {
        return try {
            partialUpdateNovel(update)
            true
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            false
        }
    }

    override suspend fun updateAllNovels(novelUpdates: List<NovelUpdate>): Boolean {
        return try {
            partialUpdateNovel(*novelUpdates.toTypedArray())
            true
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            false
        }
    }

    private suspend fun partialUpdateNovel(vararg novelUpdates: NovelUpdate) {
        handler.await(inTransaction = true) {
            novelUpdates.forEach { value ->
                novelsQueries.update(
                    source = value.source,
                    url = value.url,
                    artist = value.artist,
                    author = value.author,
                    description = value.description,
                    genre = value.genre?.let(StringListColumnAdapter::encode),
                    title = value.title,
                    status = value.status,
                    thumbnailUrl = value.thumbnailUrl,
                    favorite = value.favorite,
                    lastUpdate = value.lastUpdate,
                    nextUpdate = value.nextUpdate,
                    calculateInterval = value.fetchInterval?.toLong(),
                    initialized = value.initialized,
                    viewer = value.viewerFlags,
                    chapterFlags = value.chapterFlags,
                    coverLastModified = value.coverLastModified,
                    dateAdded = value.dateAdded,
                    novelId = value.id,
                    updateStrategy = value.updateStrategy?.let(MangaUpdateStrategyColumnAdapter::encode),
                    version = value.version,
                    isSyncing = 0,
                )
            }
        }
    }
}
