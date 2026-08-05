# Task: Fase 2 Novel — Source/Extension + Reader + Test (2026-08-05 03:41)

## Ringkasan
Menyelesaikan Fase 2 integrasi Novel di Aniyomi: logika Source/Extension Novel (port plugin LNReader
`royalroad.ts`) dan Novel Reader berbasis teks. Build, lint (spotless), dan test hijau.

## Yang dikerjakan
- **App-layer Novel**: `NovelExtension`/`NovelExtensionLoader`/`NovelExtensionManager` (mirror manga),
  `AndroidNovelSourceManager` (seed `SampleNovelSource` + `RoyalRoadNovelSource`).
- **Konversi model**: `Novel.kt` (`toSNovel`/`copyFrom`), `NovelChapter.kt` (`toSNovelChapter`/`copyFromSNovelChapter`).
- **Interactors** (domain + app): `NetworkToLocalNovel`, `NovelFetchInterval` (max 28), `UpdateNovel`,
  `GetNovel*`, `GetChaptersByNovelId`, `UpdateNovelChapter`, `ShouldUpdateDbNovelChapter`,
  `SyncNovelChaptersWithSource`.
- **Sumber**: `RoyalRoadNovelSource` (ParsedNovelHttpSource, parse JSON `window.chapters` via
  kotlinx.serialization, fixture HTML test), `SampleNovelSource` (50.000 kata/deterministik).
- **Reader**: `NovelReaderActivity` (Compose LazyColumn + settings fontSize/lineSpacing/theme),
  `ChapterTextExtractor`, `NovelReaderPreferences`.
- **DI**: `DomainModule`, `AppModule` eager-init manager.

## Keputusan penting
- Ganti `org.json.JSONArray` → `kotlinx.serialization` di `chapterListParse` karena `org.json` di-mock
  pada unit test JVM Android.
- Delegasi `Preference.collectAsState()` butuh import `tachiyomi.presentation.core.util.collectAsState`
  (extension, bukan compose bawaan) agar delegate `by` terselesaikan.

## Verifikasi
- `spotlessApply`, `assembleDebug`, `:app:testDebugUnitTest` → BUILD SUCCESSFUL (16 test baru: parser,
  RR source fixtures, extractor + performa 50k kata < 10s).

## Berikutnya
- Uji manual reader di emulator (loading teks, scroll LazyColumn).
- Katalog/favorite/library UI Novel belum diintegrasikan ke layar navigasi.
