package com.example.still_wave_music_player.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import kotlin.math.abs

/**
 * Data respons dari API LRCLIB.
 */
data class LrclibResponse(
    val instrumental: Boolean = false,
    val plainLyrics: String? = null,
    val syncedLyrics: String? = null
)

/**
 * Klien API untuk mengambil lirik lagu dari LRCLIB (https://lrclib.net).
 */
object LrclibApi {
    private const val TAG = "MusikPlayer"
    private const val BASE_URL = "https://lrclib.net/api"
    private const val USER_AGENT = "StillWavePlayer/1.0"
    private const val TIMEOUT_MS = 10_000

    private val FORBIDDEN_TITLE_KEYWORDS = listOf(
        "official", "video", "audio", "lyrics", "lirik", "mv", "hd", "remaster", "live"
    )
    private val BRACKETS_REGEX = Regex("""[\(\[](.*?)[\)\]]""")
    private val TOPIC_REGEX = Regex("""\s*-\s*Topic\b""", RegexOption.IGNORE_CASE)
    private val VEVO_REGEX = Regex("""\bVEVO\b""", RegexOption.IGNORE_CASE)
    private val FEAT_REGEX = Regex("""\s+(?:feat\.?|ft\.?)\s+.*$""", RegexOption.IGNORE_CASE)
    private val MULTI_SPACE_REGEX = Regex("""\s+""")

    /**
     * Membersihkan nama artis dari embel-embel seperti " - Topic", "VEVO", dan akhiran "feat./ft.".
     */
    fun cleanArtist(artist: String): String {
        var cleaned = artist
        cleaned = TOPIC_REGEX.replace(cleaned, "")
        cleaned = VEVO_REGEX.replace(cleaned, "")
        cleaned = FEAT_REGEX.replace(cleaned, "")
        return cleaned.trim()
    }

    /**
     * Membersihkan judul dari teks dalam kurung yang memuat kata kunci terlarang
     * (official, video, audio, lyrics, lirik, mv, hd, remaster, live).
     */
    fun cleanTitle(title: String): String {
        var cleaned = title
        cleaned = BRACKETS_REGEX.replace(cleaned) { matchResult ->
            val inner = matchResult.groupValues[1].lowercase(Locale.ROOT)
            val hasForbidden = FORBIDDEN_TITLE_KEYWORDS.any { inner.contains(it) }
            if (hasForbidden) "" else matchResult.value
        }
        cleaned = MULTI_SPACE_REGEX.replace(cleaned, " ")
        return cleaned.trim()
    }

    /**
     * Mengambil lirik lagu dari LRCLIB di Dispatchers.IO.
     */
    suspend fun fetchLyrics(
        title: String,
        artist: String,
        album: String?,
        durationMs: Long
    ): Result<LrclibResponse?> = withContext(Dispatchers.IO) {
        val cleanedTitle = cleanTitle(title)
        val cleanedArtist = cleanArtist(artist)
        val cleanedAlbum = album?.trim().orEmpty()
        val durationSeconds = if (durationMs > 0L) (durationMs / 1000L).toInt() else 0

        if (cleanedTitle.isEmpty()) {
            return@withContext Result.success(null)
        }

        try {
            // Percobaan 1: GET /api/get
            val getResult = executeGetRequest(cleanedTitle, cleanedArtist, cleanedAlbum, durationSeconds)
            if (getResult != null) {
                return@withContext Result.success(getResult)
            }

            // Percobaan 2 (jika 404): GET /api/search?q=<judul artis>
            val searchQuery = "$cleanedTitle $cleanedArtist".trim()
            val searchResult = executeSearchRequest(searchQuery, durationSeconds)
            Result.success(searchResult)
        } catch (e: Exception) {
            Log.e(TAG, "Gagal mengambil lirik dari LRCLIB: ${e.message}")
            Result.failure(e)
        }
    }

    private fun executeGetRequest(
        title: String,
        artist: String,
        album: String,
        durationSeconds: Int
    ): LrclibResponse? {
        val params = StringBuilder()
        params.append("track_name=").append(URLEncoder.encode(title, "UTF-8"))
        params.append("&artist_name=").append(URLEncoder.encode(artist, "UTF-8"))
        if (album.isNotEmpty()) {
            params.append("&album_name=").append(URLEncoder.encode(album, "UTF-8"))
        }
        if (durationSeconds > 0) {
            params.append("&duration=").append(durationSeconds)
        }

        val urlString = "$BASE_URL/get?$params"
        val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("User-Agent", USER_AGENT)
        }

        val responseCode = conn.responseCode
        if (responseCode == 404) {
            return null
        }
        if (responseCode != 200) {
            Log.w(TAG, "LRCLIB /get mengembalikan kode HTTP $responseCode")
            return null
        }

        val jsonStr = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val json = JSONObject(jsonStr)
        return parseResponseObject(json)
    }

    private fun executeSearchRequest(query: String, durationSeconds: Int): LrclibResponse? {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val urlString = "$BASE_URL/search?q=$encodedQuery"

        val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("User-Agent", USER_AGENT)
        }

        val responseCode = conn.responseCode
        if (responseCode != 200) {
            Log.w(TAG, "LRCLIB /search mengembalikan kode HTTP $responseCode")
            return null
        }

        val jsonStr = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val array = JSONArray(jsonStr)
        if (array.length() == 0) return null

        var bestMatch: LrclibResponse? = null
        var bestDiff = Int.MAX_VALUE

        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val itemDuration = item.optDouble("duration", 0.0).toInt()
            val diff = if (durationSeconds > 0 && itemDuration > 0) {
                abs(itemDuration - durationSeconds)
            } else {
                0
            }

            // Batas maksimal selisih durasi 3 detik (jika durasi lagu diketahui)
            if (durationSeconds > 0 && diff > 3) {
                continue
            }

            if (diff < bestDiff) {
                bestDiff = diff
                bestMatch = parseResponseObject(item)
                if (diff == 0) break
            }
        }

        return bestMatch
    }

    private fun parseResponseObject(json: JSONObject): LrclibResponse {
        val instrumental = json.optBoolean("instrumental", false)
        val plainLyrics = json.optString("plainLyrics").takeIf { it.isNotBlank() }
        val syncedLyrics = json.optString("syncedLyrics").takeIf { it.isNotBlank() }
        return LrclibResponse(
            instrumental = instrumental,
            plainLyrics = plainLyrics,
            syncedLyrics = syncedLyrics
        )
    }
}
