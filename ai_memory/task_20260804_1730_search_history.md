# Task: Search History (Riwayat Pencarian)

**Tanggal:** 2026-08-04 17:30
**Status:** Selesai

## Ringkasan
Fitur riwayat pencarian per source untuk halaman browse (Manga & Anime), tersimpan di dua database SQLDelight terpisah (`Database` untuk Manga, `AnimeDatabase` untuk Anime). Maksimal 8 kueri terbaru per source, terpisah per source, mendukung hapus satu item, hapus per source, dan hapus semua (global).

## File & Database
- **Schema SQLDelight (baru):** `data/src/main/sqldelight/data/searchHistory.sq` dan `data/src/main/sqldelightanime/dataanime/searchHistory.sq` — tabel `search_history(id, source_id, search_query, created_at)`, index `source_id`, unique `(source_id, search_query)`. Kueri: `insertSearchQuery` (upsert refresh `created_at`), `deleteOldestExcess`, `getSearchHistoryBySource` (LIMIT 8, terbaru dulu), `deleteSearchQuery`, `clearSearchHistoryBySource`, `clearAllSearchHistory`. Nama file wajib `searchHistory.sq` agar accessor jadi `searchHistoryQueries`.
- **Migrasi:** Manga DB versi 33 (`33.sqm`), Anime DB versi 136 (`136.sqm`).
- **Domain:** `tachiyomi.domain.searchhistory` — model `SearchHistory`, enum `SearchSourceType` (MANGA/ANIME), interface `SearchHistoryRepository` dengan `MAX_SEARCH_HISTORY = 8L`.
- **Data:** `SearchHistoryRepositoryImpl` (route per `SearchSourceType`; `insertSearchQuery` dalam `inTransaction = true` lalu `deleteOldestExcess`) + `SearchHistoryMapper`.
- **DI:** `DomainModule.kt` — `addSingletonFactory<SearchHistoryRepository>`.
- **UI/VM:** `BrowseMangaSourceScreenModel` & `BrowseAnimeSourceScreenModel` (collect history ke State, insert di `search()`, aksi delete/clear), `SearchHistoryRow.kt` (chip recent searches + ikon clear) di kedua screen, i18n `label_recent_searches` & `action_clear_search_history`.
- **Test deps:** `sqldelight-driver-jvm` (sqlite-driver) + `sqlite-jdbc` di `data/build.gradle.kts` & `libs.versions.toml`.

## Test
- `data/src/test/.../SearchHistoryRepositoryImplTest.kt`: 8 test PASSED (cap 8 & evict tertua, genap 8 tidak evict, duplikat naik ke atas, pisah per source, Manga/Anime independen, hapus satu, clear per source, clear semua).
- `app/src/test/.../coil/TachiyomiImageDecoderTest.kt` (Step 4.3): 2 test PASSED — verifikasi error decode gambar besar (source tidak bisa dibuka) ditangkap sebagai `IllegalStateException` terkontrol ("Failed to initialize decoder") sehingga tidak crash; Coil meneruskan ke error callback → layout error reader (bukan crash).
- Seluruh `testDebugUnitTest` BUILD SUCCESSFUL; `spotlessApply` bersih.

## Next Steps
- Belum di-commit (menunggu instruksi). File leftover `ai-response-recap.txt` belum dihapus.
