# AI Memory Index

Proyek: Aniyomi — fork Android reader (Manga + Anime) dengan dua database SQLDelight terpisah.

## File Log
- [task_20260804_1730_search_history.md](task_20260804_1730_search_history.md) — Fitur riwayat pencarian (search history) per source.
- [task_20260804_2305_novel_db_phase1.md](task_20260804_2305_novel_db_phase1.md) — Fase 1 Novel: arsitektur database SQLDelight (tabel/view novel, migrasi 34.sqm, migration test).
- [task_20260805_0341_novel_reader.md](task_20260805_0341_novel_reader.md) — Fase 2 Novel: source/extension (port RoyalRoad LNReader), Novel Reader berbasis teks, tests parser+fixture+performa 50k kata.
- [task_20260805_0651_novel_phase3_ui.md](task_20260805_0651_novel_phase3_ui.md) — Fase 3 Novel: tab Novel (library), manajemen kategori, Novel Sources/Extensions di Browse; build + spotless hijau.

## Status Proyek
- Build & seluruh test hijau: `testDebugUnitTest` BUILD SUCCESSFUL + `assembleDebug` BUILD SUCCESSFUL + `spotlessApply` bersih.
- Manga catalog DB: versi 35 (schema) / migration terakhir `34.sqm` (menambah tabel Novel: novels, novel_chapters, novelhistory, novels_categories, novelsources + 3 view); Anime DB: versi 137 (`data/src/main/sqldelightanime/migrations/136.sqm`).
- Novel terintegrasi ke database Manga (`tachiyomi.db`) dengan tabel terpisah — backward compatible (hanya additive), history/updates/library novel terpisah per media.
- Novel sudah masuk navigasi utama: tab Novel di Home (library + kategori) dan tab Novel Sources/Extensions di Browse (BrowseTab kini 8 tab, search mapping `% 3`).

## Konvensi
- Wajib baca `00_INDEX.md` dulu sebelum mulai kerja.
- Tulis log per tugas di `ai_memory/task_YYYYMMDD_HHMM_[nama_task].md`, lalu update index ini.
