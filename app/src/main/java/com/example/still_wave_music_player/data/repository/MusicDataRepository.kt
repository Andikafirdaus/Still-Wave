package com.example.still_wave_music_player.data.repository

import android.util.Log
import com.example.still_wave_music_player.data.local.FavoriteSong
import com.example.still_wave_music_player.data.local.FavoriteSongDao
import com.example.still_wave_music_player.data.local.PlayHistory
import com.example.still_wave_music_player.data.local.PlayHistoryDao
import com.example.still_wave_music_player.data.local.Playlist
import com.example.still_wave_music_player.data.local.PlaylistDao
import com.example.still_wave_music_player.data.local.PlaylistSong
import com.example.still_wave_music_player.data.local.PlaylistSongDao
import com.example.still_wave_music_player.data.local.getSongKey
import com.example.still_wave_music_player.data.local.toFavoriteSong
import com.example.still_wave_music_player.data.local.toPlayHistory
import com.example.still_wave_music_player.data.local.toPlaylistSong
import com.example.still_wave_music_player.data.local.toSong
import com.example.still_wave_music_player.data.model.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val TAG = "MusikPlayer"

/**
 * Repository untuk riwayat pemutaran.
 */
class HistoryRepository(private val dao: PlayHistoryDao) {
    fun getRecent(limit: Int = 100): Flow<List<Song>> =
        dao.getRecent(limit).map { list -> list.map { it.toSong() } }

    suspend fun addHistory(song: Song) {
        try {
            dao.insert(song.toPlayHistory())
        } catch (e: Exception) {
            Log.e(TAG, "Gagal menyimpan riwayat: ${e.message}")
        }
    }

    suspend fun getRecentSongs(limit: Int = 100): List<Song> {
        return try {
            dao.getRecentSuspend(limit).map { it.toSong() }
        } catch (e: Exception) {
            Log.e(TAG, "Gagal membaca riwayat: ${e.message}")
            emptyList()
        }
    }

    suspend fun getSince(since: Long): List<PlayHistory> {
        return try {
            dao.getSince(since)
        } catch (e: Exception) {
            Log.e(TAG, "Gagal membaca riwayat sejak waktu: ${e.message}")
            emptyList()
        }
    }
}

/**
 * Repository untuk favorit.
 */
class FavoritesRepository(private val dao: FavoriteSongDao) {
    fun getAll(): Flow<List<Song>> =
        dao.getAll().map { list -> list.map { it.toSong() } }

    fun isFavorite(song: Song): Flow<Boolean> =
        dao.isFavorite(song.getSongKey())

    suspend fun isFavoriteSuspend(song: Song): Boolean {
        return try {
            dao.isFavoriteSuspend(song.getSongKey())
        } catch (e: Exception) {
            false
        }
    }

    suspend fun addFavorite(song: Song) {
        try {
            dao.insert(song.toFavoriteSong())
        } catch (e: Exception) {
            Log.e(TAG, "Gagal menambah favorit: ${e.message}")
        }
    }

    suspend fun removeFavorite(song: Song) {
        try {
            dao.deleteByKey(song.getSongKey())
        } catch (e: Exception) {
            Log.e(TAG, "Gagal menghapus favorit: ${e.message}")
        }
    }

    suspend fun toggleFavorite(song: Song): Boolean {
        val was = isFavoriteSuspend(song)
        if (was) removeFavorite(song) else addFavorite(song)
        return !was
    }
}

/**
 * Repository untuk daftar putar.
 */
class PlaylistRepository(
    private val playlistDao: PlaylistDao,
    private val songDao: PlaylistSongDao
) {
    fun getAllPlaylists(): Flow<List<Playlist>> = playlistDao.getAll()

    suspend fun createPlaylist(name: String): Long {
        return try {
            playlistDao.insert(Playlist(name = name))
        } catch (e: Exception) {
            Log.e(TAG, "Gagal membuat daftar putar: ${e.message}")
            -1L
        }
    }

    suspend fun renamePlaylist(playlist: Playlist, newName: String) {
        try {
            playlistDao.update(playlist.copy(name = newName))
        } catch (e: Exception) {
            Log.e(TAG, "Gagal mengganti nama daftar putar: ${e.message}")
        }
    }

    suspend fun deletePlaylist(playlist: Playlist) {
        try {
            songDao.deleteAllForPlaylist(playlist.id)
            playlistDao.delete(playlist)
        } catch (e: Exception) {
            Log.e(TAG, "Gagal menghapus daftar putar: ${e.message}")
        }
    }

    fun getSongsForPlaylist(playlistId: Long): Flow<List<Song>> =
        songDao.getSongsForPlaylist(playlistId).map { list ->
            list.map { it.toSong() }
        }

    suspend fun getSongsForPlaylistSuspend(playlistId: Long): List<Song> {
        return try {
            songDao.getSongsForPlaylistSuspend(playlistId).map { it.toSong() }
        } catch (e: Exception) {
            Log.e(TAG, "Gagal membaca lagu daftar putar: ${e.message}")
            emptyList()
        }
    }

    suspend fun addSongToPlaylist(playlistId: Long, song: Song) {
        try {
            val existing = songDao.getSongsForPlaylistSuspend(playlistId)
            val position = existing.size
            songDao.insert(song.toPlaylistSong(playlistId, position))
        } catch (e: Exception) {
            Log.e(TAG, "Gagal menambah lagu ke daftar putar: ${e.message}")
        }
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, song: Song) {
        try {
            songDao.delete(playlistId, song.getSongKey())
        } catch (e: Exception) {
            Log.e(TAG, "Gagal menghapus lagu dari daftar putar: ${e.message}")
        }
    }
}
