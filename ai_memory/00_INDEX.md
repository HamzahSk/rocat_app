# AI Memory Index

Proyek: Aniyomi — fork Android reader (Manga + Anime) dengan dua database SQLDelight terpisah.

## File Log
- [task_20260804_1730_search_history.md](task_20260804_1730_search_history.md) — Fitur riwayat pencarian (search history) per source.
- [task_20260804_2305_novel_db_phase1.md](task_20260804_2305_novel_db_phase1.md) — Fase 1 Novel: arsitektur database SQLDelight (tabel/view novel, migrasi 34.sqm, migration test).

## Status Proyek
- Build & seluruh test hijau: `testDebugUnitTest` BUILD SUCCESSFUL + `assembleDebug` BUILD SUCCESSFUL + `spotlessApply` bersih.
- Manga catalog DB: versi 35 (schema) / migration terakhir `34.sqm` (menambah tabel Novel: novels, novel_chapters, novelhistory, novels_categories, novelsources + 3 view); Anime DB: versi 137 (`data/src/main/sqldelightanime/migrations/136.sqm`).
- Novel terintegrasi ke database Manga (`tachiyomi.db`) dengan tabel terpisah — backward compatible (hanya additive), history/updates/library novel terpisah per media.

## Konvensi
- Wajib baca `00_INDEX.md` dulu sebelum mulai kerja.
- Tulis log per tugas di `ai_memory/task_YYYYMMDD_HHMM_[nama_task].md`, lalu update index ini.
