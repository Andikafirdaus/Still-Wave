package com.example.still_wave_music_player.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.still_wave_music_player.data.model.Song

fun Song.getSongKey(): String {
    return when {
        !videoId.isNullOrBlank() -> "yt_$videoId"
        id.startsWith("yt_") -> id
        id.startsWith("local_") -> id
        else -> "local_$id"
    }
}

/**
 * Entitas riwayat pemutaran lagu.
 */
@Entity(
    tableName = "play_history",
    indices = [Index(value = ["playedAt"]), Index(value = ["songKey"])]
)
data class PlayHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val songKey: String,
    val videoId: String? = null,
    val localId: Long? = null,
    val title: String,
    val artist: String,
    val album: String? = null,
    val thumbnailUrl: String? = null,
    val durationMs: Long = 0L,
    val contentUri: String? = null,
    val artistBrowseId: String? = null,
    val albumBrowseId: String? = null,
    val playedAt: Long = System.currentTimeMillis()
)

fun PlayHistory.toSong(): Song {
    return Song(
        id = if (!videoId.isNullOrBlank()) "yt_$videoId" else (localId?.toString() ?: songKey),
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        thumbnailUrl = thumbnailUrl,
        contentUri = contentUri,
        dateAdded = 0L,
        albumId = 0L,
        videoId = videoId,
        artistBrowseId = artistBrowseId,
        albumBrowseId = albumBrowseId
    )
}

fun Song.toPlayHistory(): PlayHistory {
    val key = getSongKey()
    val localIdLong = if (key.startsWith("local_")) {
        key.removePrefix("local_").toLongOrNull()
    } else null

    return PlayHistory(
        songKey = key,
        videoId = videoId,
        localId = localIdLong,
        title = title,
        artist = artist,
        album = album,
        thumbnailUrl = thumbnailUrl,
        durationMs = durationMs,
        contentUri = contentUri,
        artistBrowseId = artistBrowseId,
        albumBrowseId = albumBrowseId,
        playedAt = System.currentTimeMillis()
    )
}

/**
 * Entitas lagu favorit.
 */
@Entity(tableName = "favorite_songs")
data class FavoriteSong(
    @PrimaryKey
    val songKey: String,
    val videoId: String? = null,
    val localId: Long? = null,
    val title: String,
    val artist: String,
    val album: String? = null,
    val thumbnailUrl: String? = null,
    val durationMs: Long = 0L,
    val contentUri: String? = null,
    val artistBrowseId: String? = null,
    val albumBrowseId: String? = null,
    val addedAt: Long = System.currentTimeMillis()
)

fun FavoriteSong.toSong(): Song {
    return Song(
        id = if (!videoId.isNullOrBlank()) "yt_$videoId" else (localId?.toString() ?: songKey),
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        thumbnailUrl = thumbnailUrl,
        contentUri = contentUri,
        dateAdded = 0L,
        albumId = 0L,
        videoId = videoId,
        artistBrowseId = artistBrowseId,
        albumBrowseId = albumBrowseId
    )
}

fun Song.toFavoriteSong(): FavoriteSong {
    val key = getSongKey()
    val localIdLong = if (key.startsWith("local_")) {
        key.removePrefix("local_").toLongOrNull()
    } else null

    return FavoriteSong(
        songKey = key,
        videoId = videoId,
        localId = localIdLong,
        title = title,
        artist = artist,
        album = album,
        thumbnailUrl = thumbnailUrl,
        durationMs = durationMs,
        contentUri = contentUri,
        artistBrowseId = artistBrowseId,
        albumBrowseId = albumBrowseId,
        addedAt = System.currentTimeMillis()
    )
}

/**
 * Entitas daftar putar (Playlist).
 */
@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Entitas lagu dalam daftar putar (PlaylistSong).
 */
@Entity(
    tableName = "playlist_songs",
    indices = [Index(value = ["playlistId"])]
)
data class PlaylistSong(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val playlistId: Long,
    val songKey: String,
    val position: Int,
    val videoId: String? = null,
    val localId: Long? = null,
    val title: String,
    val artist: String,
    val album: String? = null,
    val thumbnailUrl: String? = null,
    val durationMs: Long = 0L,
    val contentUri: String? = null,
    val artistBrowseId: String? = null,
    val albumBrowseId: String? = null
)

fun PlaylistSong.toSong(): Song {
    return Song(
        id = if (!videoId.isNullOrBlank()) "yt_$videoId" else (localId?.toString() ?: songKey),
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        thumbnailUrl = thumbnailUrl,
        contentUri = contentUri,
        dateAdded = 0L,
        albumId = 0L,
        videoId = videoId,
        artistBrowseId = artistBrowseId,
        albumBrowseId = albumBrowseId
    )
}

fun Song.toPlaylistSong(playlistId: Long, position: Int): PlaylistSong {
    val key = getSongKey()
    val localIdLong = if (key.startsWith("local_")) {
        key.removePrefix("local_").toLongOrNull()
    } else null

    return PlaylistSong(
        playlistId = playlistId,
        position = position,
        songKey = key,
        videoId = videoId,
        localId = localIdLong,
        title = title,
        artist = artist,
        album = album,
        thumbnailUrl = thumbnailUrl,
        durationMs = durationMs,
        contentUri = contentUri,
        artistBrowseId = artistBrowseId,
        albumBrowseId = albumBrowseId
    )
}
