package com.example.still_wave_music_player.data.repository

import android.content.Context
import android.util.Log
import android.util.LruCache
import com.example.still_wave_music_player.data.model.LyricsUiState
import com.example.still_wave_music_player.data.model.Song
import com.example.still_wave_music_player.data.remote.LrclibApi
import com.example.still_wave_music_player.utils.LrcParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

/**
 * Repository pengelola lirik lagu dengan cache dua lapis:
 * 1. Cache memori (LruCache)
 * 2. Cache berkas di cacheDir (satu berkas JSON per lagu)
 * 3. Himpunan sesi untuk lagu tanpa lirik agar tidak meminta berulang
 */
class LyricsRepository(private val context: Context) {

    companion object {
        private const val TAG = "MusikPlayer"
        private const val MEMORY_CACHE_SIZE = 100

        @Volatile
        private var instance: LyricsRepository? = null

        fun getInstance(context: Context): LyricsRepository {
            return instance ?: synchronized(this) {
                instance ?: LyricsRepository(context.applicationContext).also { instance = it }
            }
        }
    }

    private val memoryCache = LruCache<String, LyricsUiState>(MEMORY_CACHE_SIZE)
    private val negativeSessionCache: MutableSet<String> = Collections.newSetFromMap(ConcurrentHashMap())
    private val lyricsDir: File by lazy {
        File(context.cacheDir, "lyrics").apply {
            if (!exists()) mkdirs()
        }
    }

    /**
     * Mengambil lirik untuk lagu yang diberikan.
     */
    suspend fun getLyrics(song: Song): LyricsUiState = withContext(Dispatchers.IO) {
        val key = getCacheKey(song)

        // 1. Cek cache memori
        memoryCache.get(key)?.let { return@withContext it }

        // 2. Cek apakah lagu sudah ditandai tidak punya lirik pada sesi ini
        if (negativeSessionCache.contains(key)) {
            return@withContext LyricsUiState.NotFound
        }

        // 3. Cek cache berkas lokal di cacheDir/lyrics/
        val diskState = readFromDisk(key)
        if (diskState != null) {
            if (diskState is LyricsUiState.NotFound) {
                negativeSessionCache.add(key)
            } else {
                memoryCache.put(key, diskState)
            }
            return@withContext diskState
        }

        // 4. Unduh dari API LRCLIB
        val result = LrclibApi.fetchLyrics(
            title = song.title,
            artist = song.artist,
            album = song.album,
            durationMs = song.durationMs
        )

        val response = result.getOrNull()
        val finalState = when {
            response == null -> {
                negativeSessionCache.add(key)
                saveToDisk(key, "none", null, null)
                LyricsUiState.NotFound
            }
            response.instrumental -> {
                saveToDisk(key, "instrumental", null, null)
                LyricsUiState.Instrumental
            }
            !response.syncedLyrics.isNullOrBlank() -> {
                val lines = LrcParser.parse(response.syncedLyrics)
                if (lines.isNotEmpty()) {
                    saveToDisk(key, "synced", response.plainLyrics, response.syncedLyrics)
                    LyricsUiState.Synced(lines)
                } else if (!response.plainLyrics.isNullOrBlank()) {
                    saveToDisk(key, "plain", response.plainLyrics, null)
                    LyricsUiState.Plain(response.plainLyrics)
                } else {
                    negativeSessionCache.add(key)
                    saveToDisk(key, "none", null, null)
                    LyricsUiState.NotFound
                }
            }
            !response.plainLyrics.isNullOrBlank() -> {
                saveToDisk(key, "plain", response.plainLyrics, null)
                LyricsUiState.Plain(response.plainLyrics)
            }
            else -> {
                negativeSessionCache.add(key)
                saveToDisk(key, "none", null, null)
                LyricsUiState.NotFound
            }
        }

        if (finalState !is LyricsUiState.NotFound) {
            memoryCache.put(key, finalState)
        }
        finalState
    }

    private fun getCacheKey(song: Song): String {
        val rawKey = song.videoId?.takeIf { it.isNotBlank() } ?: song.id
        return rawKey.replace(Regex("""[^a-zA-Z0-9_\-]"""), "_")
    }

    private fun readFromDisk(key: String): LyricsUiState? {
        val file = File(lyricsDir, "$key.json")
        if (!file.exists()) return null

        return try {
            val content = file.readText(Charsets.UTF_8)
            val json = JSONObject(content)
            when (json.optString("type")) {
                "none" -> LyricsUiState.NotFound
                "instrumental" -> LyricsUiState.Instrumental
                "synced" -> {
                    val syncedLrc = json.optString("syncedLyrics")
                    val lines = LrcParser.parse(syncedLrc)
                    if (lines.isNotEmpty()) {
                        LyricsUiState.Synced(lines)
                    } else {
                        val plain = json.optString("plainLyrics").takeIf { it.isNotBlank() }
                        if (plain != null) LyricsUiState.Plain(plain) else LyricsUiState.NotFound
                    }
                }
                "plain" -> {
                    val plain = json.optString("plainLyrics").takeIf { it.isNotBlank() }
                    if (plain != null) LyricsUiState.Plain(plain) else LyricsUiState.NotFound
                }
                else -> null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Gagal membaca cache lirik berkas $key: ${e.message}")
            null
        }
    }

    private fun saveToDisk(key: String, type: String, plainLyrics: String?, syncedLyrics: String?) {
        try {
            val file = File(lyricsDir, "$key.json")
            val json = JSONObject().apply {
                put("type", type)
                if (!plainLyrics.isNullOrBlank()) put("plainLyrics", plainLyrics)
                if (!syncedLyrics.isNullOrBlank()) put("syncedLyrics", syncedLyrics)
            }
            file.writeText(json.toString(), Charsets.UTF_8)
        } catch (e: Exception) {
            Log.w(TAG, "Gagal menulis cache lirik berkas $key: ${e.message}")
        }
    }
}
