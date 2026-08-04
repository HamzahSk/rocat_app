# Task: Fase 1 Novel — Database Architecture (SQLDelight)

**Tanggal:** 2026-08-04 23:05
**Status:** Selesai

## Ringkasan
Menambahkan dukungan tipe media **Novel** pada database SQLDelight Manga (`Database`, `tachiyomi.db`). Novel disatukan ke database Manga (karena novel berbasis bab/text-reading seperti manga) dengan tabel & view terpisah sehingga pemisahan history/updates/library antar Anime-Manga-Novel efisien dan backward compatible.

## Keputusan Arsitektur
- **Approach:** Tambah tabel novel baru ke `Database` yang sudah ada (bukan tabel `library_item` dengan flag, bukan DB ketiga) — semua perubahan **additive**, tabel Manga/Anime tidak disentuh.
- **Schema v34→v35** (SQLDelight 2.x: `version = maxMigration + 1`; migration file baru = `34.sqm`).

## File & Database
- **Schema baru:** `data/src/main/sqldelight/datanovel/` → `novels.sq`, `novel_chapters.sq`, `novelhistory.sq`, `novels_categories.sq`, `novelsources.sq` (mirror `mangas`/`chapters`/`history`; reuse enum `UpdateStrategy` + `List<String>`/`Date`/`Boolean` adapters).
- **View baru:** `data/src/main/sqldelight/view/` → `novellibraryView.sq`, `novelupdatesView.sq`, `novelhistoryView.sq` (mirror library/updates/history view Manga, query `novellibrary`, `getRecentNovelUpdates`, `novelhistory`).
- **Migrasi:** `data/src/main/sqldelight/migrations/34.sqm` — CREATE TABLE/INDEX/TRIGGER/VIEW novel (additive; `verifyDebugDatabaseMigration` PASSED).
- **DI/Adapters:** `app/.../di/AppModule.kt` — `Database(...)` kini butuh `novelhistoryAdapter` (DateColumnAdapter) & `novelsAdapter` (StringList + MangaUpdateStrategyColumnAdapter); `SearchHistoryRepositoryImplTest.kt` disesuaikan.
- **DAO queries:** Insert/Update/Delete/Select lengkap di `novels.sq`; `getFavorites`, `getChaptersByNovelId` (chapters per novel), history upsert/query, dsb.

## Test
- `NovelDatabaseMigrationTest.kt` (baru, `data/src/test/.../novelmigration/`): 2 test PASSED — (1) migrasi v34→v35 mempertahankan dummy data Manga (mangas+chapters+history) & tabel novel siap CRUD + view library/updates/history jalan; (2) database Anime tidak tersentuh & tetap fungsional. Migrasi diuji via `Database.Schema.migrate(driver, 34, 35)` setelah menghapus objek novel dari hasil `create()` (simulasi schema lama v34).
- `spotlessApply` bersih; `:data:testDebugUnitTest` & seluruh `testDebugUnitTest` BUILD SUCCESSFUL; `assembleDebug` BUILD SUCCESSFUL; `verifyDebugDatabaseMigration` PASSED.

## Next Steps
- Belum di-commit (menunggu instruksi). Fase 2: domain model/repository Novel, extension source API, dan UI. File leftover `ai-response-recap.txt` masih ada (diubah spotless).
