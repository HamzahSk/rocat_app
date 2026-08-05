package tachiyomi.domain.category.novel.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.category.model.CategoryUpdate

interface NovelCategoryRepository {

    suspend fun getNovelCategory(id: Long): Category?

    suspend fun getAllNovelCategories(): List<Category>

    suspend fun getAllVisibleNovelCategories(): List<Category>

    fun getAllNovelCategoriesAsFlow(): Flow<List<Category>>

    fun getAllVisibleNovelCategoriesAsFlow(): Flow<List<Category>>

    suspend fun getCategoriesByNovelId(novelId: Long): List<Category>

    suspend fun getVisibleCategoriesByNovelId(novelId: Long): List<Category>

    fun getCategoriesByNovelIdAsFlow(novelId: Long): Flow<List<Category>>

    fun getVisibleCategoriesByNovelIdAsFlow(novelId: Long): Flow<List<Category>>

    suspend fun insertNovelCategory(category: Category)

    suspend fun updatePartialNovelCategory(update: CategoryUpdate)

    suspend fun updatePartialNovelCategories(updates: List<CategoryUpdate>)

    suspend fun updateAllNovelCategoryFlags(flags: Long?)

    suspend fun deleteNovelCategory(categoryId: Long)
}
