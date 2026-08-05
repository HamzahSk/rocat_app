package eu.kanade.tachiyomi.ui.library.novel

import eu.kanade.tachiyomi.source.novel.getNameForNovelInfo
import tachiyomi.domain.library.novel.LibraryNovel
import tachiyomi.domain.source.novel.service.NovelSourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class NovelLibraryItem(
    val libraryNovel: LibraryNovel,
    var unreadCount: Long = -1,
    var sourceLanguage: String = "",
    private val sourceManager: NovelSourceManager = Injekt.get(),
) {
    /**
     * Checks if a query matches the novel
     *
     * @param constraint the query to check.
     * @return true if the novel matches the query, false otherwise.
     */
    fun matches(constraint: String): Boolean {
        val sourceName by lazy { sourceManager.getOrStub(libraryNovel.novel.source).getNameForNovelInfo() }
        if (constraint.startsWith("id:", true)) {
            val id = constraint.substringAfter("id:").toLongOrNull()
            return libraryNovel.id == id
        }
        return libraryNovel.novel.title.contains(constraint, true) ||
            (libraryNovel.novel.author?.contains(constraint, true) ?: false) ||
            (libraryNovel.novel.artist?.contains(constraint, true) ?: false) ||
            (libraryNovel.novel.description?.contains(constraint, true) ?: false) ||
            constraint.split(",").map { it.trim() }.all { subconstraint ->
                checkNegatableConstraint(subconstraint) {
                    sourceName.contains(it, true) ||
                        (libraryNovel.novel.genre?.any { genre -> genre.equals(it, true) } ?: false)
                }
            }
    }

    /**
     * Checks a predicate on a negatable constraint. If the constraint starts with a minus character,
     * the minus is stripped and the result of the predicate is inverted.
     *
     * @param constraint the argument to the predicate. Inverts the predicate if it starts with '-'.
     * @param predicate the check to be run against the constraint.
     * @return !predicate(x) if constraint = "-x", otherwise predicate(constraint)
     */
    private fun checkNegatableConstraint(
        constraint: String,
        predicate: (String) -> Boolean,
    ): Boolean {
        return if (constraint.startsWith("-")) {
            !predicate(constraint.substringAfter("-").trimStart())
        } else {
            predicate(constraint)
        }
    }
}
