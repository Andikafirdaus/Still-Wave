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
