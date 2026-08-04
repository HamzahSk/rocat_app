package tachiyomi.domain.searchhistory.model

data class SearchHistory(
    val id: Long,
    val sourceId: Long,
    val searchQuery: String,
    val createdAt: Long,
)
