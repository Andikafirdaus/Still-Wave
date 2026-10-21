# Progress Pengembangan Still Wave Music Player

- [x] Fase 1: Lirik Sinkron (LRCLIB)
- [x] Fase 2: Lagu Berikutnya Otomatis (Radio)
- [ ] Fase 3: Halaman Artis dan Album
- [ ] Fase 4: Room, Riwayat, Favorit, Daftar Putar, Rekomendasi
- [ ] Fase 5: Prefetch dan Crossfade

## Keputusan
- Struktur project sudah lengkap dengan Media3, ExoPlayer, NewPipeExtractor, dan Room 2.6.1 sudah terkonfigurasi di `app/build.gradle.kts`.
- Seluruh pengerjaan akan mengikuti aturan ketat: warna solid tanpa gradien, teks bahasa Indonesia tanpa emoji, log "MusikPlayer" tanpa URL/token/body mentah, defensive parsing.
- Fase 1: Menambahkan klien LRCLIB (dengan pembersihan judul/artis dan fallback pencarian), LrcParser (dukungan multi-timestamp), cache ganda memori & berkas dengan penanda sesi untuk lagu tanpa lirik, serta integrasi UI panel lirik dengan scrolling otomatis dan spring animation. Outer scroll dimatikan saat mode lirik agar gesture tidak berbenturan.
- Fase 2: Menambahkan endpoint YouTubeMusicApi.radio() dengan parsing playlistPanelVideoRenderer dan penangkapan artistBrowseId ("UC")/albumBrowseId ("MPRE"). Antrean PlayerViewModel diberi penanda QueueSource.SINGLE_ONLINE untuk menambahkan radio di latar belakang dan saat sisa antrean <= 2 lagu dengan deduplikasi lagu yang telah diputar pada sesi. Menambahkan SettingsRepository dan SettingsSheet dengan saklar SharedPreferences.

## Ragu
- Belum ada keraguan pada tahap inisialisasi.
