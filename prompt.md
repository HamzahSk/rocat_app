# Role
Kamu adalah seorang Senior Android Developer dan Software Architect yang ahli dalam bahasa Kotlin, arsitektur Clean/MVVM, dan Jetpack Compose/XML. Kamu sangat familiar dengan basis kode aplikasi open-source Aniyomi dan Tachiyomi. 

# Memory
1. **BACA ATURAN MEMORI:**
   - Buka dan baca file `memory_prompt.md` untuk memahami seluruh protokol manajemen memori, pembatasan token, dan aturan penulisan log secara ketat.

# Task
Ini adalah **Fase 3** dari proyek integrasi fitur **Novel** ke dalam Aniyomi (Fase 1: Database, Fase 2: Source & Reader sudah diselesaikan). 
Tugasmu sekarang adalah mengintegrasikan fitur Novel ke dalam navigasi utama aplikasi. Kamu harus menambahkan tab "Novel" di layar *Home* (Library), serta menambahkan bagian "Novel Sources" dan "Novel Extensions" di layar *Explore* (Jelajahi).

# Requirements
1. **Integrasi Bottom Navigation (Home/Library):** Modifikasi navigasi utama agar menampilkan kategori "Novel" berdampingan dengan "Anime" dan "Manga". 
2. **Pembaruan Layar Explore/Browse:** Layar *Explore* saat ini memiliki tab untuk Anime dan Manga. Tambahkan logika dan UI agar aplikasi juga dapat menampilkan tab untuk:
   - **Novel Sources:** Daftar sumber novel yang sudah diinstal dan dipin.
   - **Novel Extensions:** Daftar ekstensi novel yang tersedia untuk diunduh/diinstal (mengambil dari *repository* ekstensi).
3. **Sinkronisasi State & UI:** Pastikan transisi antar tipe media (Anime, Manga, Novel) berjalan mulus tanpa merusak *state* atau *view model* yang sudah ada.

---

# Implementation Steps

### Step 1: Update Main Navigation & Library
- Temukan komponen *Bottom Navigation* utama aplikasi (biasanya di `MainActivity` atau komponen Compose navigasi utama).
- Tambahkan item navigasi/tab untuk **Novel Library**.
- Pastikan tampilan *Library* bisa memfilter dan menampilkan daftar novel yang sudah disimpan oleh pengguna, membedakannya dari *library* anime dan manga.

### Step 2: Update Browse/Explore Screen
- Navigasi ke modul `browse` atau `explore`.
- Modifikasi *Tab Layout* atau *Pager* (baik di XML maupun Compose) untuk menambahkan tab **Novel**.
- Di dalam tab Novel pada layar Browse, bagi lagi menjadi dua sub-layar atau *list*:
  - **Sources:** Menampilkan daftar `NovelSource` yang terinstal (gunakan komponen UI yang sama dengan Manga/Anime sources).
  - **Extensions:** Menampilkan daftar ekstensi novel yang tersedia dari jaringan.

### Step 3: View Model & State Management
- Perbarui `BrowseViewModel` (atau kelas sejenisnya) agar bisa menangani *fetching* dan penyimpanan *state* untuk ekstensi dan *source* novel.
- Pastikan logika instalasi, *update*, dan *uninstall* ekstensi novel berfungsi dengan baik, terpisah dari ekstensi manga/anime.

### Step 4: Testing & Build (WAJIB)
1. **Formatting:** Jalankan `./gradlew spotlessApply` untuk merapikan format kode Kotlin.
2. **Build Check:** Jalankan `./gradlew assembleDebug` dan pastikan **BUILD SUCCESSFUL**.
3. **UI/UX Test:** - Buka aplikasi dan pastikan tab "Novel" muncul di Home.
   - Pindah ke menu *Explore* dan pastikan tab "Novel Source" dan "Novel Extension" muncul dan bisa diklik tanpa *crash*.
