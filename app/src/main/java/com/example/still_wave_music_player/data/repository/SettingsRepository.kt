package com.example.still_wave_music_player.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository penyimpanan pengaturan aplikasi berbasis SharedPreferences "pengaturan".
 * Mengelola opsi putar otomatis lagu serupa (radio) dan durasi crossfade.
 */
class SettingsRepository(context: Context) {

    companion object {
        private const val PREFS_NAME = "pengaturan"
        private const val KEY_AUTO_RADIO = "putar_otomatis_radio"
        private const val KEY_CROSSFADE = "crossfade_detik"

        @Volatile
        private var instance: SettingsRepository? = null

        fun getInstance(context: Context): SettingsRepository {
            return instance ?: synchronized(this) {
                instance ?: SettingsRepository(context.applicationContext).also { instance = it }
            }
        }
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _isAutoRadioEnabled = MutableStateFlow(prefs.getBoolean(KEY_AUTO_RADIO, true))
    val isAutoRadioEnabled: StateFlow<Boolean> = _isAutoRadioEnabled.asStateFlow()

    private val _crossfadeSeconds = MutableStateFlow(prefs.getInt(KEY_CROSSFADE, 0))
    val crossfadeSeconds: StateFlow<Int> = _crossfadeSeconds.asStateFlow()

    fun setAutoRadioEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_RADIO, enabled).apply()
        _isAutoRadioEnabled.value = enabled
    }

    fun setCrossfadeSeconds(seconds: Int) {
        prefs.edit().putInt(KEY_CROSSFADE, seconds).apply()
        _crossfadeSeconds.value = seconds
    }
}
