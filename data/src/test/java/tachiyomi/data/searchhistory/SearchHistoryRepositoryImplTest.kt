package tachiyomi.data.searchhistory

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import data.History
import data.Mangas
import dataanime.Animehistory
import dataanime.Animes
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.data.AnimeUpdateStrategyColumnAdapter
import tachiyomi.data.Database
import tachiyomi.data.DateColumnAdapter
import tachiyomi.data.FetchTypeColumnAdapter
import tachiyomi.data.MangaUpdateStrategyColumnAdapter
import tachiyomi.data.StringListColumnAdapter
import tachiyomi.data.handlers.anime.AndroidAnimeDatabaseHandler
import tachiyomi.data.handlers.manga.AndroidMangaDatabaseHandler
import tachiyomi.domain.searchhistory.model.SearchSourceType
import tachiyomi.domain.searchhistory.repository.SearchHistoryRepository
import tachiyomi.mi.data.AnimeDatabase

class SearchHistoryRepositoryImplTest {

    private fun createRepository(): SearchHistoryRepositoryImpl {
        val mangaDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        Database.Schema.create(mangaDriver)
        val mangaDatabase = Database(
            driver = mangaDriver,
            historyAdapter = History.Adapter(
                last_readAdapter = DateColumnAdapter,
            ),
            mangasAdapter = Mangas.Adapter(
                genreAdapter = StringListColumnAdapter,
                update_strategyAdapter = MangaUpdateStrategyColumnAdapter,
            ),
        )

        val animeDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        AnimeDatabase.Schema.create(animeDriver)
        val animeDatabase = AnimeDatabase(
            driver = animeDriver,
            animehistoryAdapter = Animehistory.Adapter(
                last_seenAdapter = DateColumnAdapter,
            ),
            animesAdapter = Animes.Adapter(
                genreAdapter = StringListColumnAdapter,
                update_strategyAdapter = AnimeUpdateStrategyColumnAdapter,
                fetch_typeAdapter = FetchTypeColumnAdapter,
            ),
        )

        return SearchHistoryRepositoryImpl(
            mangaHandler = AndroidMangaDatabaseHandler(mangaDatabase, mangaDriver),
            animeHandler = AndroidAnimeDatabaseHandler(animeDatabase, animeDriver),
        )
    }

    @Test
    fun `keeps only the 8 most recent queries per source`() = runTest {
        val repository = createRepository()

        (1..10).forEach { index ->
            repository.insertSearchQuery(
                sourceId = 1L,
                query = "query$index",
                sourceType = SearchSourceType.MANGA,
                now = index.toLong(),
            )
        }

        val history = repository.getSearchHistoryBySource(1L, SearchSourceType.MANGA).first()
        history.map { it.searchQuery } shouldContainExactly (10 downTo 3).map { "query$it" }
        history.size shouldBe SearchHistoryRepository.MAX_SEARCH_HISTORY
        history shouldNotContain "query1"
        history shouldNotContain "query2"
    }

    @Test
    fun `does not delete anything when exactly 8 queries are stored`() = runTest {
        val repository = createRepository()

        (1..8).forEach { index ->
            repository.insertSearchQuery(
                sourceId = 1L,
                query = "query$index",
                sourceType = SearchSourceType.MANGA,
                now = index.toLong(),
            )
        }

        val history = repository.getSearchHistoryBySource(1L, SearchSourceType.MANGA).first()
        history.map { it.searchQuery } shouldContainExactly (8 downTo 1).map { "query$it" }
    }

    @Test
    fun `re-searching an existing query moves it to the top`() = runTest {
        val repository = createRepository()

        repository.insertSearchQuery(1L, "naruto", SearchSourceType.MANGA, now = 1L)
        repository.insertSearchQuery(1L, "one piece", SearchSourceType.MANGA, now = 2L)
        repository.insertSearchQuery(1L, "naruto", SearchSourceType.MANGA, now = 3L)

        val history = repository.getSearchHistoryBySource(1L, SearchSourceType.MANGA).first()
        history.map { it.searchQuery } shouldContainExactly listOf("naruto", "one piece")
        history.size shouldBe 2
    }

    @Test
    fun `separates history per source`() = runTest {
        val repository = createRepository()

        repository.insertSearchQuery(1L, "naruto", SearchSourceType.MANGA, now = 1L)
        repository.insertSearchQuery(2L, "one piece", SearchSourceType.MANGA, now = 2L)

        val historySourceOne = repository.getSearchHistoryBySource(1L, SearchSourceType.MANGA).first()
        val historySourceTwo = repository.getSearchHistoryBySource(2L, SearchSourceType.MANGA).first()

        historySourceOne.map { it.searchQuery } shouldContainExactly listOf("naruto")
        historySourceTwo.map { it.searchQuery } shouldContainExactly listOf("one piece")
    }

    @Test
    fun `works independently for anime and manga sources`() = runTest {
        val repository = createRepository()

        repository.insertSearchQuery(1L, "naruto", SearchSourceType.MANGA, now = 1L)
        repository.insertSearchQuery(1L, "naruto shippuden", SearchSourceType.ANIME, now = 2L)

        val mangaHistory = repository.getSearchHistoryBySource(1L, SearchSourceType.MANGA).first()
        val animeHistory = repository.getSearchHistoryBySource(1L, SearchSourceType.ANIME).first()

        mangaHistory.map { it.searchQuery } shouldContainExactly listOf("naruto")
        animeHistory.map { it.searchQuery } shouldContainExactly listOf("naruto shippuden")
    }

    @Test
    fun `deletes a single history item`() = runTest {
        val repository = createRepository()

        repository.insertSearchQuery(1L, "naruto", SearchSourceType.MANGA, now = 1L)
        repository.insertSearchQuery(1L, "one piece", SearchSourceType.MANGA, now = 2L)
        repository.insertSearchQuery(1L, "bleach", SearchSourceType.MANGA, now = 3L)

        val history = repository.getSearchHistoryBySource(1L, SearchSourceType.MANGA).first()
        val toDelete = history.first { it.searchQuery == "one piece" }
        repository.deleteSearchQuery(toDelete.id, SearchSourceType.MANGA)

        val remaining = repository.getSearchHistoryBySource(1L, SearchSourceType.MANGA).first()
        remaining.map { it.searchQuery } shouldContainExactly listOf("bleach", "naruto")
    }

    @Test
    fun `clears history for a single source`() = runTest {
        val repository = createRepository()

        repository.insertSearchQuery(1L, "naruto", SearchSourceType.MANGA, now = 1L)
        repository.insertSearchQuery(2L, "one piece", SearchSourceType.MANGA, now = 2L)

        repository.clearSearchHistoryBySource(1L, SearchSourceType.MANGA)

        repository.getSearchHistoryBySource(1L, SearchSourceType.MANGA).first() shouldBe emptyList()
        repository.getSearchHistoryBySource(2L, SearchSourceType.MANGA).first().map {
            it.searchQuery
        } shouldContainExactly
            listOf("one piece")
    }

    @Test
    fun `clears all search history globally`() = runTest {
        val repository = createRepository()

        repository.insertSearchQuery(1L, "naruto", SearchSourceType.MANGA, now = 1L)
        repository.insertSearchQuery(2L, "one piece", SearchSourceType.ANIME, now = 2L)

        repository.clearAllSearchHistory()

        repository.getSearchHistoryBySource(1L, SearchSourceType.MANGA).first() shouldBe emptyList()
        repository.getSearchHistoryBySource(2L, SearchSourceType.ANIME).first() shouldBe emptyList()
    }
}
