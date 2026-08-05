# Task: Fase 3 Novel — UI Navigasi + Kategori + Sources/Extensions (2026-08-05 06:51)

## Ringkasan
Mengintegrasikan UI Novel ke layar navigasi Aniyomi: tab "Novel" (Home/Library), manajemen kategori novel,
tab "Novel Sources" + "Novel Extensions" (Browse). Build hijau: `spotlessApply` bersih + `assembleDebug`
BUILD SUCCESSFUL.

## Yang dikerjakan
- **Presentation Library novel** (`presentation/library/novel/`): `NovelLibraryContent.kt`,
  `NovelLibraryPager.kt`, `NovelLibraryList.kt`, `NovelLibraryCompactGrid.kt`,
  `NovelLibraryComfortableGrid.kt`, `NovelLibrarySettingsDialog.kt`. Adaptasi dari manga memakai komponen
  generik `EntryListItem`/`EntryCompactGridItem`/`EntryComfortableGridItem` (`CommonEntryItem.kt`) +
  `asNovelCover()`; tanpa DownloadsBadge.
- **NovelLibraryTab** (`ui/library/novel/`): reader via `NovelReaderActivity.newIntent`,
  `DeleteLibraryEntryDialog(isManga=false, isNovel=true)`, channel query/settings sheet,
  `HomeScreen.showBottomNav`/`openTab`.
- **Navigasi**: `NavStyle.tabs` + `StartScreen.NOVEL`; `HomeScreen.Tab.AnimeLib()` diverifikasi ada.
- **Kategori novel**: 5 interactor CRUD (`Create/Delete/Hide/Rename/ReorderNovelCategory`) + registrasi
  `DomainModule`; `NovelCategoryScreenModel` + `NovelCategoryTab` (page 2 di CategoriesTab) +
  `NovelCategoryScreen` (presentation).
- **Novel Sources** (`presentation/browse/novel/` + `ui/browse/novel/source/`): screen, filter, screenmodel,
  tab; `Source.icon` di `domain/source/novel/model/NovelSource.kt`.
- **Novel Extensions** (`ui/browse/novel/extension/` + presentation): `GetNovelExtensionsByType`
  (installed+untrusted, tanpa remote available oleh desain), `NovelExtensionsScreenModel`, `NovelExtensionsScreen`,
  `novelExtensionsTab`. `NovelExtensionManager` ditambah `reload()/trust()/uninstallExtension()`.
- **TabbedScreen**: tambah `novelSearchQuery`/`onChangeNovelSearchQuery`; mapping search per page diubah
  dari `% 2` ke `% 3` (Browse kini 8 tab: anime/manga/novel sources, anime/manga/novel extensions, migrate x2).
- **i18n** (AYMR base): `label_novel`, `label_novel_library`, `label_novel_sources`,
  `label_novel_extensions`, `novel_from_library`, `action_sort_last_novel_update`.

## Keputusan penting
- Query search BrowseTab menggunakan `currentPage % 3` (0→anime, 1→manga, 2→novel) — berlaku untuk
  HistoriesTab (2 tab) dan BrowseTab (8 tab).
- `PreferenceMutableState.collectAsState()` wajib import `tachiyomi.presentation.core.util.collectAsState`.
- Query filter extensions memakai label eksplisit `filter@{ ... return@filter }` (sama dengan manga).

## Verifikasi
- `spotlessApply` bersih (perbaikan 1 ktlint max-line-length di `SetNovelReadStatus.kt`).
- `assembleDebug` → BUILD SUCCESSFUL (perbaikan compile: import collectAsState, label `filter@`,
  import `androidx.compose.ui.unit.dp`).

## Berikutnya / Catatan
- Gap yang tersisa: `NovelCoverKeyer` belum didaftarkan di `App.kt` (potensi NPE fetch cover); source
  novel `onClickItem` masih no-op (belum ada BrowseNovelSourceScreen); `onRefresh` library novel selalu
  return false; Novel Extension tab tidak punya repos/filter screen (belum ada remote available API).
