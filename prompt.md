# Role
Kamu adalah seorang Senior Android Developer dan Software Architect yang ahli dalam bahasa Kotlin dan manajemen database lokal (SQLDelight/Room). Kamu sangat familiar dengan basis kode aplikasi open-source seperti Tachiyomi dan Aniyomi (yang saat ini mendukung Anime dan Manga). 

# Memory
1. **BACA ATURAN MEMORI:**
   - Buka dan baca file `memory_prompt.md` untuk memahami seluruh protokol manajemen memori, pembatasan token, dan aturan penulisan log secara ketat.

# Task
Ini adalah **Fase 1** dari proyek besar untuk mengintegrasikan fitur **Novel** ke dalam Aniyomi. Aplikasi ini nantinya akan mendukung 3 tipe media: Anime, Manga, dan Novel. 
Fokus utama dan **SATU-SATUNYA** tugasmu pada *prompt* ini adalah memodifikasi dan membangun arsitektur *database* (SQLDelight/Room) agar mendukung entitas Novel. **JANGAN** membuat UI, Reader, atau logika Network/Scraper pada tahap ini.

# Requirements
1. **Unified Database:** Modifikasi skema database agar mendukung tipe media baru. Jika saat ini hanya ada flag/enum/tipe untuk `ANIME` dan `MANGA`, tambahkan dukungan untuk `NOVEL`. 
2. **Backward Compatibility:** Semua modifikasi tabel tidak boleh merusak data Library pengguna yang sudah ada. Migrasi database (*database migration*) wajib ditulis dengan sangat hati-hati.
3. **Pemisahan Kategori:** Skema harus bisa membedakan riwayat baca (*history*), pembaruan (*updates*), dan perpustakaan (*library*) antara Anime, Manga, dan Novel dengan efisien.

---

# Implementation Steps

### Step 1: Database Schema & Entity Updates
- Cek struktur tabel utama saat ini (seperti `manga`, `anime`, `history`, `chapter`, `episode`).
- Putuskan pendekatan terbaik: apakah menambahkan tabel baru khusus `novel` dan `novel_chapter`, atau menggabungkannya ke tabel yang sudah ada (misalnya mengubah nama tabel menjadi `library_item` dengan kolom `item_type = 'NOVEL'`).
- Tuliskan *file* skema yang baru (contoh: `.sq` file jika menggunakan SQLDelight atau *data class* beranotasi `@Entity` jika menggunakan Room).

### Step 2: Database Migration Script
- Buat skrip migrasi *database* (misalnya dari versi N ke N+1).
- Pastikan ada logika SQL `ALTER TABLE` yang tepat jika kamu menambahkan kolom baru ke tabel yang sudah *existing*.

### Step 3: DAO / Database Queries (CRUD)
- Tulis *query* dasar (Insert, Update, Delete, Select) untuk mengelola entitas Novel di *database*.
- Buat *query* untuk mengambil daftar Novel berdasarkan kategori (*Library*) dan mengambil daftar *chapter* sebuah Novel.

### Step 4: Testing & Build (WAJIB)
1. **Formatting:** Jalankan `./gradlew spotlessApply` untuk merapikan format kode Kotlin dan SQL.
2. **Build Check:** Jalankan `./gradlew assembleDebug` (atau *task build* yang relevan) dan pastikan **BUILD SUCCESSFUL**.
3. **Migration Test:** Buat *unit test* sederhana untuk menguji apakah migrasi *database* dari versi lama ke versi baru berhasil tanpa menghilangkan *dummy data* Manga/Anime.
