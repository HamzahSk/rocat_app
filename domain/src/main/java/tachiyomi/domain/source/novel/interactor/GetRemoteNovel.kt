package tachiyomi.domain.source.novel.interactor

import eu.kanade.tachiyomi.source.model.FilterList
import tachiyomi.domain.source.novel.repository.NovelSourcePagingSourceType
import tachiyomi.domain.source.novel.repository.NovelSourceRepository

class GetRemoteNovel(
    private val repository: NovelSourceRepository,
) {

    fun subscribe(sourceId: Long, query: String, filterList: FilterList): NovelSourcePagingSourceType {
        return when (query) {
            QUERY_POPULAR -> repository.getPopularNovels(sourceId)
            QUERY_LATEST -> repository.getLatestNovels(sourceId)
            else -> repository.searchNovels(sourceId, query, filterList)
        }
    }

    companion object {
        const val QUERY_POPULAR = "eu.kanade.domain.source.novel.interactor.POPULAR"
        const val QUERY_LATEST = "eu.kanade.domain.source.novel.interactor.LATEST"
    }
}
