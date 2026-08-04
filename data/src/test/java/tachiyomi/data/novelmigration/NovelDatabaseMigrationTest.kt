package tachiyomi.data.novelmigration

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import data.History
import data.Mangas
import dataanime.Animehistory
import dataanime.Animes
import datanovel.Novelhistory
import datanovel.Novels
import eu.kanade.tachiyomi.animesource.model.AnimeUpdateStrategy
import eu.kanade.tachiyomi.animesource.model.FetchType
import eu.kanade.tachiyomi.source.model.UpdateStrategy
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.data.AnimeUpdateStrategyColumnAdapter
import tachiyomi.data.Database
import tachiyomi.data.DateColumnAdapter
import tachiyomi.data.FetchTypeColumnAdapter
import tachiyomi.data.MangaUpdateStrategyColumnAdapter
import tachiyomi.data.StringListColumnAdapter
import tachiyomi.mi.data.AnimeDatabase
import java.util.Date

class NovelDatabaseMigrationTest {

    private fun createDatabase(driver: JdbcSqliteDriver): Database {
        Database.Schema.create(driver)
        return Database(
            driver = driver,
            historyAdapter = History.Adapter(
                last_readAdapter = DateColumnAdapter,
            ),
            mangasAdapter = Mangas.Adapter(
                genreAdapter = StringListColumnAdapter,
                update_strategyAdapter = MangaUpdateStrategyColumnAdapter,
            ),
            novelhistoryAdapter = Novelhistory.Adapter(
                last_readAdapter = DateColumnAdapter,
            ),
            novelsAdapter = Novels.Adapter(
                genreAdapter = StringListColumnAdapter,
                update_strategyAdapter = MangaUpdateStrategyColumnAdapter,
            ),
        )
    }

    private fun dropNovelObjects(driver: JdbcSqliteDriver) {
        driver.execute(null, "DROP VIEW IF EXISTS novelhistoryView", 0)
        driver.execute(null, "DROP VIEW IF EXISTS novelupdatesView", 0)
        driver.execute(null, "DROP VIEW IF EXISTS novellibraryView", 0)
        driver.execute(null, "DROP TABLE IF EXISTS novels_categories", 0)
        driver.execute(null, "DROP TABLE IF EXISTS novelhistory", 0)
        driver.execute(null, "DROP TABLE IF EXISTS novel_chapters", 0)
        driver.execute(null, "DROP TABLE IF EXISTS novelsources", 0)
        driver.execute(null, "DROP TABLE IF EXISTS novels", 0)
    }

    private fun Database.insertDummyManga(): Pair<Long, Long> {
        mangasQueries.insert(
            source = 1L,
            url = "manga-url",
            artist = "Manga Artist",
            author = "Manga Author",
            description = "A manga",
            genre = listOf("Action", "Adventure"),
            title = "Manga A",
            status = 0L,
            thumbnailUrl = null,
            favorite = true,
            lastUpdate = null,
            nextUpdate = null,
            initialized = true,
            viewerFlags = 1L,
            chapterFlags = 0L,
            coverLastModified = 0L,
            dateAdded = 1000L,
            updateStrategy = UpdateStrategy.ALWAYS_UPDATE,
            calculateInterval = 0L,
            version = 0L,
        )
        val mangaId = mangasQueries.selectLastInsertedRowId().executeAsOne()
        chaptersQueries.insert(
            mangaId = mangaId,
            url = "manga-chapter-url",
            name = "Chapter 1",
            scanlator = null,
            read = false,
            bookmark = false,
            lastPageRead = 0L,
            chapterNumber = 1.0,
            sourceOrder = 1L,
            dateFetch = 2000L,
            dateUpload = 2000L,
            version = 0L,
        )
        val chapterId = chaptersQueries.selectLastInsertedRowId().executeAsOne()
        historyQueries.upsert(chapterId, Date(3000L), 60L)
        return mangaId to chapterId
    }

    @Test
    fun `migrating manga database from v34 to v35 preserves existing manga library data`() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        val db = createDatabase(driver)

        // Simulate the old (v34) database by removing the novel objects, keeping
        // every pre-existing Manga table untouched.
        dropNovelObjects(driver)

        val (mangaId, _) = db.insertDummyManga()
        db.mangasQueries.getAllManga().executeAsList().map { it.title } shouldContainExactly
            listOf("Manga A")

