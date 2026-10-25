package com.example.still_wave_music_player.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.still_wave_music_player.data.local.AppDatabase
import com.example.still_wave_music_player.data.local.getSongKey
import com.example.still_wave_music_player.data.local.toSong
import com.example.still_wave_music_player.data.model.Song
import com.example.still_wave_music_player.data.remote.YouTubeMusicApi
import com.example.still_wave_music_player.data.repository.HistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * ViewModel untuk mengelola rekomendasi "Untuk kamu" dan lagu "Sedang populer" di Beranda.
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val historyRepo = HistoryRepository(db.playHistoryDao())

    private val _forYouSongs = MutableStateFlow<List<Song>>(emptyList())
    val forYouSongs: StateFlow<List<Song>> = _forYouSongs.asStateFlow()

    private val _trendingSongs = MutableStateFlow<List<Song>>(emptyList())
    val trendingSongs: StateFlow<List<Song>> = _trendingSongs.asStateFlow()

    private val _isTrendingLoading = MutableStateFlow(false)
    val isTrendingLoading: StateFlow<Boolean> = _isTrendingLoading.asStateFlow()

    companion object {
        private var cachedForYou: Pair<Long, List<Song>>? = null
        private var cachedTrending: Pair<Long, List<Song>>? = null
        private const val SIX_HOURS_MS = 6 * 3600 * 1000L
    }

    init {
        loadTrending()
        loadForYou()
    }

    fun refreshRecommendations() {
        loadForYou(forceRefresh = true)
    }

    fun loadForYou(forceRefresh: Boolean = false) {
        val now = System.currentTimeMillis()
        val cache = cachedForYou
        if (!forceRefresh && cache != null && (now - cache.first) < SIX_HOURS_MS && cache.second.isNotEmpty()) {
            _forYouSongs.value = cache.second
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val thirtyDaysAgo = now - (30L * 24 * 60 * 60 * 1000L)
                val recentPlays = historyRepo.getSince(thirtyDaysAgo)
                if (recentPlays.isEmpty()) {
                    _forYouSongs.value = emptyList()
                    return@launch
                }

                // Skor tiap artis = jumlah putaran 30 hari terakhir dikali peluruhan (setengah nilai tiap 7 hari)
                val halfLifeMs = 7.0 * 24 * 60 * 60 * 1000.0
                val artistScores = mutableMapOf<String, Double>()
                for (play in recentPlays) {
                    val artistClean = play.artist.trim().lowercase(Locale.ROOT)
                    if (artistClean.isBlank() || artistClean.equals("artis tidak diketahui", ignoreCase = true)) continue
                    val ageMs = (now - play.playedAt).coerceAtLeast(0L)
                    val weight = Math.pow(0.5, ageMs / halfLifeMs)
                    artistScores[artistClean] = (artistScores[artistClean] ?: 0.0) + weight
                }

                val topArtists = artistScores.entries.sortedByDescending { it.value }.map { it.key }

                // Ambil 3 lagu benih dari artis teratas yang memiliki videoId (atau cari)
                val seedCandidates = recentPlays.filter { play ->
                    val a = play.artist.trim().lowercase(Locale.ROOT)
                    a in topArtists
                }

                val seedSongs = mutableListOf<Song>()
                for (candidate in seedCandidates.distinctBy { it.songKey }) {
                    val s = candidate.toSong()
                    if (!s.videoId.isNullOrBlank()) {
                        seedSongs.add(s)
                    } else {
                        try {
                            val searchResult = YouTubeMusicApi.cari("${s.title} ${s.artist}").getOrNull()
                            val foundSong = searchResult?.firstOrNull()
                            if (foundSong != null && !foundSong.videoId.isNullOrBlank()) {
                                seedSongs.add(foundSong)
                            }
                        } catch (e: Exception) {
                            Log.w("MusikPlayer", "Gagal mencari benih online untuk ${s.title}: ${e.message}")
                        }
                    }
                    if (seedSongs.size >= 3) break
                }

                if (seedSongs.isEmpty()) {
                    _forYouSongs.value = emptyList()
                    return@launch
                }

                // Panggil radio untuk tiap benih
                val candidateList = mutableListOf<Song>()
                for (seed in seedSongs) {
                    val vId = seed.videoId ?: continue
                    val radioRes = YouTubeMusicApi.radio(vId, seed.artist).getOrNull().orEmpty()
                    candidateList.addAll(radioRes)
                }

                // Buang 20 putaran terakhir
                val last20Keys = historyRepo.getRecentSongs(20).map { it.getSongKey() }.toSet()

                val filtered = candidateList
                    .distinctBy { it.getSongKey() }
                    .filter { !last20Keys.contains(it.getSongKey()) }
                    .shuffled()

                // Terapkan variasi artis
                val varied = YouTubeMusicApi.applyRadioArtistVariation(filtered)
                val finalSongs = varied.take(20)

                cachedForYou = now to finalSongs
                _forYouSongs.value = finalSongs
            } catch (e: Exception) {
                Log.w("MusikPlayer", "Gagal memuat rekomendasi Untuk Kamu: ${e.message}")
            }
        }
    }

    fun loadTrending() {
        val now = System.currentTimeMillis()
        val cache = cachedTrending
        if (cache != null && (now - cache.first) < SIX_HOURS_MS && cache.second.isNotEmpty()) {
            _trendingSongs.value = cache.second
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _isTrendingLoading.value = true
            try {
                val result = YouTubeMusicApi.cari("lagu populer indonesia").getOrNull().orEmpty()
                if (result.isNotEmpty()) {
                    cachedTrending = now to result
                    _trendingSongs.value = result
                }
            } catch (e: Exception) {
                Log.w("MusikPlayer", "Gagal memuat Sedang Populer: ${e.message}")
            } finally {
                _isTrendingLoading.value = false
            }
        }
    }
}
