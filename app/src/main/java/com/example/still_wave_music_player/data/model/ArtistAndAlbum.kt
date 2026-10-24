package com.example.still_wave_music_player.data.model

/**
 * Data hasil pencarian artis YouTube Music.
 */
data class ArtistSearchResult(
    val browseId: String,
    val name: String,
    val thumbnailUrl: String? = null
)

/**
 * Data hasil pencarian album YouTube Music.
 */
data class AlbumSearchResult(
    val browseId: String,
    val title: String,
    val artist: String,
    val year: String? = null,
    val thumbnailUrl: String? = null
)

/**
 * Rincian halaman album YouTube Music.
 */
data class AlbumDetails(
    val browseId: String,
    val title: String,
    val artist: String,
    val artistBrowseId: String? = null,
    val year: String? = null,
    val thumbnailUrl: String? = null,
    val songs: List<Song> = emptyList()
)

/**
 * Rincian halaman artis YouTube Music.
 */
data class ArtistDetails(
    val browseId: String,
    val name: String,
    val thumbnailUrl: String? = null,
    val listeners: String? = null,
    val topSongs: List<Song> = emptyList(),
    val albums: List<AlbumSearchResult> = emptyList(),
    val singles: List<AlbumSearchResult> = emptyList()
)

/**
 * Tipe entitas untuk Hasil Teratas pencarian "Semua".
 */
enum class TopResultType {
    LAGU, ARTIS, ALBUM
}

/**
 * Hasil Teratas kartu utama di tab "Semua".
 */
data class TopResult(
    val type: TopResultType,
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val thumbnailUrl: String? = null,
    val song: Song? = null,
    val artist: ArtistSearchResult? = null,
    val album: AlbumSearchResult? = null
)

/**
 * Hasil gabungan pencarian "Semua": hasil teratas, lagu, artis, dan album.
 */
data class SearchAllResult(
    val topResult: TopResult? = null,
    val songs: List<Song> = emptyList(),
    val artists: List<ArtistSearchResult> = emptyList(),
    val albums: List<AlbumSearchResult> = emptyList()
)
