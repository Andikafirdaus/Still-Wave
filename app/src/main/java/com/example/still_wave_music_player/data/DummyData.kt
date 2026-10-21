package com.example.still_wave_music_player.data

import androidx.compose.ui.graphics.Color
import com.example.still_wave_music_player.data.model.Song

/**
 * Model pembungkus lagu tiruan beserta warna sampul solid kalemnya.
 */
data class DummyTrack(
    val song: Song,
    val coverColor: Color
)

/**
 * Himpunan kecil warna solid kalem untuk kartu dan sampul album.
 * Bukan gradien dan bukan warna neon.
 */
val CoverSlate    = Color(0xFF252D38)
val CoverOlive    = Color(0xFF2D3528)
val CoverTaupe    = Color(0xFF382F2A)
val CoverTeal     = Color(0xFF1E3330)
val CoverCharcoal = Color(0xFF26272C)
val CoverPlum     = Color(0xFF332633)

/**
 * Kumpulan data palsu untuk menguji tampilan antarmuka pada Tahap 2.2a.
 */
object DummyData {
    val tracks: List<DummyTrack> = listOf(
        DummyTrack(
            song = Song(
                id = "1",
                title = "Langkah Sunyi",
                artist = "Arka Sena",
                album = "Senandika",
                durationMs = 214000L,
                thumbnailUrl = null
            ),
            coverColor = CoverSlate
        ),
        DummyTrack(
            song = Song(
                id = "2",
                title = "Bicara Senja",
                artist = "Kala Nada",
                album = "Ruang Temu",
                durationMs = 188000L,
                thumbnailUrl = null
            ),
            coverColor = CoverTaupe
        ),
        DummyTrack(
            song = Song(
                id = "3",
                title = "Echoes of Dawn",
                artist = "Mira Vance",
                album = "First Light",
                durationMs = 245000L,
                thumbnailUrl = null
            ),
            coverColor = CoverTeal
        ),
        DummyTrack(
            song = Song(
                id = "4",
                title = "Bayang Waktu",
                artist = "Ruang Teduh",
                album = "Pagi ke Malam",
                durationMs = 202000L,
                thumbnailUrl = null
            ),
            coverColor = CoverOlive
        ),
        DummyTrack(
            song = Song(
                id = "5",
                title = "Jejak Malam",
                artist = "Danu & Sastra",
                album = "Arsip Suara",
                durationMs = 195000L,
                thumbnailUrl = null
            ),
            coverColor = CoverCharcoal
        ),
        DummyTrack(
            song = Song(
                id = "6",
                title = "Hening Sementara",
                artist = "Biru Laut",
                album = "Gelombang Tenang",
                durationMs = 228000L,
                thumbnailUrl = null
            ),
            coverColor = CoverPlum
        ),
        DummyTrack(
            song = Song(
                id = "7",
                title = "Midnight Drift",
                artist = "The Outer Orbit",
                album = "Signals",
                durationMs = 260000L,
                thumbnailUrl = null
            ),
            coverColor = CoverSlate
        ),
        DummyTrack(
            song = Song(
                id = "8",
                title = "Rona Ingatan",
                artist = "Melati Puteri",
                album = "Kembara",
                durationMs = 179000L,
                thumbnailUrl = null
            ),
            coverColor = CoverTaupe
        ),
        DummyTrack(
            song = Song(
                id = "9",
                title = "Arus Tenang",
                artist = "Sinar Petang",
                album = "Fana",
                durationMs = 232000L,
                thumbnailUrl = null
            ),
            coverColor = CoverTeal
        ),
        DummyTrack(
            song = Song(
                id = "10",
                title = "Slow Horizon",
                artist = "Clara Lind",
                album = "Aura",
                durationMs = 210000L,
                thumbnailUrl = null
            ),
            coverColor = CoverOlive
        ),
        DummyTrack(
            song = Song(
                id = "11",
                title = "Pendar Abu",
                artist = "Suara Kamar",
                album = "Catatan Lepas",
                durationMs = 198000L,
                thumbnailUrl = null
            ),
            coverColor = CoverCharcoal
        ),
        DummyTrack(
            song = Song(
                id = "12",
                title = "Sisa Hujan",
                artist = "Langit Kelabu",
                album = "Episode",
                durationMs = 225000L,
                thumbnailUrl = null
            ),
            coverColor = CoverPlum
        )
    )

    val categories: List<Pair<String, Color>> = listOf(
        Pair("Pop Indonesia", CoverTeal),
        Pair("Akustik Santai", CoverTaupe),
        Pair("Fokus Bekerja", CoverSlate),
        Pair("Elektronik", CoverCharcoal),
        Pair("Malam Tenang", CoverPlum),
        Pair("Indie Lokal", CoverOlive),
        Pair("Klasik Modern", CoverSlate),
        Pair("Nuansa Hujan", CoverTeal)
    )

    private val coverPalette = listOf(
        CoverSlate,
        CoverOlive,
        CoverTaupe,
        CoverTeal,
        CoverCharcoal,
        CoverPlum
    )

    /**
     * Mengambil warna sampul solid untuk id lagu tertentu.
     * Menggunakan warna spesifik dari tracks dummy bila ada, atau
     * memetakan hash id lagu asli ke salah satu warna solid dari palet.
     */
    fun getCoverColorForSong(songId: String): Color {
        val dummyMatch = tracks.find { it.song.id == songId }?.coverColor
        if (dummyMatch != null) return dummyMatch

        val index = (songId.hashCode() and 0x7FFFFFFF) % coverPalette.size
        return coverPalette[index]
    }
}

/**
 * Ekstensi praktis untuk mendapatkan warna sampul solid dari objek Song.
 */
val Song.coverColor: Color
    get() = DummyData.getCoverColorForSong(this.id)

