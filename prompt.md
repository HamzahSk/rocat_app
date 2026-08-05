# Role
Kamu adalah seorang Senior Android Developer dan Software Architect yang ahli dalam bahasa Kotlin, arsitektur Clean/MVVM, dan Jetpack Compose/XML. Kamu sangat familiar dengan basis kode aplikasi open-source Aniyomi dan Tachiyomi.

# Memory
1. **BACA ATURAN MEMORI:**
   - Buka dan baca file `memory_prompt.md` untuk memahami seluruh protokol manajemen memori, pembatasan token, dan aturan penulisan log secara ketat.

# Task
Ini adalah **Fase 7** dari proyek integrasi fitur **Novel** ke dalam Aniyomi. 
Fase ini berfokus pada tiga pilar utama: **Revamp UI Detail Novel**, **Pembuatan Sistem Support Extension Novel**, dan **Implementasi Reader Settings khusus Novel**. 

Referensi utama untuk logika sistem ekstensi dan fitur *reader* adalah folder `NovelApp`. Perlu diingat bahwa `NovelApp` mungkin menggunakan bahasa pemrograman lain (seperti Dart/Java/Swift), sehingga tugas utamamu adalah **memahami logikanya dan menerjemahkannya ke dalam Kotlin modern dan ekosistem Jetpack Compose/MVVM milik Aniyomi**.

# Requirements

1. **Revamp UI Detail Novel (Jetpack Compose):**
   - Rombak tampilan halaman Detail Novel agar senada dan mirip dengan halaman detail dari *source* Manga dan Anime (`MangaInfoScreen` / `AnimeInfoScreen`).
   - Gunakan komponen *composable* yang sudah ada di Aniyomi (seperti *header cover*, tombol aksi, dan *collapsing toolbar*) lalu sesuaikan *state* datanya menggunakan `NovelInfoViewModel`.

2. **Support Extension Novel:**
   - Pelajari folder `NovelApp` terkait bagaimana mereka mendefinisikan *source* novel dan melakukan *scraping/parsing* data.
   - Buat *interface* atau kelas abstrak di Kotlin (misal: `NovelHttpSource`) yang kompatibel dengan sistem `ExtensionManager` Aniyomi.
   - Pastikan aplikasi kini bisa mengenali dan memuat ekstensi bertipe `NOVEL`.

3. **Novel Reader & Settings:**
   - Pelajari fitur *reader* dari `NovelApp` dan adaptasi logikanya ke dalam Compose (`LazyColumn` dengan *Native Text* direkomendasikan untuk performa yang baik).
   - Tambahkan *Setting* / *Preference* baru khusus *Novel Reader* di `PreferenceStore` yang mencakup:
     * **Text Size** (Ukuran font)
     * **Font Family** (Pilihan jenis font)
     * **Background Theme / Color** (Terang, Gelap, Sepia)
     * **Line Spacing** (Jarak antar baris)
     * **Padding/Margin** (Jarak teks ke layar)
   - Buat UI *Bottom Sheet* di halaman *Reader* agar pengguna bisa mengubah pengaturan ini secara *real-time*.

---

# Implementation Steps

### Step 1: Analisis & Translasi Kode `NovelApp`
- Buka dan telusuri folder `NovelApp`. Pahami alur *fetching* ekstensi dan manipulasi UI *reader*-nya.
- Terjemahkan logika *scraping/parsing* dan *settings* tersebut ke dalam Kotlin, sesuaikan dengan standar Clean Architecture Aniyomi.

### Step 2: Rombak UI Detail Screen
- Modifikasi UI Detail Novel agar menggunakan Jetpack Compose dengan struktur yang identik seperti `MangaInfoScreen`. 
- Pastikan daftar chapter yang sudah diperbaiki pada Fase 6 muncul dengan rapi di bawah informasi detail novel.

### Step 3: Pembuatan Reader & Settings
- Daftarkan variabel pengaturan novel baru ke dalam *DataStore* atau *Preferences* bawaan Aniyomi.
- Bangun UI Compose untuk *Reader Novel* yang bereaksi langsung terhadap perubahan *state* pengaturan (ukuran font, warna, dll).

### Step 4: Testing & Build (WAJIB)
1. **Formatting:** Jalankan perintah `./gradlew spotlessApply` untuk memastikan seluruh kode Kotlin mengikuti standar *style guide*.
2. **Build Check:** Jalankan `./gradlew assembleDebug` dan pastikan tidak ada *error* saat kompilasi.
3. **Validasi Logika:** - Pastikan tidak ada *error* saat kompilasi.
   - Pastikan tidak ada *unresolved references* terkait UI Compose yang baru dibuat maupun *DataStore* preferensi yang baru ditambahkan.