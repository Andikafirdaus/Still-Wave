package com.example.still_wave_music_player.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.still_wave_music_player.data.local.AppDatabase
import com.example.still_wave_music_player.data.local.Playlist
import com.example.still_wave_music_player.data.model.Song
import com.example.still_wave_music_player.data.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel untuk daftar putar.
 */
class PlaylistViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repo = PlaylistRepository(db.playlistDao(), db.playlistSongDao())

    val playlists: Flow<List<Playlist>> = repo.getAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            repo.createPlaylist(name.trim())
        }
    }

    fun renamePlaylist(playlist: Playlist, newName: String) {
        viewModelScope.launch {
            repo.renamePlaylist(playlist, newName.trim())
        }
    }

    fun deletePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            repo.deletePlaylist(playlist)
        }
    }

    fun getSongsForPlaylist(playlistId: Long): Flow<List<Song>> =
        repo.getSongsForPlaylist(playlistId)

    suspend fun isSongInPlaylist(playlistId: Long, song: Song): Boolean =
        repo.isSongInPlaylist(playlistId, song)

    suspend fun getPlaylistById(playlistId: Long): Playlist? =
        repo.getPlaylistById(playlistId)

    fun addSongToPlaylist(
        playlistId: Long,
        song: Song,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val success = repo.addSongToPlaylist(playlistId, song)
            onComplete?.invoke(success)
        }
    }

    fun createPlaylistAndAddSong(
        name: String,
        song: Song,
        onComplete: ((Boolean, String) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val cleanName = name.trim()
            val newId = repo.createPlaylist(cleanName)
            if (newId > 0) {
                val success = repo.addSongToPlaylist(newId, song)
                onComplete?.invoke(success, cleanName)
            } else {
                onComplete?.invoke(false, cleanName)
            }
        }
    }

    fun removeSongFromPlaylist(playlistId: Long, song: Song) {
        viewModelScope.launch {
            repo.removeSongFromPlaylist(playlistId, song)
        }
    }
}
