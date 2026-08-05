# Role
Kamu adalah seorang Senior Android Developer dan Software Architect yang ahli dalam bahasa Kotlin, arsitektur Clean/MVVM, dan Jetpack Compose/XML. Kamu sangat familiar dengan basis kode aplikasi open-source Aniyomi dan Tachiyomi.

# Memory
1. **BACA ATURAN MEMORI:**
   - Buka dan baca file `memory_prompt.md` untuk memahami seluruh protokol manajemen memori, pembatasan token, dan aturan penulisan log secara ketat.

# Task
Ini adalah **Fase 6** dari proyek integrasi fitur **Novel** ke dalam Aniyomi. 
Fase ini berfokus murni pada **Bug Fixing Resolusi Dependency & Chapter Parsing**. Saat ini, halaman Detail Novel sudah bisa dibuka, namun daftar *chapter* kosong ("Tidak ada bab yang ditemukan") dan muncul *crash/toast* di UI dengan pesan: 
`NoClassDefFoundError: eu.kanade.tachiyomi.novelsource.util.NovelChapterNumberParser`.

Tugasmu adalah menelusuri mengapa kelas tersebut hilang saat *runtime* dan mengembalikan fungsionalitas *parsing chapter* agar daftar bab muncul dengan benar.

# Requirements
1. **Analisis `NoClassDefFoundError`:**
   - Cari kelas `NovelChapterNumberParser`. Pastikan kelas tersebut benar-benar ada di *package* `eu.kanade.tachiyomi.novelsource.util`.
   - Periksa apakah ini masalah *dependency* antar modul (misal: kelas ada di modul `app` tapi dipanggil oleh modul `source` atau *extension* tanpa konfigurasi gradle yang benar).
2. **Konfigurasi ProGuard / R8:**
   - Jika kelas tersebut diakses menggunakan *reflection* atau merupakan bagian dari API yang digunakan oleh *extension*, pastikan kelas tersebut dilindungi dari *minification/obfuscation*.
   - Tambahkan *rule* yang sesuai di `proguard-rules.pro` jika diperlukan (contoh: `-keep class eu.kanade.tachiyomi.novelsource.** { *; }`).
3. **Perbaikan Logika Fetching Chapter:**
   - Setelah error tersebut ditangani, periksa ulang logika *fetching* daftar bab. Pastikan *parser* dapat mengekstrak nomor bab dan judul dengan benar, lalu meneruskannya ke *UI state*.

---

# Implementation Steps

### Step 1: Penelusuran Kelas & Package
- Buka basis kode dan pastikan file `NovelChapterNumberParser.kt` tersedia dan tidak memiliki *error* saat kompilasi.
- Jika kelas ini seharusnya diadaptasi dari `ChapterNumberParser` milik Manga, pastikan semua referensinya sudah diubah dengan benar.

### Step 2: Perbaikan Build & Dependencies
- Periksa file `build.gradle.kts` pada modul yang relevan. Jika komponen novel berada di modul terpisah, pastikan ia disertakan (`implementation` atau `api`) dengan benar ke modul utama.
- Cek dan sesuaikan aturan ProGuard jika *build* mode rilis/debug dengan *minifyEnabled* membuang kelas tersebut.

### Step 3: Debugging Alur Data
- Telusuri fungsi `fetchChapterList` pada *source* atau *repository* novel. 
- Tambahkan blok `try-catch` sementara atau *logging* tambahan di sekitar pemanggilan `NovelChapterNumberParser` untuk memastikan data HTML/JSON berhasil masuk ke parser sebelum diproses.

### Step 4: Testing & Build (WAJIB)
1. **Formatting:** Jalankan `./gradlew spotlessApply` untuk menjaga standar kode.
2. **Build Check:** Jalankan `./gradlew assembleDebug`.
3. **End-to-End Test (Chapter List):**
   - Buka kembali novel di aplikasi (seperti "The Hunter's Gonna Lay Low" atau novel tes lainnya).
   - Pastikan notifikasi *error* `NoClassDefFoundError` sudah hilang.
   - Pastikan daftar *chapter* termuat penuh (tidak lagi menampilkan "Tidak ada bab yang ditemukan").