        // Run the real 34.sqm migration.
        Database.Schema.migrate(driver, oldVersion = 34, newVersion = 35)

        // Existing Manga library, chapters and history all survive the migration.
        db.mangasQueries.getAllManga().executeAsList().map { it.title } shouldContainExactly
            listOf("Manga A")
        db.chaptersQueries.getChaptersByMangaId(mangaId, applyScanlatorFilter = 0L).executeAsList()
            .map { it.name } shouldContainExactly listOf("Chapter 1")
        db.historyQueries.getHistoryByMangaId(mangaId).executeAsList().size shouldBe 1

        // The novel tables now exist and support full CRUD.
        db.novelsQueries.insert(
            source = 1L,
            url = "novel-url",
            artist = null,
            author = "Novel Author",
            description = "A novel",
            genre = listOf("Fantasy"),
            title = "Novel One",
            status = 0L,
            thumbnailUrl = null,
            favorite = true,
            lastUpdate = null,
            nextUpdate = null,
            initialized = true,
            viewerFlags = 1L,
            chapterFlags = 0L,
            coverLastModified = 0L,
            dateAdded = 5000L,
            updateStrategy = UpdateStrategy.ALWAYS_UPDATE,
            calculateInterval = 0L,
            version = 0L,
        )
        val novelId = db.novelsQueries.selectLastInsertedRowId().executeAsOne()
        db.novel_chaptersQueries.insert(
            novelId = novelId,
            url = "novel-chapter-url",
            name = "Prologue",
            scanlator = null,
            read = false,
            bookmark = false,
            lastPageRead = 0L,
            chapterNumber = 0.0,
            sourceOrder = 1L,
            dateFetch = 6000L,
            dateUpload = 6000L,
            version = 0L,
        )
        val novelChapterId = db.novel_chaptersQueries.selectLastInsertedRowId().executeAsOne()
        db.novelhistoryQueries.upsert(novelChapterId, Date(7000L), 90L)

        db.novelsQueries.getFavorites().executeAsList().map { it.title } shouldContainExactly
            listOf("Novel One")
        db.novel_chaptersQueries.getChaptersByNovelId(novelId).executeAsList().map { it.name } shouldContainExactly
            listOf("Prologue")
        db.novelhistoryQueries.getHistoryByNovelId(novelId).executeAsList().size shouldBe 1

        // Library, updates and history views are usable for novels.
        db.novellibraryViewQueries.novellibrary().executeAsList().map { it.title } shouldContainExactly
            listOf("Novel One")
        db.novelupdatesViewQueries.getRecentNovelUpdates(after = 0L, limit = 10).executeAsList()
            .map { it.novelTitle } shouldContainExactly listOf("Novel One")
        db.novelhistoryViewQueries.novelhistory(query = "").executeAsList().size shouldBe 1
    }

    @Test
    fun `anime database remains untouched and functional`() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        AnimeDatabase.Schema.create(driver)
        val animeDatabase = AnimeDatabase(
            driver = driver,
            animehistoryAdapter = Animehistory.Adapter(
                last_seenAdapter = DateColumnAdapter,
            ),
            animesAdapter = Animes.Adapter(
                genreAdapter = StringListColumnAdapter,
                update_strategyAdapter = AnimeUpdateStrategyColumnAdapter,
                fetch_typeAdapter = FetchTypeColumnAdapter,
            ),
        )

        animeDatabase.animesQueries.insert(
            source = 1L,
            url = "anime-url",
            artist = null,
            author = null,
            description = "An anime",
            genre = listOf("Action"),
            title = "Anime One",
            status = 0L,
            thumbnailUrl = null,
            favorite = true,
            lastUpdate = null,
            nextUpdate = null,
            initialized = true,
            viewerFlags = 1L,
            episodeFlags = 0L,
            coverLastModified = 0L,
            dateAdded = 1000L,
            updateStrategy = AnimeUpdateStrategy.ALWAYS_UPDATE,
            calculateInterval = 0L,
            version = 0L,
            fetchType = FetchType.Episodes,
            parentId = null,
            seasonFlags = 0L,
            seasonNumber = 1.0,
            seasonSourceOrder = 0L,
            backgroundUrl = null,
            backgroundLastModified = 0L,
        )

        animeDatabase.animesQueries.getFavorites().executeAsList().map { it.title } shouldContainExactly
            listOf("Anime One")
    }
}
