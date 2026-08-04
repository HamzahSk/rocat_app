# SYSTEM INSTRUCTION: MEMORY MANAGEMENT SYSTEM

Kamu beroperasi di lingkungan GitHub Actions yang bersifat stateless. Untuk menjaga kesinambungan pekerjaan tanpa membuat context window (token) overload, kamu WAJIB mematuhi protokol memori berikut:

---

## 1. Protokol Membaca Konteks (SEBELUM BEKERJA)
1. **Cek Master Index:** Selalu baca file `ai_memory/00_INDEX.md` terlebih dahulu untuk memahami status proyek dan riwayat singkat secara keseluruhan.
2. **Cek Log Terbaru:** Jika butuh detail pekerjaan terakhir, baca **maksimal 2 file log terbaru** di dalam folder `ai_memory/` (berdasarkan urutan waktu/tanggal).
3. **DILARANG** membaca seluruh file di folder `ai_memory/` sekaligus agar context window tidak kehabisan batas.

---

## 2. Protokol Menulis Konteks (SETELAH SELESAI BEKERJA)
Setiap kali kamu selesai melakukan suatu tugas/commit, lakukan 2 hal berikut:

1. **Buat File Log Baru:**
   - Buat file `.md` baru di folder `ai_memory/` dengan format nama: `task_YYYYMMDD_HHMM_[nama_task].md`.
   - Isinya harus singkat (maksimal 150-200 kata) dengan struktur:
     - **Status:** [Selesai / Dalam Proses / Error]
     - **Ringkasan Perubahan:** [Apa saja file/kode yang diubah/dibuat]
     - **Tugas Selanjutnya (Next Steps):** [Apa yang harus dikerjakan di eksekusi berikutnya]

2. **Perbarui `00_INDEX.md`:**
   - Tambahkan baris baru di daftar riwayat `00_INDEX.md` yang mengarah ke file log baru tersebut.
   - Update bagian **"Status Proyek Terkini"** di dalam `00_INDEX.md`.

---

## 3. Protokol Auto-Archive / Rolling Summary (PEMBERSIHAN OTOMATIS)
1. Periksa jumlah file log di dalam `ai_memory/` (tidak termasuk `00_INDEX.md`).
2. **Jika jumlah file log sudah melebihi 10 file:**
   - Gabungkan dan rangkum isi dari 10 log tersebut menjadi 1 file arsip, misalnya: `archive_phase_1.md`.
   - Update `00_INDEX.md` untuk mencatat bahwa log 1-10 telah diarsipkan ke `archive_phase_1.md`.
   - Hapus 10 file log individu yang sudah dirangkum tersebut agar folder `ai_memory/` tetap bersih dan ringan.
