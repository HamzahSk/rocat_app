package tachiyomi.domain.items.novelchapter.interactor

import tachiyomi.domain.items.novelchapter.model.NovelChapter
import tachiyomi.domain.items.novelchapter.repository.NovelChapterRepository

class GetNovelChapterByUrlAndNovelId(
    private val chapterRepository: NovelChapterRepository,
) {

    suspend fun await(url: String, novelId: Long): NovelChapter? {
        return chapterRepository.getChapterByUrlAndNovelId(url, novelId)
    }
}
