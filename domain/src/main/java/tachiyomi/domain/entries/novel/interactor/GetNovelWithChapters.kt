package tachiyomi.domain.entries.novel.interactor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import tachiyomi.domain.entries.novel.model.Novel
import tachiyomi.domain.entries.novel.repository.NovelRepository
import tachiyomi.domain.items.novelchapter.model.NovelChapter
import tachiyomi.domain.items.novelchapter.repository.NovelChapterRepository

class GetNovelWithChapters(
    private val novelRepository: NovelRepository,
    private val chapterRepository: NovelChapterRepository,
) {

    suspend fun subscribe(id: Long): Flow<Pair<Novel, List<NovelChapter>>> {
        return combine(
            novelRepository.getNovelByIdAsFlow(id),
            chapterRepository.getChapterByNovelIdAsFlow(id, applyScanlatorFilter = false),
        ) { novel, chapters ->
            Pair(novel, chapters)
        }
    }

    suspend fun awaitNovel(id: Long): Novel {
        return novelRepository.getNovelById(id)
    }

    suspend fun awaitChapters(id: Long): List<NovelChapter> {
        return chapterRepository.getChapterByNovelId(id, applyScanlatorFilter = false)
    }
}
