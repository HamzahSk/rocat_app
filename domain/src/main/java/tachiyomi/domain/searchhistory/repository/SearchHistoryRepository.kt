package tachiyomi.domain.searchhistory.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.searchhistory.model.SearchHistory
import tachiyomi.domain.searchhistory.model.SearchSourceType

interface SearchHistoryRepository {

    fun getSearchHistoryBySource(
        sourceId: Long,
        sourceType: SearchSourceType,
    ): Flow<List<SearchHistory>>

    suspend fun insertSearchQuery(
        sourceId: Long,
        query: String,
        sourceType: SearchSourceType,
        now: Long = System.currentTimeMillis(),
    )

    suspend fun deleteSearchQuery(
        id: Long,
        sourceType: SearchSourceType,
    )

    suspend fun clearSearchHistoryBySource(
        sourceId: Long,
        sourceType: SearchSourceType,
    )

    suspend fun clearAllSearchHistory()

    companion object {
        const val MAX_SEARCH_HISTORY = 8L
    }
}
