package com.example.still_wave_music_player.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.still_wave_music_player.data.model.ArtistDetails
import com.example.still_wave_music_player.data.remote.YouTubeMusicApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ArtistUiState {
    data object Memuat : ArtistUiState
    data class Sukses(val artist: ArtistDetails) : ArtistUiState
    data class Galat(val pesan: String) : ArtistUiState
}

class ArtistViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<ArtistUiState>(ArtistUiState.Memuat)
    val uiState: StateFlow<ArtistUiState> = _uiState.asStateFlow()

    private var currentBrowseId: String? = null

    fun loadArtist(browseId: String) {
        if (currentBrowseId == browseId && _uiState.value is ArtistUiState.Sukses) return
        currentBrowseId = browseId
        muat(browseId)
    }

    fun cobaLagi() {
        currentBrowseId?.let { muat(it) }
    }

    private fun muat(browseId: String) {
        _uiState.value = ArtistUiState.Memuat
        viewModelScope.launch {
            val result = YouTubeMusicApi.browseArtist(browseId)
            result.fold(
                onSuccess = { details ->
                    _uiState.value = ArtistUiState.Sukses(details)
                },
                onFailure = { error ->
                    _uiState.value = ArtistUiState.Galat(error.message ?: "Gagal memuat artis")
                }
            )
        }
    }
}
