# Task: Fase 4 Novel — Local Source EPUB (2026-08-05 09:17)

## Ringkasan
Menambahkan `LocalNovelSource` yang membaca file `.epub` dari folder `localnovel/` di storage: parsing
metadata + TOC + isi chapter (dengan image inline `data:` URI), integrasi ke `NovelReader` (progress
+ history), cover extraction. Build hijau: `spotlessApply` + `assembleDebug` + `testDebugUnitTest`
(38 test) semua sukses.

## Yang dikerjakan
- **Epub infra** (`core/archive/EpubReader.kt`): tambah `getPages()`, `getPagePath()`, `resolveEntry()`,
  `getCoverHref()` (EPUB3 `meta property=cover`, EPUB2 `cover-image`).
- **Parser** (`source-local/.../epub/`): `EpubParser` (metadata/chapters/chapterText/cover) +
  `EpubMetadataParser` (pure) + `EpubHtmlCleaner` (pure, image inlining via callback) + `EpubTocParser`
  (pure; nav EPUB3 + NCX EPUB2). `EpubChapter.url = "<epubFileName>/<resolvedZipPath>"`.
- **LocalNovelSource** (expect/actual KMP): `ID=1L`, `lang="other"`, implement `NovelCatalogueSource`
  + `UnmeteredSource`; `LocalNovelSourceFileSystem`, `LocalNovelCoverManager` (ekstrak cover ke
  `.covers/`), filter `NovelOrderBy` (Popular/Latest). DI di `AppModule`; `AndroidNovelSourceManager`
  seed; `GetEnabledNovelSources` selalu aktifkan local; `StorageManager` tambah dir `localnovel`;
  i18n `local_novel_source`.
- **Reader** (`app/.../ui/novelreader/`): `ChapterTextExtractor` → sealed `NovelChapterContent`
  (`Text`/`Image`, decode `data:` via `java.util.Base64`); `NovelReaderViewModel` baru (load chapter,
  mark read + `NovelHistoryUpdate`, persist scroll `lastPageRead` threshold 5 + `onCleared` final);
  `NovelReaderActivity` render `AsyncImage` untuk blok Image; `NovelLibraryTab` pass `novelId`.
- **Cover** (`data/coil/NovelCoverFetcher.kt`): dukung `content://` (UniFile) + `file://`/path absolut.
- **PreferenceModule**: registrasi `NovelReaderPreferences` (cegah crash latent).
- **Tests**: `EpubParserTest` (metadata, parseIsoDate, nav/NCX, cleaner: boilerplate/hidden, data URI,
  SVG `xlink:href`, image limit, mime) + update `ChapterTextExtractorTest` (blok Image, 50k kata).

## Keputusan penting
- Jsoup HTML parser menormalkan `<image>` → `<img>` (attr `xlink:href` tetap), jadi SVG inline dibaca
  lewat `img` dengan fallback `xlink:href`; tag `image` tidak pernah muncul.
- OPF diparse dengan `Parser.xmlParser()` (bukan `Jsoup.parse` HTML) supaya `<meta property="dcterms:modified">`
  text-nya tidak hilang (di HTML parser `<meta>` = void element).
- `source-local` tidak punya test source set; pure helpers dipisah (parser/cleaner) agar bisa diuji dari
  `app/src/test` (jsoup + JUnit5 sudah tersedia).
- Interactor DB ada dua lokasi: `tachiyomi.domain.items.novelchapter.interactor` (domain: GetByUrlAndNovelId,
  UpdateNovelChapter) vs `eu.kanade.domain.items.novelchapter.interactor` (app: SetNovelReadStatus).

## Verifikasi
- `spotlessApply` bersih; `:source-local:compileDebugKotlinAndroid` + `:app:assembleDebug` BUILD
  SUCCESSFUL; `:app:testDebugUnitTest` 38 PASSED / 0 FAILED.
- Fix compile: `file.nameWithoutExtension` nullable → `.orEmpty()`; import `SetNovelReadStatus` dari
  package app.
- Fix test: `<meta>` void element → `meta.text()`/`nextSibling()` fallback; `xlink:href` pada `<img>`
  (normalisasi `<image>`); image eksternal tidak di-flush (teks sekitarnya menyatu).

## Berikutnya / Catatan
- `NovelCoverKeyer` masih belum didaftarkan di `App.kt` (gap dari Fase 3).
- Belum ada file EPUB fixture untuk integration test end-to-end parser (bisa ditambahkan nanti).
