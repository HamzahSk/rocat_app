# Role
Kamu adalah seorang Senior Android Developer dan Software Architect yang ahli dalam bahasa Kotlin, arsitektur Clean/MVVM, dan Jetpack Compose/XML. Kamu sangat familiar dengan basis kode aplikasi open-source Aniyomi dan Tachiyomi. Kamu memiliki spesialisasi dalam mem-*porting* kode TypeScript ke Kotlin dan merancang UI yang efisien untuk performa tinggi.

# Memory
1. **BACA ATURAN MEMORI:**
   - Buka dan baca file `memory_prompt.md` untuk memahami seluruh protokol manajemen memori, pembatasan token, dan aturan penulisan log secara ketat.

# Task
Ini adalah **Fase 2** dari proyek integrasi fitur **Novel** ke dalam Aniyomi (Fase 1: Database sudah diselesaikan). 
Tugasmu sekarang adalah membuat logika *Source/Extension* untuk Novel dan membangun antarmuka pembaca (*Novel Reader*). Logika *parsing* dan *scraping* harus merujuk pada kode TypeScript yang ada di dalam folder `NovelApp`.

# Requirements
1. **Porting TypeScript ke Kotlin:** Buka folder `NovelApp` dan pelajari bagaimana aplikasi tersebut melakukan *fetch* dan *parse* data novel (baik daftar novel maupun isi *chapter*). Terjemahkan logika tersebut ke dalam Kotlin menggunakan arsitektur *Source* Aniyomi (menggunakan OkHttp/JSoup).
2. **Novel Source Architecture:** Buat *base class* atau *interface* untuk `NovelSource` yang sejajar dengan `AnimeSource` dan `MangaSource`. Pastikan sistem bisa membedakan *source* teks dengan gambar/video.
3. **Text-Based Reader UI:** Buat layar pembaca (*reader*) yang khusus didesain untuk teks, bukan gambar. Fitur wajib di *reader*:
   - Pengaturan *Font Size* (Ukuran teks).
   - Pengaturan *Line Spacing* (Jarak antar baris).
   - Pengaturan Tema Latar/Warna Teks (misal: *Dark mode, Light mode, Sepia*).
   - Scroll yang mulus (*continuous scroll* atau *pagination*) untuk teks berukuran masif.

---

# Implementation Steps

### Step 1: Base Source & Porting Parser
- Buat *interface* `NovelSource` (mirip dengan `HttpSource` atau `ParsedHttpSource` di Tachiyomi) yang memiliki fungsi `fetchPopularNovel`, `fetchNovelDetails`, dan `fetchChapterList`.
- Translasi file TypeScript *parser* dari folder `NovelApp` menjadi class Kotlin yang mengimplementasikan `NovelSource` tersebut.

### Step 2: Domain & Data Layer Integration
- Hubungkan `NovelSource` yang baru dibuat dengan *Repository* dan *Database* (yang sudah dikerjakan di Fase 1).
- Pastikan logika *Update History* dan penanda *Chapter Read/Unread* berfungsi normal saat data di-*fetch* dari *source*.

### Step 3: Novel Reader Presentation
- Buat `NovelReaderActivity` atau Compose/Fragment. 
- Implementasikan UI *settings* (*bottom sheet* atau menu) untuk mengatur ukuran huruf dan warna tema.
- Gunakan komponen UI yang efisien (seperti `LazyColumn` jika pakai Compose atau `RecyclerView` jika pakai XML) agar teks panjang tidak menyebabkan *lag*.

### Step 4: Testing & Build (WAJIB)
1. **Formatting:** Jalankan `./gradlew spotlessApply` untuk merapikan format kode Kotlin.
2. **Build Check:** Jalankan `./gradlew assembleDebug` dan pastikan **BUILD SUCCESSFUL**.
3. **Performance Test (Reader):** Masukkan data *dummy string* berupa teks dengan panjang >50.000 kata ke dalam `NovelReader`. Pastikan aplikasi tidak *freeze* (*ANR*) saat merender teks, mengganti ukuran *font*, atau saat *scrolling*.
