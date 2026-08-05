package tachiyomi.domain.category.novel.interactor

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.category.novel.repository.NovelCategoryRepository

class GetNovelCategories(
    private val categoryRepository: NovelCategoryRepository,
) {
    fun subscribe(): Flow<List<Category>> {
        return categoryRepository.getAllNovelCategoriesAsFlow()
    }

    fun subscribe(novelId: Long): Flow<List<Category>> {
        return categoryRepository.getCategoriesByNovelIdAsFlow(novelId)
    }

    suspend fun await(): List<Category> {
        return categoryRepository.getAllNovelCategories()
    }

    suspend fun await(novelId: Long): List<Category> {
        return categoryRepository.getCategoriesByNovelId(novelId)
    }
}
