package tachiyomi.domain.category.novel.interactor

import tachiyomi.domain.category.model.Category
import tachiyomi.domain.category.model.CategoryUpdate
import tachiyomi.domain.category.novel.repository.NovelCategoryRepository
import tachiyomi.domain.library.model.plus
import tachiyomi.domain.library.novel.model.NovelLibrarySort
import tachiyomi.domain.library.service.LibraryPreferences
import kotlin.random.Random

class SetSortModeForNovelCategory(
    private val preferences: LibraryPreferences,
    private val categoryRepository: NovelCategoryRepository,
) {

    suspend fun await(
        categoryId: Long?,
        type: NovelLibrarySort.Type,
        direction: NovelLibrarySort.Direction,
    ) {
        val category = categoryId?.let { categoryRepository.getNovelCategory(it) }
        val flags = (category?.flags ?: 0) + type + direction
        if (type == NovelLibrarySort.Type.Random) {
            preferences.randomNovelSortSeed().set(Random.nextInt())
        }
        if (category != null && preferences.categorizedDisplaySettings().get()) {
            categoryRepository.updatePartialNovelCategory(
                CategoryUpdate(
                    id = category.id,
                    flags = flags,
                ),
            )
        } else {
            preferences.novelSortingMode().set(NovelLibrarySort(type, direction))
            categoryRepository.updateAllNovelCategoryFlags(flags)
        }
    }

    suspend fun await(
        category: Category?,
        type: NovelLibrarySort.Type,
        direction: NovelLibrarySort.Direction,
    ) {
        await(category?.id, type, direction)
    }
}
