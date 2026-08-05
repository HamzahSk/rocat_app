package tachiyomi.data.searchhistory

import kotlinx.coroutines.flow.Flow
import tachiyomi.data.handlers.anime.AnimeDatabaseHandler
import tachiyomi.data.handlers.manga.MangaDatabaseHandler
import tachiyomi.domain.searchhistory.model.SearchHistory
import tachiyomi.domain.searchhistory.model.SearchSourceType
import tachiyomi.domain.searchhistory.repository.SearchHistoryRepository

class SearchHistoryRepositoryImpl(
    private val mangaHandler: MangaDatabaseHandler,
    private val animeHandler: AnimeDatabaseHandler,
) : SearchHistoryRepository {

    override fun getSearchHistoryBySource(
        sourceId: Long,
        sourceType: SearchSourceType,
    ): Flow<List<SearchHistory>> {
        return when (sourceType) {
            SearchSourceType.MANGA, SearchSourceType.NOVEL -> mangaHandler.subscribeToList {
                searchHistoryQueries.getSearchHistoryBySource(
                    sourceId,
                    SearchHistoryRepository.MAX_SEARCH_HISTORY,
                    SearchHistoryMapper::mapSearchHistory,
                )
            }

            SearchSourceType.ANIME -> animeHandler.subscribeToList {
                searchHistoryQueries.getSearchHistoryBySource(
                    sourceId,
                    SearchHistoryRepository.MAX_SEARCH_HISTORY,
                    SearchHistoryMapper::mapSearchHistory,
                )
            }
        }
    }

    override suspend fun insertSearchQuery(
        sourceId: Long,
        query: String,
        sourceType: SearchSourceType,
        now: Long,
    ) {
        when (sourceType) {
            SearchSourceType.MANGA, SearchSourceType.NOVEL -> mangaHandler.await(inTransaction = true) {
                searchHistoryQueries.insertSearchQuery(sourceId, query, now)
                searchHistoryQueries.deleteOldestExcess(sourceId, SearchHistoryRepository.MAX_SEARCH_HISTORY)
            }

            SearchSourceType.ANIME -> animeHandler.await(inTransaction = true) {
                searchHistoryQueries.insertSearchQuery(sourceId, query, now)
                searchHistoryQueries.deleteOldestExcess(sourceId, SearchHistoryRepository.MAX_SEARCH_HISTORY)
            }
        }
    }

    override suspend fun deleteSearchQuery(
        id: Long,
        sourceType: SearchSourceType,
    ) {
        when (sourceType) {
            SearchSourceType.MANGA, SearchSourceType.NOVEL -> mangaHandler.await {
                searchHistoryQueries.deleteSearchQuery(id)
            }

            SearchSourceType.ANIME -> animeHandler.await {
                searchHistoryQueries.deleteSearchQuery(id)
            }
        }
    }

    override suspend fun clearSearchHistoryBySource(
        sourceId: Long,
        sourceType: SearchSourceType,
    ) {
        when (sourceType) {
            SearchSourceType.MANGA, SearchSourceType.NOVEL -> mangaHandler.await {
                searchHistoryQueries.clearSearchHistoryBySource(sourceId)
            }

            SearchSourceType.ANIME -> animeHandler.await {
                searchHistoryQueries.clearSearchHistoryBySource(sourceId)
            }
        }
    }

    override suspend fun clearAllSearchHistory() {
        mangaHandler.await { searchHistoryQueries.clearAllSearchHistory() }
        animeHandler.await { searchHistoryQueries.clearAllSearchHistory() }
    }
}
