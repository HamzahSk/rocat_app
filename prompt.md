# Role
Kamu adalah seorang Senior Android Developer dan Software Architect yang ahli dalam bahasa Kotlin, arsitektur Clean/MVVM, dan Jetpack Compose/XML. Kamu sangat familiar dengan basis kode aplikasi open-source Aniyomi dan Tachiyomi, khususnya sistem `LocalSource` dan penanganan *file system* / *Storage Access Framework (SAF)*.

# Memory
1. **BACA ATURAN MEMORI:**
   - Buka dan baca file `memory_prompt.md` untuk memahami seluruh protokol manajemen memori, pembatasan token, dan aturan penulisan log secara ketat.

# Task
Ini adalah **Fase 4** dari proyek integrasi fitur **Novel** ke dalam Aniyomi (Fase 1: Database, Fase 2: Source & Reader, Fase 3: Navigasi UI & Explore). 
Tugasmu sekarang adalah menambahkan dukungan **Sumber Lokal (Local Source)** khusus untuk berkas **EPUB** agar pengguna dapat membaca novel yang tersimpan di penyimpanan lokal perangkat mereka.

# Requirements
1. **EPUB Parser & Metadata Extractor:**
   - Implementasikan *parser* EPUB berbasis Kotlin (menggunakan pustaka ringan seperti `epublib` / *custom zip/XML parser*) untuk membaca berkas `.epub`.
   - Ekstrak metadata penting: *Title, Author, Cover Image, Description,* dan *Table of Contents (TOC) / Chapter List*.
2. **Local Novel Source Architecture:**
   - Buat `LocalNovelSource` yang terintegrasi dengan arsitektur `LocalSource` Aniyomi/Tachiyomi.
   - Pindai (*scan*) direktori khusus novel lokal yang dipilih pengguna via *Storage Access Framework (SAF)* atau direktori standar aplikasi.
3. **Integrasi ke Text-Based Novel Reader:**
   - Hubungkan *chapter* bertipe HTML/XHTML dari file EPUB ke `NovelReader` yang dibuat di Fase 2.
   - Tangani pembersihan markup HTML, styling dasar (CSS), dan *asset* gambar *inline* di dalam EPUB agar ter-render dengan sempurna di *Reader UI*.
4. **Sinkronisasi Progress & State:**
   - Simpan posisi membaca, riwayat (*history*), dan status *read/unread* untuk novel EPUB lokal di basis data local.

---

# Implementation Steps

### Step 1: EPUB Parsing Engine
- Buat modul/utilitas `EpubParser` untuk mengekstrak isi berkas `.epub` (arsip ZIP bertipe container `.opf` / XHTML).
- Ambil metadata (`container.xml` -> `.opf`) untuk mendapatkan judul, penulis, sampul (`cover`), dan daftar bab dari manifest/NCX/nav.xhtml.
- Buat metode untuk mengekstrak teks/HTML dari setiap bab (*chapter*) berdasarkan *itemref* pada spine EPUB.

### Step 2: Local Novel Source Implementation
- Buat kelas `LocalNovelSource` yang mengimplementasikan `NovelSource` (dari Fase 2).
- Atur mekanisme pembacaan direktori lokal (misal folder `Aniyomi/novels/` atau lokasi folder kustom yang diizinkan pengguna).
- Petakan setiap file `.epub` atau subfolder novel lokal menjadi entitas `Novel` di dalam basis data.

### Step 3: Reader Asset & Content Handling
- Pada `NovelReaderViewModel`, tambahkan logika untuk memuat teks dari `LocalNovelSource`.
- Pastikan gambar internal EPUB (seperti `OEBPS/images/...`) dapat dimuat secara lokal jika bab novel mengandung gambar ilustrasi.
- Bersihkan tag HTML berlebih yang tidak diperlukan agar tampilan teks tetap konsisten dengan tema/font settings di `NovelReader`.

### Step 4: Testing & Build (WAJIB)
1. **Formatting:** Jalankan `./gradlew spotlessApply` untuk merapikan format kode Kotlin.
2. **Build Check:** Jalankan `./gradlew assembleDebug` dan pastikan **BUILD SUCCESSFUL**.
3. **EPUB Compatibility Test:**
   - Uji dengan berbagai format file EPUB (EPUB2 dan EPUB3, dengan variasi ukuran mulai dari yang sangat kecil hingga batas maksimal **kurang dari 100MB**).
   - Pastikan *sampul novel*, *daftar bab*, dan *konten teks/gambar* muncul secara akurat tanpa *Out of Memory (OOM)*.
   - Pastikan perpindahan bab lokal lancar tanpa memicu kebocoran memori (*memory leak*).
