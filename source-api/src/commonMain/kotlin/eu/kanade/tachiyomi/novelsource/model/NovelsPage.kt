package eu.kanade.tachiyomi.novelsource.model

/**
 * A page of novels returned by catalogue style calls such as
 * [eu.kanade.tachiyomi.novelsource.NovelCatalogueSource.getPopularNovels].
 */
data class NovelsPage(val novels: List<SNovel>, val hasNextPage: Boolean)
