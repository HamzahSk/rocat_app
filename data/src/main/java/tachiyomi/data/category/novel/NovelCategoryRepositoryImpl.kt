package tachiyomi.data.category.novel

import kotlinx.coroutines.flow.Flow
import tachiyomi.data.Database
import tachiyomi.data.handlers.manga.MangaDatabaseHandler
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.category.model.CategoryUpdate
import tachiyomi.domain.category.novel.repository.NovelCategoryRepository

class NovelCategoryRepositoryImpl(
    private val handler: MangaDatabaseHandler,
) : NovelCategoryRepository {

    override suspend fun getNovelCategory(id: Long): Category? {
        return handler.awaitOneOrNull { categoriesQueries.getCategory(id, ::mapCategory) }
    }

    override suspend fun getAllNovelCategories(): List<Category> {
        return handler.awaitList { categoriesQueries.getCategories(::mapCategory) }
    }

    override suspend fun getAllVisibleNovelCategories(): List<Category> {
        return handler.awaitList { categoriesQueries.getVisibleCategories(::mapCategory) }
    }

    override fun getAllNovelCategoriesAsFlow(): Flow<List<Category>> {
        return handler.subscribeToList { categoriesQueries.getCategories(::mapCategory) }
    }

    override fun getAllVisibleNovelCategoriesAsFlow(): Flow<List<Category>> {
        return handler.subscribeToList { categoriesQueries.getVisibleCategories(::mapCategory) }
    }

    override suspend fun getCategoriesByNovelId(novelId: Long): List<Category> {
        return handler.awaitList {
            novels_categoriesQueries.getCategoriesByNovelId(novelId, ::mapCategory)
        }
    }

    override suspend fun getVisibleCategoriesByNovelId(novelId: Long): List<Category> {
        return handler.awaitList {
            novels_categoriesQueries.getVisibleCategoriesByNovelId(novelId, ::mapCategory)
        }
    }

    override fun getCategoriesByNovelIdAsFlow(novelId: Long): Flow<List<Category>> {
        return handler.subscribeToList {
            novels_categoriesQueries.getCategoriesByNovelId(novelId, ::mapCategory)
        }
    }

    override fun getVisibleCategoriesByNovelIdAsFlow(novelId: Long): Flow<List<Category>> {
        return handler.subscribeToList {
            novels_categoriesQueries.getVisibleCategoriesByNovelId(novelId, ::mapCategory)
        }
    }

    override suspend fun insertNovelCategory(category: Category) {
        handler.await {
            categoriesQueries.insert(
                name = category.name,
                order = category.order,
                flags = category.flags,
            )
        }
    }

    override suspend fun updatePartialNovelCategory(update: CategoryUpdate) {
        handler.await {
            updatePartialBlocking(update)
        }
    }

    override suspend fun updatePartialNovelCategories(updates: List<CategoryUpdate>) {
        handler.await(inTransaction = true) {
            for (update in updates) {
                updatePartialBlocking(update)
            }
        }
    }

    private fun Database.updatePartialBlocking(update: CategoryUpdate) {
        categoriesQueries.update(
            name = update.name,
            order = update.order,
            flags = update.flags,
            hidden = update.hidden?.let { if (it) 1L else 0L },
            categoryId = update.id,
        )
    }

    override suspend fun updateAllNovelCategoryFlags(flags: Long?) {
        handler.await {
            categoriesQueries.updateAllFlags(flags)
        }
    }

    override suspend fun deleteNovelCategory(categoryId: Long) {
        handler.await {
            categoriesQueries.delete(
                categoryId = categoryId,
            )
        }
    }

    private fun mapCategory(
        id: Long,
        name: String,
        order: Long,
        flags: Long,
        hidden: Long,
    ): Category {
        return Category(
            id = id,
            name = name,
            order = order,
            flags = flags,
            hidden = hidden == 1L,
        )
    }
}
