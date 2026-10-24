package com.example.still_wave_music_player.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.still_wave_music_player.data.model.AlbumSearchResult
import com.example.still_wave_music_player.data.model.ArtistSearchResult
import com.example.still_wave_music_player.data.model.SearchAllResult
import com.example.still_wave_music_player.data.model.Song
import com.example.still_wave_music_player.data.remote.YouTubeMusicApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Kategori pencarian online YouTube Music.
 */
enum class OnlineSearchCategory(val label: String) {
    SEMUA("Semua"),
    LAGU("Lagu"),
    ARTIS("Artis"),
    ALBUM("Album")
}

/**
 * Status UI untuk pencarian online YouTube Music.
 */
sealed interface OnlineSearchUiState {
    data object Kosong : OnlineSearchUiState
    data object Memuat : OnlineSearchUiState
    data class BerisiSemua(val hasil: SearchAllResult) : OnlineSearchUiState
    data class Berisi(val daftar: List<Song>) : OnlineSearchUiState
    data class BerisiArtis(val daftar: List<ArtistSearchResult>) : OnlineSearchUiState
    data class BerisiAlbum(val daftar: List<AlbumSearchResult>) : OnlineSearchUiState
    data object TidakAda : OnlineSearchUiState
    data class Galat(val pesan: String) : OnlineSearchUiState
}

/**
 * ViewModel untuk menangani pencarian lagu, artis, dan album online dengan debounce 500 ms.
 */
class OnlineSearchViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<OnlineSearchUiState>(OnlineSearchUiState.Kosong)
    val uiState: StateFlow<OnlineSearchUiState> = _uiState.asStateFlow()

    private val _selectedCategory = MutableStateFlow(OnlineSearchCategory.SEMUA)
    val selectedCategory: StateFlow<OnlineSearchCategory> = _selectedCategory.asStateFlow()

    private var currentQuery: String = ""
    private var searchJob: Job? = null

    /**
     * Mengubah kategori pencarian online (Lagu, Artis, Album).
     */
    fun setCategory(category: OnlineSearchCategory) {
        if (_selectedCategory.value == category) return
        _selectedCategory.value = category

        val trimmed = currentQuery.trim()
        if (trimmed.length >= 2) {
            searchJob?.cancel()
            searchJob = viewModelScope.launch {
                jalankanPencarian(trimmed)
            }
        }
    }

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

        when (_selectedCategory.value) {
            OnlineSearchCategory.SEMUA -> {
                val result = YouTubeMusicApi.searchAll(kueri)
                result.fold(
                    onSuccess = { res ->
                        _uiState.value = if (res.topResult == null && res.songs.isEmpty() && res.artists.isEmpty() && res.albums.isEmpty()) {
                            OnlineSearchUiState.TidakAda
                        } else {
                            OnlineSearchUiState.BerisiSemua(res)
                        }
                    },
                    onFailure = { error ->
                        _uiState.value = OnlineSearchUiState.Galat(
                            error.message ?: "Gagal memuat hasil pencarian"
                        )
                    }
                )
            }
            OnlineSearchCategory.LAGU -> {
                val result = YouTubeMusicApi.cari(kueri)
                result.fold(
                    onSuccess = { list ->
                        _uiState.value = if (list.isEmpty()) {
                            OnlineSearchUiState.TidakAda
                        } else {
                            OnlineSearchUiState.Berisi(list)
                        }
                    },
                    onFailure = { error ->
                        _uiState.value = OnlineSearchUiState.Galat(
                            error.message ?: "Gagal memuat lagu"
                        )
                    }
                )
            }
            OnlineSearchCategory.ARTIS -> {
                val result = YouTubeMusicApi.searchArtists(kueri)
                result.fold(
                    onSuccess = { list ->
                        _uiState.value = if (list.isEmpty()) {
                            OnlineSearchUiState.TidakAda
                        } else {
                            OnlineSearchUiState.BerisiArtis(list)
                        }
                    },
                    onFailure = { error ->
                        _uiState.value = OnlineSearchUiState.Galat(
                            error.message ?: "Gagal memuat artis"
                        )
                    }
                )
            }
            OnlineSearchCategory.ALBUM -> {
                val result = YouTubeMusicApi.searchAlbums(kueri)
                result.fold(
                    onSuccess = { list ->
                        _uiState.value = if (list.isEmpty()) {
                            OnlineSearchUiState.TidakAda
                        } else {
                            OnlineSearchUiState.BerisiAlbum(list)
                        }
                    },
                    onFailure = { error ->
                        _uiState.value = OnlineSearchUiState.Galat(
                            error.message ?: "Gagal memuat album"
                        )
                    }
                )
            }
        }
    }
}
