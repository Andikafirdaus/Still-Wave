package com.example.still_wave_music_player.playback

import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Status timer tidur.
 */
data class SleepTimerState(
    val isActive: Boolean = false,
    val remainingMs: Long = 0L,
    val isUntilEndOfSong: Boolean = false
)

/**
 * Pengelola Timer Tidur yang berjalan di background.
 */
object SleepTimerManager {
    private var player: ExoPlayer? = null
    private var timerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _state = MutableStateFlow(SleepTimerState())
    val state: StateFlow<SleepTimerState> = _state.asStateFlow()

    @Volatile
    var isFadingOut = false
        private set

    fun attachPlayer(exo: ExoPlayer) {
        player = exo
    }

    fun detachPlayer() {
        player = null
        cancelTimer()
    }

    /**
     * Memulai timer dengan durasi tertentu dalam menit.
     */
    fun startTimer(minutes: Int) {
        cancelTimer()
        val durationMs = minutes * 60 * 1000L
        val endTime = System.currentTimeMillis() + durationMs

        timerJob = scope.launch {
            while (isActive) {
                val remaining = endTime - System.currentTimeMillis()

                // Jika sisa waktu 5 detik atau kurang, mulai fade out
                if (remaining <= 5000L) {
                    if (remaining > 0) {
                        _state.value = SleepTimerState(isActive = true, remainingMs = remaining, isUntilEndOfSong = false)
                        fadeOutAndPause(player, remaining)
                    } else {
                        fadeOutAndPause(player, 0L)
                    }
                    break
                }

                _state.value = SleepTimerState(isActive = true, remainingMs = remaining, isUntilEndOfSong = false)
                delay(1000L)
            }
        }
    }

    /**
     * Memulai timer yang berhenti tepat saat lagu ini selesai.
     */
    fun startUntilEndOfSong() {
        cancelTimer()
        _state.value = SleepTimerState(isActive = true, remainingMs = 0L, isUntilEndOfSong = true)

        timerJob = scope.launch {
            while (isActive) {
                val p = player ?: break
                val duration = p.duration
                val pos = p.currentPosition

                if (duration > 0 && duration != C.TIME_UNSET) {
                    val remaining = duration - pos

                    // Jika sisa lagu 5 detik atau kurang, mulai fade out
                    if (remaining <= 5000L) {
                        _state.value = _state.value.copy(remainingMs = remaining)
                        fadeOutAndPause(p, remaining)
                        break
                    }

                    _state.value = _state.value.copy(remainingMs = remaining)
                }
                delay(500L)
            }
        }
    }

    /**
     * Mengurangi volume perlahan lalu menjeda pemutaran.
     */
    private suspend fun fadeOutAndPause(exo: ExoPlayer?, remainingMs: Long) {
        if (exo == null) return
        isFadingOut = true
        val startVol = exo.volume
        val steps = 10

        // Jika sisa waktu kurang dari 5 detik, sesuaikan jeda agar pas
        val actualDuration = if (remainingMs in 1L..5000L) remainingMs else 5000L
        val stepMs = (actualDuration / steps).coerceAtLeast(50L)

        for (i in 1..steps) {
            exo.volume = (startVol * (1f - i.toFloat() / steps)).coerceIn(0f, 1f)
            delay(stepMs)
        }

        exo.volume = startVol
        exo.pause()
        isFadingOut = false
        cancelTimer()
    }

    fun cancelTimer() {
        timerJob?.cancel()
        timerJob = null
        _state.value = SleepTimerState()
    }
}