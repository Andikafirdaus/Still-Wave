package com.example.still_wave_music_player.viewmodels

import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.still_wave_music_player.data.model.Song
import com.example.still_wave_music_player.data.repository.LocalMusicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Representasi status pemuatan lagu di perpustakaan.
 */
sealed interface LibraryUiState {
    data object BelumIzin : LibraryUiState
    data object Memuat : LibraryUiState
    data object Kosong : LibraryUiState
    data class Berisi(val daftar: List<Song>) : LibraryUiState
}

/**
 * ViewModel pengelola perpustakaan lagu dari penyimpanan lokal.
 */
class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LocalMusicRepository(application)

    private val _uiState = MutableStateFlow<LibraryUiState>(LibraryUiState.Memuat)
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        periksaIzinDanMuat()
    }

    /**
     * Mengembalikan nama izin yang sesuai menurut versi SDK Android.
     */
    fun getRequiredPermission(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_AUDIO
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    /**
     * Memeriksa apakah izin telah diberikan.
     */
    fun hasPermission(): Boolean {
        val permission = getRequiredPermission()
        return ContextCompat.checkSelfPermission(
            getApplication(),
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Memeriksa izin saat awal inisialisasi; jika ada maka langsung memuat lagu.
     */
    fun periksaIzinDanMuat() {
        if (hasPermission()) {
            muat()
        } else {
            _uiState.value = LibraryUiState.BelumIzin
        }
    }

    /**
     * Memuat lagu dari MediaStore ke dalam state perpustakaan.
     */
    fun muat() {
        viewModelScope.launch {
            _uiState.value = LibraryUiState.Memuat
            val songs = repository.getSongs()
            _uiState.value = if (songs.isEmpty()) {
                LibraryUiState.Kosong
            } else {
                LibraryUiState.Berisi(songs)
            }
        }
    }
}
