package com.example.still_wave_music_player.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.still_wave_music_player.data.model.AlbumDetails
import com.example.still_wave_music_player.data.remote.YouTubeMusicApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AlbumUiState {
    data object Memuat : AlbumUiState
    data class Sukses(val album: AlbumDetails) : AlbumUiState
    data class Galat(val pesan: String) : AlbumUiState
}

class AlbumViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<AlbumUiState>(AlbumUiState.Memuat)
    val uiState: StateFlow<AlbumUiState> = _uiState.asStateFlow()

    private var currentBrowseId: String? = null

    fun loadAlbum(browseId: String) {
        if (currentBrowseId == browseId && _uiState.value is AlbumUiState.Sukses) return
        currentBrowseId = browseId
        muat(browseId)
    }

    fun cobaLagi() {
        currentBrowseId?.let { muat(it) }
    }

    private fun muat(browseId: String) {
        _uiState.value = AlbumUiState.Memuat
        viewModelScope.launch {
            val result = YouTubeMusicApi.browseAlbum(browseId)
            result.fold(
                onSuccess = { details ->
                    _uiState.value = AlbumUiState.Sukses(details)
                },
                onFailure = { error ->
                    _uiState.value = AlbumUiState.Galat(error.message ?: "Gagal memuat album")
                }
            )
        }
    }
}
