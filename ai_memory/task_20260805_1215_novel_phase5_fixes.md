# Fase 5 Novel: Bug Fixing, Referensi Silang, Penyempurnaan Alur UI

Task: `task_20260805_1215_novel_phase5_fixes.md`
Status: Selesai — build + test hijau.

## Ringkasan
Melengkapi alur UI Novel yang sebelumnya putus: klik source di Browse tidak bekerja,
tidak ada layar detail novel, dan klik item di library langsung buka reader tanpa detail.

## Yang Dikerjakan
1. **GetRemoteNovel** (`domain/.../source/novel/interactor/GetRemoteNovel.kt`):
   mirror `GetRemoteAnime` (QUERY_POPULAR/QUERY_LATEST, `subscribe` via NovelSourceRepository);
   didaftarkan di `app/.../domain/DomainModule.kt` (`addFactory` + import).
2. **Browse Novel Source lengkap** (package `ui/browse/novel/source/browse/` + `presentation/browse/novel/`):
   `BrowseNovelSourceScreen` + `ScreenModel` (Listing Popular/Latest/Search, filter sheet,
   search history `SearchSourceType.NOVEL`, pager flow, displayMode), `BrowseNovelSourceToolbar`,
   `BrowseNovelSourceList/ComfortableGrid/CompactGrid`, `BrowseNovelSourceContent` +
   `MissingNovelSourceScreen`, `SourceFilterNovelDialog` (delegasi ke `SourceFilterMangaDialog`
   karena model `Filter` sama), dan `NovelSourceUtil.ifNovelSourcesLoaded()`.
   Perbaikan import: `GetNovel`/`NetworkToLocalNovel` seharusnya dari `tachiyomi.domain.*`
   (bukan `eu.kanade.domain.*`).
3. **Detail Novel** (`ui/entries/novel/NovelScreen.kt` + `NovelScreenModel.kt` +
   `presentation/entries/novel/NovelScreen.kt`):
   subscribe novel+chapters via `GetNovelWithChapters`, fetch info/chapter otomatis saat
   dibuka dari source (`!initialized`/chapters kosong, dilewati utk `isLocalNovel`),
   toggle favorite (`UpdateNovel.awaitUpdateFavorite`), mark read/unread chapter
   (`SetNovelReadStatus`), lanjut baca (unread pertama), klik chapter →
   `NovelReaderActivity.newIntent(sourceId, novelId, chapterUrl, chapterName)`.
4. **Navigasi**:
   - `NovelSourcesTab.onClickItem` → `navigator.push(BrowseNovelSourceScreen(it.id, GetRemoteNovel.QUERY_POPULAR))`.
   - `NovelLibraryTab.onNovelClicked` → `navigator.push(NovelScreen(id))` (ganti dari langsung buka reader).

## Catatan / Detail Penting
- `LocalNovelSource.ID = 1L`, `Novel.isLocalNovel()` di `tachiyomi.source.local.entries.novel`.
- Cover detail pakai `ItemCover.Book` + `novel.asNovelCover()`; `NovelCoverFetcher`
  menangani `content://`, `file://`, dan http.
- Status novel: konstanta `SNovel.*` (UNKNOWN..ON_HIATUS) + `MR.strings.*`.
- String yang dipakai: `add_to_library`, `remove_from_library`, `action_resume/action_start`,
  `no_chapters_error`, `chapters`, `ongoing`/`completed`/dst.
- Browse novel tanpa long-click action (item `onLongClick = onClick`).

## Verifikasi
- `./gradlew spotlessApply` → BUILD SUCCESSFUL.
- `./gradlew assembleDebug` → BUILD SUCCESSFUL (Kotlin compile + APK).
- `./gradlew testDebugUnitTest` → BUILD SUCCESSFUL (38 test tetap PASSED).

## Next Steps
- Belum: wiring WebView/search dari BrowseNovelSource ke GlobalSearch, action long-press
  browse (add to library/duplicate), dan pengujian end-to-end manual di emulator.
