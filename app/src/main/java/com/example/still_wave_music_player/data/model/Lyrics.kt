package com.example.still_wave_music_player.data.model

/**
 * Baris lirik lagu dengan stempel waktu dalam milidetik.
 */
data class LyricLine(
    val timeMs: Long,
    val text: String
)

/**
 * State status lirik untuk UI pemutar.
 */
sealed class LyricsUiState {
    data object Loading : LyricsUiState()
    data object NotFound : LyricsUiState()
    data object Instrumental : LyricsUiState()
    data class Plain(val text: String) : LyricsUiState()
    data class Synced(val lines: List<LyricLine>) : LyricsUiState()
}
