package tachiyomi.data.searchhistory

import tachiyomi.domain.searchhistory.model.SearchHistory

object SearchHistoryMapper {
    fun mapSearchHistory(
        id: Long,
        sourceId: Long,
        searchQuery: String,
        createdAt: Long,
    ): SearchHistory = SearchHistory(
        id = id,
        sourceId = sourceId,
        searchQuery = searchQuery,
        createdAt = createdAt,
    )
}
