# Fase 6: Bug Fix Resolusi Dependency & Chapter Parsing

## Status: Selesai

## Ringkasan Perubahan

### Root Cause
`NoClassDefFoundError: eu.kanade.tachiyomi.novelsource.util.NovelChapterNumberParser` terjadi karena:
1. `source-api/consumer-proguard.pro` hanya memiliki keep rules untuk `source.model.*`, `source.online.*`, dan `animesource.*` — **tidak ada rules untuk `novelsource.*`**.
2. KMP module consumer proguard merging bisa tidak konsisten antara versi Gradle/AGP, sehingga broad rule di app proguard (`-keep,allowoptimization class eu.kanade.**`) tidak selalu cukup.
3. Tidak ada defensive error handling di `SyncNovelChaptersWithSource` — satu exception di `NovelChapterNumberParser.parse()` menghancurkan seluruh coroutine chapter sync.

### Files Diubah
- **`source-api/consumer-proguard.pro`**: Tambah 4 rules eksplisit untuk `novelsource.model.*`, `novelsource.online.*`, `novelsource.** extends NovelSource`, `novelsource.util.*`.
- **`app/proguard-rules.pro`**: Tambah 4 rules novelsource identik sebagai secondary safety net (mirroring source/animesource rules yang sudah ada).
- **`app/.../SyncNovelChaptersWithSource.kt`**: Wrap `NovelChapterNumberParser.parse()` dalam try-catch — log warning dan gunakan fallback `chapter.chapterNumber ?: -1.0` jika parse gagal. Tambah import `logcat.LogPriority` dan `tachiyomi.core.common.util.system.logcat`.

## Verifikasi
- `./gradlew spotlessApply` — BUILD SUCCESSFUL
- `./gradlew assembleDebug` — BUILD SUCCESSFUL (380 tasks)
- `./gradlew :app:testDebugUnitTest` — BUILD SUCCESSFUL (38 PASSED, termasuk 9 NovelChapterNumberParserTest)

## Tugas Selanjutnya (Next Steps)
- Fase 7: End-to-end test novel browsing → detail → chapter list → reader (dengan extension asli atau LocalNovelSource EPUB).
- Evaluasi apakah perlu menambahkan chapter list loading state di UI (skeleton/shimmer) saat fetchChapter berlangsung.
