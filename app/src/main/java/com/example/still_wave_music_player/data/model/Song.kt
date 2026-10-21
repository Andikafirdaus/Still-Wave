package com.example.still_wave_music_player.data.model

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val album: String?,
    val durationMs: Long,
    val thumbnailUrl: String?,
    val contentUri: String? = null,
    val dateAdded: Long = 0L,
    val albumId: Long = 0L,
    val videoId: String? = null,
    val artistBrowseId: String? = null,
    val albumBrowseId: String? = null
)

