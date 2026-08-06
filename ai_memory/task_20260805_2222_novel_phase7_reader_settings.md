# Fase 7 Novel: Revamp Detail UI, Reader Settings & Konfirmasi Extension (2026-08-05 22:22)

Task: `task_20260805_2222_novel_phase7_reader_settings.md`
Status: Selesai — build + 42 test hijau.

## Ringkasan
Menuntaskan tiga pilar integrasi Novel ke Aniyomi: (1) revamp UI Detail Novel ala
`MangaInfoScreen` (Jetpack Compose + Voyager MVVM), (2) konfirmasi support extension
bertipe NOVEL (ternyata sudah terpenuhi dari fase 2-3), dan (3) Novel Reader Settings
real-time (text size, font family, theme, line spacing, padding) via bottom sheet.
Logika/setting reader diport dari NovelApp (React Native): `ReaderBottomSheet.tsx`,
`ReaderFontPicker.tsx`, `ReaderThemeSelector.tsx`, `readerConstants.ts`, `useSettings.ts`.

## Penemuan Kunci
- Requirement "support extension Novel" SUDAH ada sejak Fase 2-3: `HttpNovelSource`/
  `ParsedNovelHttpSource`/`NovelCatalogueSource`/`NovelSourceFactory` di
  `source-api/.../novelsource/`, plus `NovelExtensionManager` + `NovelExtensionLoader`
  (baca APK tipe NOVEL) + `AndroidNovelSourceManager`. Tidak perlu source baru.

## Yang Diubah
1. `NovelReaderPreferences.kt`: +`fontFamily()` (enum `NovelReaderFontFamily` SYSTEM/SERIF/
   SANS_SERIF/MONOSPACE), +`padding()` (MIN=0/MAX=48/DEFAULT=20); konstanta DEFAULT_FONT_SIZE,
   DEFAULT_LINE_SPACING, `NovelReaderTheme` (LIGHT/DARK/SEPIA) untuk dipakai UI & test.
2. `NovelReaderSettingsSheet.kt` (baru): `ModalBottomSheet` — slider Text size (A-/A+),
   Line spacing, Padding; FilterChip Theme (Light/Dark/Sepia) & Font family.
3. `NovelReaderActivity.kt`: `collectAsState` 5 preferensi; `LazyColumn` memakai
   `fontSize.sp` + `lineHeight=(fontSize*lineSpacing).sp` + `fontFamily` + `contentPadding`
   horizontal=padding.dp; tap layar toggle sheet; warna latar dari `theme.backgroundColor`.
4. `presentation/entries/novel/NovelScreen.kt`: tulis ulang — `EntryToolbar` collapsing +
   `NovelInfoBox` (backdrop blur + cover Buku) + `NovelActionRow` (library/resume) +
   `ExpandableNovelDescription` + `ItemHeader(isManga=true)` + daftar `processedChapters`
   (filter unread) + `PullRefresh` + `VerticalFastScroller` + empty "Tidak ada bab yang
   ditemukan".
5. Komponen baru: `components/NovelInfoHeader.kt`, `components/NovelChapterListItem.kt`,
   `NovelChapterSettingsDialog.kt` (filter unread TriState + display mode
   judul/nomor bab).
6. `NovelScreenModel.kt`: state `unreadFilter` (TriState), `displayMode`
   (DISPLAY_NAME=0x0L / DISPLAY_NUMBER=0x00100000L), `showFilterDialog`, `filterActive`,
   `processedChapters`; fungsi toggle/onChange.
7. Route `ui/entries/novel/NovelScreen.kt`: wire `onFilterClicked`, `onUnreadFilterChanged`,
   `onDisplayModeChanged`, `onDismissFilterDialog`.
8. `i18n/.../base/strings.xml`: +14 string `novel_reader_*` (settings, text size, font family
   x4, line spacing, padding, theme x3, meta).
9. Test baru `NovelReaderPreferencesTest.kt` (4 test): defaults, baca dari InMemoryPreferenceStore
   (fontFamily/padding/theme/lineSpacing), mapping `composeFontFamily`.

## Verifikasi
- `spotlessApply` bersih (fix format); `:app:compileDebugKotlin` SUCCESS.
- `:app:assembleDebug` BUILD SUCCESSFUL.
- `:app:testDebugUnitTest` **42 PASSED / 0 FAILED** (38 lama + 4 baru).

## Next Steps
- Uji manual di emulator: detail novel (info+chapter), dialog filter/display, bottom sheet
  settings reader (perubahan ukuran/jarak/spasi teks & tema langsung terlihat).
- Pertimbangkan sinkronisasi font family/theme reader dengan NovelApp bila perlu konsistensi.
