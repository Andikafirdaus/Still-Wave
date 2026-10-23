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

    fun addSongToPlaylist(playlistId: Long, song: Song) {
        viewModelScope.launch {
            repo.addSongToPlaylist(playlistId, song)
        }
    }

    fun removeSongFromPlaylist(playlistId: Long, song: Song) {
        viewModelScope.launch {
            repo.removeSongFromPlaylist(playlistId, song)
        }
    }
}
