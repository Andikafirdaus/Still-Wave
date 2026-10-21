package com.example.still_wave_music_player.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.still_wave_music_player.data.model.Song
import com.example.still_wave_music_player.data.remote.YouTubeMusicApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Status UI untuk pencarian online YouTube Music.
 */
sealed interface OnlineSearchUiState {
    data object Kosong : OnlineSearchUiState
    data object Memuat : OnlineSearchUiState
    data class Berisi(val daftar: List<Song>) : OnlineSearchUiState
    data object TidakAda : OnlineSearchUiState
    data class Galat(val pesan: String) : OnlineSearchUiState
}

/**
 * ViewModel untuk menangani pencarian lagu online dengan debounce 500 ms.
 */
class OnlineSearchViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<OnlineSearchUiState>(OnlineSearchUiState.Kosong)
    val uiState: StateFlow<OnlineSearchUiState> = _uiState.asStateFlow()

    private var currentQuery: String = ""
    private var searchJob: Job? = null

    /**
     * Dipanggil setiap kali kueri pencarian berubah.
     * Menggunakan debounce 500 ms dan membatalkan pekerjaan sebelumnya.
     */
    fun onQueryChanged(query: String) {
        currentQuery = query
        searchJob?.cancel()

        val trimmed = query.trim()
        if (trimmed.length < 2) {
            _uiState.value = OnlineSearchUiState.Kosong
            return
        }

        searchJob = viewModelScope.launch {
            delay(500)
            jalankanPencarian(trimmed)
        }
    }

    /**
     * Mencoba ulang pencarian terakhir yang gagal atau tertunda.
     */
    fun cobaLagi() {
        val trimmed = currentQuery.trim()
        if (trimmed.length >= 2) {
            searchJob?.cancel()
            searchJob = viewModelScope.launch {
                jalankanPencarian(trimmed)
            }
        }
    }

    private suspend fun jalankanPencarian(kueri: String) {
        _uiState.value = OnlineSearchUiState.Memuat
        val result = YouTubeMusicApi.cari(kueri)
        result.fold(
            onSuccess = { list ->
                if (list.isEmpty()) {
                    _uiState.value = OnlineSearchUiState.TidakAda
                } else {
                    _uiState.value = OnlineSearchUiState.Berisi(list)
                }
            },
            onFailure = { error ->
                _uiState.value = OnlineSearchUiState.Galat(
                    error.message ?: "Gagal memuat hasil"
                )
            }
        )
    }
}
