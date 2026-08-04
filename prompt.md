# Role
Kamu adalah seorang Senior Android Developer yang ahli dalam bahasa Kotlin, arsitektur MVVM, dan manajemen database lokal (SQLDelight/Room). Kamu sangat familiar dengan basis kode aplikasi open-source sejenis Mihon atau Tachiyomi, khususnya **Aniyomi** (yang mendukung Anime dan Manga).

# Memory
1. **BACA ATURAN MEMORI:**
   - Buka dan baca file `memory_prompt.md` untuk memahami seluruh protokol manajemen memori, pembatasan token, dan aturan penulisan log.
   
# Task
Buatkan implementasi database baru untuk fitur **Search History** (Riwayat Pencarian) di aplikasi Aniyomi. 

# Requirements
1. **Separasi per Extension/Source:** Tiap extension (sumber) harus memiliki riwayat pencarian yang berbeda. Riwayat pencarian dari Source A tidak boleh muncul saat pengguna mencari di Source B.
2. **Dukungan Anime & Manga:** Database dan logic ini harus berlaku dan bisa digunakan oleh *source* Anime maupun Manga.
3. **Fitur Hapus (Delete):** Pengguna harus bisa menghapus riwayat pencarian (bisa hapus satu item spesifik, hapus semua riwayat di satu *source*, atau hapus semua riwayat pencarian secara global).
4. **Limitasi (WAJIB):** Batasi jumlah riwayat yang disimpan per *source* maksimal **8 riwayat terakhir**. Jika ada pencarian baru ke-9, riwayat yang paling lama (berdasarkan `created_at`) harus otomatis terhapus.

---

# Implementation Steps

### Step 1: Database Schema & Entity
Buat skema tabel database `search_history` yang optimal. 
Pastikan memiliki kolom setidaknya: 
- `id` (Primary Key)
- `source_id` (Int/Long, mengacu pada ID extension Anime atau Manga)
- `search_query` (String)
- `created_at` (Timestamp/Long)

### Step 2: DAO / Database Queries
Tuliskan *query* atau DAO (jika pakai Room) / `.sq` file (jika pakai SQLDelight) untuk operasi berikut:
- `insertSearchQuery(sourceId, query)`: Memasukkan data pencarian baru. Jika *query* sudah ada di *source* yang sama, update `created_at` menjadi yang terbaru. **Penting:** Tambahkan *query* atau *logic* untuk menghapus riwayat terlama jika jumlah data di `source_id` tersebut sudah melebihi 8 item setelah *insert*.
- `getSearchHistoryBySource(sourceId)`: Mengambil daftar riwayat berdasarkan `source_id` (maksimal 8), urutkan dari yang paling baru.
- `deleteSearchQuery(id)`: Menghapus satu item riwayat.
- `clearSearchHistoryBySource(sourceId)`: Menghapus semua riwayat di satu *source*.
- `clearAllSearchHistory()`: Menghapus seluruh riwayat pencarian.

### Step 3: Repository & ViewModel Logic
Buat class `SearchHistoryRepository` untuk membungkus operasi database. Lalu, berikan contoh singkat bagaimana fungsi-fungsi ini diintegrasikan dan dipanggil di dalam `SearchViewModel` (saat *user* mengetik pencarian dan saat *user* menekan tombol silang/hapus).

### Step 4: Testing & Clean Code (WAJIB)
 1. Jalankan `./gradlew spotlessApply` untuk merapikan format kode.
 2. Jalankan `./gradlew testDebugUnitTest` dan pastikan **BUILD SUCCESSFUL**.
 3. Simulasikan *error decoding* (misalnya dengan gambar beresolusi sangat besar di perangkat *low-end*) dan verifikasi bahwa *pop-up* peringatan muncul dengan benar tanpa membuat aplikasi *crash*.
