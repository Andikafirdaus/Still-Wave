# Still Wave

Pemutar musik Android pribadi yang gratis, tanpa iklan, dan tanpa akun. Memutar lagu dari YouTube Music maupun dari penyimpanan HP, dengan lirik sinkron, mode dengar bareng, dan tampilan gelap yang bersih.

> Status: dalam pengembangan aktif. Dibuat untuk pemakaian pribadi dan pembelajaran.

![Platform](https://img.shields.io/badge/platform-Android%208.0%2B-3ddc84)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2-7f52ff)
![UI](https://img.shields.io/badge/UI-Jetpack%20Compose-4285f4)
![Status](https://img.shields.io/badge/status-in%20development-orange)
[![Rilis terbaru](https://img.shields.io/github/v/release/Andikafirdaus/Still-Wave?label=rilis)](https://github.com/Andikafirdaus/Still-Wave/releases/latest)

## Unduh

Ingin langsung mencoba tanpa membangun sendiri? Unduh APK terbaru di halaman rilis:

**[Unduh Still Wave (Releases)](https://github.com/Andikafirdaus/Still-Wave/releases/latest)**

Cara memasang:

1. Buka halaman rilis di atas, lalu unduh berkas `.apk` pada bagian **Assets** di rilis terbaru.
2. Buka berkas yang sudah diunduh di HP-mu.
3. Jika diminta, izinkan pemasangan dari sumber ini (**Izinkan dari sumber ini** / **Install unknown apps**) di pengaturan Android.
4. Ketuk **Pasang** (Install), lalu buka aplikasi **Still Wave**.

Kebutuhan: Android 8.0 (API 26) atau lebih baru dan koneksi internet. Untuk memperbarui, unduh APK rilis yang lebih baru lalu pasang di atas aplikasi yang lama; data tetap aman selama tanda tangan aplikasinya sama.

> Fitur yang masih dalam pengembangan bisa belum tersedia di versi rilis. Lihat catatan di setiap rilis dan bagian [Peta jalan](#peta-jalan).

---

## Daftar isi

- [Unduh](#unduh)
- [Tentang](#tentang)
- [Tampilan](#tampilan)
- [Fitur](#fitur)
- [Cara kerja singkat](#cara-kerja-singkat)
- [Teknologi](#teknologi)
- [Struktur proyek](#struktur-proyek)
- [Menjalankan proyek](#menjalankan-proyek)
- [Mengaktifkan Listening Together](#mengaktifkan-listening-together)
- [Peta jalan](#peta-jalan)
- [Prinsip desain](#prinsip-desain)
- [Catatan penting](#catatan-penting)
- [Kredit dan inspirasi](#kredit-dan-inspirasi)
- [Lisensi](#lisensi)

---

## Tentang

Still Wave dibuat dari nol dengan Kotlin dan Jetpack Compose. Tujuannya sederhana: pemutar musik yang nyaman dipakai sehari-hari, gratis, dan bisa disesuaikan kapan saja kalau layanan sumbernya berubah aturan, karena kodenya dimiliki sendiri.

Yang membedakan:

- **Sumber ganda.** Lagu online dari YouTube Music dan lagu lokal dari HP dalam satu perpustakaan.
- **Dengar bareng.** Dua orang bisa mendengarkan lagu yang sama dalam waktu yang sinkron.
- **Lirik berlapis.** Lirik sinkron per baris, dengan beberapa sumber cadangan bila lagunya tidak ditemukan.
- **Tanpa akun.** Semua data (favorit, daftar putar, riwayat) tersimpan lokal di perangkat.
- **Warna solid.** Tampilan gelap dengan warna solid dan satu aksen merah, tanpa gradien.

## Tampilan

<!-- Tambahkan tangkapan layar ke folder docs/screenshots/ lalu aktifkan tabel di bawah. -->

<!--
| Beranda | Pencarian | Pemutar | Lirik |
|---|---|---|---|
| ![Beranda](docs/screenshots/beranda.png) | ![Cari](docs/screenshots/cari.png) | ![Pemutar](docs/screenshots/pemutar.png) | ![Lirik](docs/screenshots/lirik.png) |
-->

Antarmuka sepenuhnya berbahasa Indonesia, dengan bilah navigasi bawah yang melayang (Beranda, Pustaka, Cari) dan mini player di atasnya.

## Fitur

Centang berarti sudah ada di aplikasi. Yang belum dicentang masih direncanakan (lihat [Peta jalan](#peta-jalan)).

### Pemutaran

- [x] Memutar lagu online dari YouTube Music (streaming)
- [x] Memutar lagu lokal dari penyimpanan HP
- [x] Pemutar penuh dengan progres yang bisa digeser, putar/jeda, lagu sebelum/berikutnya, acak, dan ulang
- [x] Mini player yang selalu ada di bawah layar
- [x] Pemutaran latar belakang dengan kontrol di notifikasi, dan ketukan notifikasi membuka aplikasi
- [x] Antrean lagu dengan "Putar berikutnya"
- [x] Putar otomatis lagu serupa (radio) saat memutar lagu tunggal
- [x] Jeda otomatis saat headset dicabut dan penanganan audio focus
- [x] Timer tidur (termasuk sampai lagu selesai)

### Pencarian dan penemuan

- [x] Cari lagu, artis, dan album, dengan kartu "Hasil teratas"
- [x] Halaman pencarian penuh dengan riwayat pencarian dan saran otomatis saat mengetik
- [x] Halaman artis dan album
- [x] Jelajah suasana (Sedih, Galau, Santai, Semangat, Fokus, Romantis, Pesta, Tidur, dan lainnya)
- [x] Bagian "Untuk kamu" dan "Baru diputar" di Beranda

### Pustaka

- [x] Empat tab: Di HP, Favorit, Daftar putar, dan Riwayat
- [x] Kolom cari di Pustaka
- [x] Menu tekan-lama pada lagu: putar berikutnya, tambah ke daftar putar, bagikan lagu, buka artis/album
- [x] Bagikan lagu lewat menu berbagi Android

### Lirik

- [x] Lirik sinkron per baris dengan penyorotan baris aktif dan gulir otomatis
- [x] Sumber utama LRCLIB, dengan pencarian ulang memakai judul yang sudah dibersihkan
- [x] Sumber cadangan: lirik bawaan YouTube Music dan lyrics.ovh (lirik polos, tanpa penyorotan)
- [x] Cache lirik lokal agar tidak mengambil ulang

### Dengar bareng (Listening Together)

- [x] Dua orang mendengarkan lagu yang sama secara sinkron
- [x] Koreksi selisih waktu otomatis (drift correction) dan pencegahan gema perintah
- [x] Tersambung lewat Firebase Realtime Database (REST), tanpa SDK Firebase

### Tampilan dan pengalaman

- [x] Tema gelap dengan warna solid dan aksen merah
- [x] Bilah navigasi bawah melayang
- [x] Ikon aplikasi adaptif dengan dukungan ikon bertema (Android 13+)
- [ ] Halaman Pengaturan penuh (sedang dikerjakan)
- [ ] Halaman suasana berisi playlist ala Spotify (direncanakan)

## Cara kerja singkat

1. **Pencarian dan metadata.** Aplikasi memanggil InnerTube milik YouTube Music (klien WEB_REMIX) untuk pencarian, saran, halaman artis dan album, serta lagu serupa.
2. **Pemutaran online.** Aliran audio diambil lewat NewPipeExtractor dengan PoToken yang dihasilkan WebView tanpa tampilan. Media3 ExoPlayer memutarnya lewat `ResolvingDataSource` dengan skema internal `ytm://<videoId>`, sehingga alamat audio baru diambil saat benar-benar dibutuhkan.
3. **Layanan pemutar.** `MediaSessionService` menjaga pemutaran tetap hidup di latar belakang; antarmuka berbicara dengannya lewat `MediaController`.
4. **Data lokal.** Room menyimpan favorit, daftar putar, riwayat putar, dan cache lirik. Riwayat pencarian dan pengaturan disimpan lokal.
5. **Lirik.** Rantai sumber dicoba berurutan sampai ada yang berhasil; hasilnya di-cache.
6. **Dengar bareng.** Perintah putar/jeda/geser dan lagu aktif dikirim lewat Firebase Realtime Database dan diterima secara langsung (SSE). Jam dua perangkat diselaraskan dengan offset waktu agar posisi lagu tetap sama.

## Teknologi

| Bidang | Pustaka |
|---|---|
| Bahasa dan UI | Kotlin 2.2, Jetpack Compose |
| Pemutar audio | AndroidX Media3 (ExoPlayer, MediaSession) |
| Ekstraksi YouTube | NewPipeExtractor v0.26.3 + PoToken |
| Metadata YouTube Music | InnerTube (WEB_REMIX) |
| Jaringan | OkHttp |
| Database lokal | Room 2.7.0 |
| Gambar | Coil 2.7.0 |
| Lirik | LRCLIB, YouTube Music, lyrics.ovh |
| Dengar bareng | Firebase Realtime Database (REST, tanpa SDK) |
| Font lirik | Inter Bold (SIL Open Font License) |
| Build | Android Gradle Plugin 9, minSdk 26 |

## Struktur proyek

```
app/src/main/java/com/example/still_wave_music_player/
├── data/
│   ├── local/        Room: database, entity, DAO
│   ├── model/        model data (Song, dll.)
│   ├── remote/       YouTubeMusicApi, LrclibApi, LyricsOvhApi
│   └── repository/   LyricsRepository, SettingsRepository, SearchHistoryRepository
├── together/         Listening Together (manager, preferensi, sheet)
├── ui/
│   ├── components/   FloatingNavBar, SearchBarField, SongOptionsSheet, AppIcons
│   ├── navigation/   rute dan layar
│   ├── screens/      Home, Search, Library, Player, dan lainnya
│   └── theme/        warna, tipografi
├── utils/            fungsi bantu (mis. format durasi)
└── viewmodels/       PlayerViewModel, OnlineSearchViewModel, dan lainnya
```

> Struktur bisa berubah seiring pengembangan.

## Menjalankan proyek

**Kebutuhan**

- Android Studio versi terbaru dengan JDK 17 atau lebih baru
- Perangkat atau emulator Android 8.0 (API 26) ke atas
- Koneksi internet untuk lagu online dan lirik

**Langkah**

```bash
git clone https://github.com/Andikafirdaus/Still-Wave.git
cd Still-Wave
```

1. Buka folder di Android Studio, lalu tunggu **Sync** Gradle selesai.
2. Pilih perangkat atau emulator, lalu tekan **Run**.

Atau lewat terminal:

```bash
# Linux / macOS
./gradlew assembleDebug

# Windows
.\gradlew.bat assembleDebug
```

APK debug berada di `app/build/outputs/apk/debug/`.

## Mengaktifkan Listening Together

Fitur ini membutuhkan Firebase Realtime Database milikmu sendiri (gratis di paket Spark).

1. Buat proyek di [Firebase Console](https://console.firebase.google.com/), lalu buat **Realtime Database**.
2. Salin alamat databasenya (berbentuk `https://<nama-proyek>-default-rtdb.<region>.firebasedatabase.app`).
3. Isi alamat itu pada `DEFAULT_SERVER_URL` di `together/TogetherPreferences.kt`.
4. Atur aturan database (Rules) sesuai kebutuhan keamananmu.

> Jangan menyimpan alamat atau kunci pribadi di repositori publik jika kamu tidak ingin orang lain memakainya.

## Peta jalan

**Sedang dikerjakan**

- [ ] Halaman Pengaturan penuh (pemutaran, lirik, penyimpanan, nama panggilan Listening Together, tentang)
- [ ] Kartu suasana yang lebih menarik
- [ ] Suasana sebagai kumpulan playlist dengan rekomendasi yang menyesuaikan selera

**Berikutnya**

- [ ] Impor playlist dari YouTube Music dan Spotify
- [ ] Rekap lagu teratas
- [ ] Cache lagu dan mode hemat kuota
- [ ] Crossfade antar lagu
- [ ] Widget layar utama
- [ ] Menu "Bareng" di bilah navigasi
- [ ] Perombakan tampilan bergaya kaca (liquid glass)
- [ ] Ekspor dan impor cadangan data
- [ ] Pembaruan aplikasi otomatis
- [ ] Animasi sampul dan warna dinamis

**Ditunda atau dibatalkan**

- Motion artwork seperti Apple Music: ditunda karena sumbernya tidak resmi dan rapuh.
- Lewati bagian diam: dibatalkan.

## Prinsip desain

- Warna solid, tanpa gradien.
- Satu aksen merah pada tema gelap.
- Seluruh teks antarmuka dalam Bahasa Indonesia, tanpa emoji.
- Konten tidak boleh tertutup bilah navigasi melayang atau mini player.
- Animasi halus dan fungsional, bukan hiasan.

## Catatan penting

- Still Wave adalah proyek pribadi dan **tidak berafiliasi** dengan YouTube, Google, Apple, atau Spotify.
- Aplikasi memakai layanan dan endpoint tidak resmi, jadi sewaktu-waktu bisa berhenti bekerja kalau layanan sumbernya berubah. Perbaikannya dilakukan di kode ini.
- Ditujukan untuk pemakaian pribadi dan pembelajaran. Hormati hak cipta dan ketentuan layanan masing-masing platform.
- Proyek sedang berkembang, jadi bug mungkin masih ada.

## Kredit dan inspirasi

- [NewPipeExtractor](https://github.com/TeamNewPipe/NewPipeExtractor) untuk ekstraksi aliran audio
- [LRCLIB](https://lrclib.net) dan [lyrics.ovh](https://lyrics.ovh) untuk sumber lirik
- [Convx](https://github.com/cosmictaserdev-creator/Convx) sebagai referensi cara memutar audio online dan ide fitur
- [musicfy](https://github.com/realidkroo/musicfy) sebagai inspirasi tampilan
- AndroidX Media3, Jetpack Compose, Room, Coil, OkHttp
- Font [Inter](https://rsms.me/inter/) di bawah SIL Open Font License

## Lisensi

Proyek ini memakai NewPipeExtractor yang berlisensi GPL-3.0, sehingga proyek ini sebaiknya dirilis dengan lisensi **GPL-3.0** juga. Tambahkan berkas `LICENSE` berisi teks GPL-3.0 di akar repositori (GitHub menyediakan templatenya saat menambah berkas baru bernama `LICENSE`).

---

Dibuat oleh [Andika Firdaus](https://github.com/Andikafirdaus).
