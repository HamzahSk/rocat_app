package tachiyomi.domain.source.novel.repository

import androidx.paging.PagingSource
import eu.kanade.tachiyomi.novelsource.model.SNovel
import eu.kanade.tachiyomi.source.model.FilterList
import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.source.novel.model.NovelSourceWithCount
import tachiyomi.domain.source.novel.model.Source

typealias NovelSourcePagingSourceType = PagingSource<Long, SNovel>

interface NovelSourceRepository {

    fun getNovelSources(): Flow<List<Source>>

    fun getOnlineNovelSources(): Flow<List<Source>>

    fun getNovelSourcesWithFavoriteCount(): Flow<List<Pair<Source, Long>>>

    fun getNovelSourcesWithNonLibraryNovels(): Flow<List<NovelSourceWithCount>>

    fun searchNovels(sourceId: Long, query: String, filterList: FilterList): NovelSourcePagingSourceType

    fun getPopularNovels(sourceId: Long): NovelSourcePagingSourceType

    fun getLatestNovels(sourceId: Long): NovelSourcePagingSourceType
}
