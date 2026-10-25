package com.example.still_wave_music_player.together

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.example.still_wave_music_player.data.model.Song

/**
 * State pemutaran sinkronisasi ruang (Listening Together).
 */
data class RoomState(
    val videoId: String = "",
    val index: Int = 0,
    val positionMs: Long = 0L,
    val isPlaying: Boolean = false,
    val atServerMs: Long = 0L,
    val by: String = "",
    val rev: Long = 0L
)

/**
 * Representasi lagu online dalam antrean bersama ruang.
 */
data class RoomSong(
    val videoId: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val thumbnailUrl: String? = null,
    val durationMs: Long = 0L,
    val artistBrowseId: String? = null,
    val albumBrowseId: String? = null
) {
    fun toSong(): Song {
        return Song(
            id = "yt_$videoId",
            title = title,
            artist = artist,
            album = album ?: "",
            durationMs = durationMs,
            thumbnailUrl = thumbnailUrl,
            contentUri = null,
            dateAdded = 0L,
            albumId = 0L,
            videoId = videoId,
            artistBrowseId = artistBrowseId,
            albumBrowseId = albumBrowseId
        )
    }

    companion object {
        fun fromSong(song: Song): RoomSong? {
            val vId = song.videoId?.takeIf { it.isNotBlank() } ?: return null
            return RoomSong(
                videoId = vId,
                title = song.title,
                artist = song.artist,
                album = song.album?.takeIf { it.isNotBlank() },
                thumbnailUrl = song.thumbnailUrl?.takeIf { it.isNotBlank() },
                durationMs = song.durationMs,
                artistBrowseId = song.artistBrowseId?.takeIf { it.isNotBlank() },
                albumBrowseId = song.albumBrowseId?.takeIf { it.isNotBlank() }
            )
        }
    }
}

fun Song.toRoomSong(): RoomSong? = RoomSong.fromSong(this)

/**
 * Fungsi bantu pembuat MediaItem lagu online (mediaId = videoId, uri "ytm://<videoId>",
 * metadata judul, artis, album, artworkUri) yang dipakai bersama oleh PlayerViewModel,
 * MusicService, dan ListenTogetherManager tanpa mengubah perilaku.
 */
fun createOnlineMediaItem(
    videoId: String,
    title: String,
    artist: String,
    album: String? = null,
    thumbnailUrl: String? = null
): MediaItem {
    val metaBuilder = MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(artist)

    if (!album.isNullOrBlank()) {
        metaBuilder.setAlbumTitle(album)
    }
    if (!thumbnailUrl.isNullOrBlank()) {
        metaBuilder.setArtworkUri(Uri.parse(thumbnailUrl))
    }

    val onlineUri = Uri.parse("ytm://$videoId")
    return MediaItem.Builder()
        .setMediaId(videoId)
        .setUri(onlineUri)
        .setRequestMetadata(
            MediaItem.RequestMetadata.Builder()
                .setMediaUri(onlineUri)
                .build()
        )
        .setMediaMetadata(metaBuilder.build())
        .build()
}

fun createOnlineMediaItem(song: Song): MediaItem {
    val vId = song.videoId?.takeIf { it.isNotBlank() } ?: song.id.removePrefix("yt_")
    return createOnlineMediaItem(
        videoId = vId,
        title = song.title,
        artist = song.artist,
        album = song.album,
        thumbnailUrl = song.thumbnailUrl
    )
}

fun createOnlineMediaItem(roomSong: RoomSong): MediaItem {
    return createOnlineMediaItem(
        videoId = roomSong.videoId,
        title = roomSong.title,
        artist = roomSong.artist,
        album = roomSong.album,
        thumbnailUrl = roomSong.thumbnailUrl
    )
}
