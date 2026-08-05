package eu.kanade.domain.source.novel.interactor

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.source.novel.model.Source
import tachiyomi.domain.source.novel.repository.NovelSourceRepository

class GetNovelSourcesWithFavoriteCount(
    private val repository: NovelSourceRepository,
) {

    fun subscribe(): Flow<List<Pair<Source, Long>>> {
        return repository.getNovelSourcesWithFavoriteCount()
    }
}
