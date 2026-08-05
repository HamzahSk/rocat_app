# Role
Kamu adalah seorang Senior Android Developer dan Software Architect yang ahli dalam bahasa Kotlin, arsitektur Clean/MVVM, dan Jetpack Compose/XML. Kamu sangat familiar dengan basis kode aplikasi open-source Aniyomi dan Tachiyomi.

# Memory
1. **BACA ATURAN MEMORI:**
   - Buka dan baca file `memory_prompt.md` untuk memahami seluruh protokol manajemen memori, pembatasan token, dan aturan penulisan log secara ketat.

# Task
Ini adalah **Fase 5** dari proyek integrasi fitur **Novel** ke dalam Aniyomi. 
Fase ini berfokus pada **Bug Fixing, Referensi Silang, dan Penyempurnaan Alur UI**. Tugas utamamu adalah mengambil referensi kode dari folder `NovelApp`, memperbaiki *bug* di mana *Local Novel* tidak bisa diklik, dan melengkapi seluruh menu standar layaknya fitur Anime dan Manga.

# Requirements
1. **Referensi Kode `NovelApp`:**
   - Buka folder `NovelApp` dan pelajari bagaimana fitur-fiturnya diimplementasikan. Adaptasikan logika yang relevan (seperti *fetching*, struktur *filter*, atau detail UI) ke dalam *codebase* Aniyomi saat ini.
2. **Perbaikan Bug Local Novel:**
   - Saat ini, item novel dari sumber lokal (EPUB) muncul namun **tidak bisa diklik**.
   - Tiru dan adaptasi metode klik, *intent*, atau *routing* dari implementasi `LocalAnime` atau `LocalManga` di Aniyomi agar pengguna bisa masuk ke halaman *Detail Novel* saat mengklik novel lokal.
3. **Kelengkapan Fitur & Menu Source:**
   - Pastikan layar *Browse/Source* untuk Novel memiliki menu dan fungsionalitas yang setara dengan Anime/Manga.
   - Fitur wajib yang harus berfungsi:
     - **Popular:** Menampilkan daftar novel populer dari *source*.
     - **Latest:** Menampilkan daftar novel update terbaru dari *source*.
     - **Filter:** Sistem filter (berdasarkan *genre*, status, dll.) yang berfungsi dengan baik saat mencari novel.
4. **Penyempurnaan Detail & Reader:**
   - Pastikan halaman **Detail Novel** menampilkan informasi lengkap (Cover, Author, Sinopsis, Status) dan **Daftar Chapter**.
   - Pastikan klik pada *chapter* langsung mengarah ke **Novel Reader** (dari Fase 2 & 4) tanpa *crash*.

---

# Implementation Steps

### Step 1: Code Review & Bug Fixing (Local Source)
- Analisis *adapter* atau komponen Compose yang menangani klik pada daftar novel lokal.
- Bandingkan dengan cara kerja klik pada `LocalManga` dan perbaiki *routing* navigasinya sehingga mengarah ke `NovelDetailController` atau layar detail yang setara.

### Step 2: Source Menus Integration
- Implementasikan *endpoint* atau fungsi `fetchPopularNovel`, `fetchLatestNovel`, dan `fetchSearchNovel` (dengan filter) pada ekstensi/source novel.
- Hubungkan fungsi-fungsi ini ke antarmuka `Browse` agar tab **Populer**, **Terbaru**, dan **Filter** muncul dan beroperasi normal.

### Step 3: Novel Detail & Chapter List
- Sempurnakan UI `NovelDetailScreen`. Pastikan pengambilan data *metadata* novel dari `NovelApp` terintegrasi dengan baik ke *state holder* / *ViewModel* Aniyomi.
- Sinkronkan status *Read/Unread* pada daftar *chapter*.

### Step 4: Testing & Build (WAJIB)
1. **Formatting:** Jalankan `./gradlew spotlessApply` untuk merapikan kode.
2. **Build Check:** Jalankan `./gradlew assembleDebug` dan pastikan **BUILD SUCCESSFUL**.
3. **End-to-End Test:**
   - Buka *Local Novel*, klik itemnya, dan pastikan masuk ke halaman Detail.
   - Buka *Novel Source*, uji tab *Popular*, *Latest*, dan coba gunakan *Filter*.
   - Buka satu novel dari *source*, baca satu bab, kembali ke halaman detail, dan pastikan *progress* membaca tersimpan.
