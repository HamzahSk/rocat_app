# Fase 6 Novel: Bug Fixing Dependency & Chapter Parsing (2026-08-05 14:32)

Task: `task_20260805_1432_novel_phase6_fixes.md`
Status: Selesai — build + 38 test hijau.

## Ringkasan
Menangani `NoClassDefFoundError: eu.kanade.tachiyomi.novelsource.util.NovelChapterNumberParser`
saat membuka Detail Novel (daftar chapter kosong / "Tidak ada bab yang ditemukan").

## Penelusuran & Verifikasi
- Kelas `NovelChapterNumberParser` sudah ada di `source-api/src/commonMain/.../novelsource/util/`
  dan terkompilasi (tanpa error). Direferensikan dari `app` (`SyncNovelChaptersWithSource`) dan
  `source-local` (`EpubParser`).
- Wiring modul benar: `app`/`domain`/`source-local` semuanya `implementation(projects.sourceApi)`.
- Verifikasi dexdump: kelas ADA di APK debug dan release (R8 tetap menyimpannya karena
  `-keep,allowoptimization class eu.kanade.**`). Masalah sesungguhnya: `source-api/consumer-proguard.pro`
  tidak punya rule `novelsource.*` (hanya manga/anime) sehingga API extension novel rentan ter-strip
  di build extension; dan sync chapter tidak resilien (parser error = seluruh daftar bab hilang).

## Yang Diubah
1. `source-api/consumer-proguard.pro`: tambah rule keep `novelsource.model/online/util.**` +
   `novelsource.** extends NovelSource` (menyamai pola manga/anime).
2. `app/proguard-rules.pro`: tambah `-keep,allowoptimization class eu.kanade.tachiyomi.novelsource.** { *; }`.
3. `SyncNovelChaptersWithSource.kt`: bungkus `NovelChapterNumberParser.parse` dengan try-catch,
   fallback ke `chapter.chapterNumber` + `logcat(ERROR)` bila parser gagal (cegah daftar bab hilang).
4. `EpubParser.kt` (source-local): try-catch serupa pada `parse("", name)` dengan fallback `-1f`
   (pakai index sebagai nomor bab).

## Verifikasi
- `spotlessApply` bersih; `:app:assembleDebug` + `:app:assembleRelease` BUILD SUCCESSFUL.
- `:app:testDebugUnitTest` 38 PASSED / 0 FAILED.
- dexdump release: `NovelChapterNumberParser` tetap ada di classes.dex setelah R8 + rule baru.

## Next Steps
- Uji manual end-to-end di emulator: buka "The Hunter's Gonna Lay Low" (Royal Road) → daftar bab
  termuat, toast error hilang; buka novel EPUB lokal → chapter number parsing tetap jalan.
