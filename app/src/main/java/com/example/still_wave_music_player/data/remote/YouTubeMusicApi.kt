package com.example.still_wave_music_player.data.remote

import android.util.Log
import com.example.still_wave_music_player.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import java.util.Locale

/**
 * Klien API pencarian lagu YouTube Music menggunakan endpoint internal YouTubei.
 */
object YouTubeMusicApi {
    private const val TAG = "MusikPlayer"

    // Konstanta konfigurasi permintaan YouTubei
    const val CLIENT_NAME = "WEB_REMIX"
    const val CLIENT_VERSION = "1.20250101.01.00"
    const val PARAMS_SONGS = "EgWKAQIIAWoMEA4QChADEAQQCRAF"

    private const val SEARCH_URL = "https://music.youtube.com/youtubei/v1/search?prettyPrint=false"
    private const val NEXT_URL = "https://music.youtube.com/youtubei/v1/next?prettyPrint=false"
    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36"

    private class HttpException(message: String) : Exception(message)

    /**
     * Mencari lagu dari YouTube Music di Dispatchers.IO.
     * Mengembalikan Result berisi daftar Song (maksimal 25 item).
     */
    suspend fun cari(kueri: String): Result<List<Song>> = withContext(Dispatchers.IO) {
        val trimmedQuery = kueri.trim()
        if (trimmedQuery.isEmpty()) {
            return@withContext Result.success(emptyList())
        }

        try {
            // Percobaan 1: dengan parameter filter "Lagu"
            var songs = parseSongsFromResponse(executeSearchRequest(trimmedQuery, PARAMS_SONGS))

            // Percobaan 2: jika respons kosong, coba ulang SEKALI tanpa parameter filter
            if (songs.isEmpty()) {
                songs = parseSongsFromResponse(executeSearchRequest(trimmedQuery, null))
            }

            Result.success(songs)
        } catch (e: UnknownHostException) {
            Log.e(TAG, "Pencarian YouTube Music gagal: tidak ada internet (${e.message})")
            Result.failure(Exception("Tidak ada koneksi internet"))
        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "Pencarian YouTube Music gagal: batas waktu terlampaui (${e.message})")
            Result.failure(Exception("Koneksi terlalu lama"))
        } catch (e: HttpException) {
            Log.e(TAG, "Pencarian YouTube Music gagal: ${e.message}")
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Pencarian YouTube Music gagal: ${e.message}")
            Result.failure(Exception("Gagal memuat hasil"))
        }
    }

    /**
     * Mengambil daftar lagu radio serupa dari YouTube Music di Dispatchers.IO.
     * Mengembalikan Result berisi daftar Song (maksimal 30 item tanpa duplikat).
     */
    suspend fun radio(videoId: String): Result<List<Song>> = withContext(Dispatchers.IO) {
        val trimmedId = videoId.trim()
        if (trimmedId.isEmpty()) {
            return@withContext Result.success(emptyList())
        }

        try {
            val responseText = executePostRequest(NEXT_URL, buildRadioRequestBody(trimmedId))
            val songs = parseRadioFromResponse(responseText, trimmedId)
            Result.success(songs)
        } catch (e: UnknownHostException) {
            Log.e(TAG, "Radio YouTube Music gagal: tidak ada internet (${e.message})")
            Result.failure(Exception("Tidak ada koneksi internet"))
        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "Radio YouTube Music gagal: batas waktu terlampaui (${e.message})")
            Result.failure(Exception("Koneksi terlalu lama"))
        } catch (e: HttpException) {
            Log.e(TAG, "Radio YouTube Music gagal: ${e.message}")
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Radio YouTube Music gagal: ${e.message}")
            Result.failure(Exception("Gagal memuat radio"))
        }
    }

    /**
     * Menjalankan permintaan HTTP POST umum ke YouTube Music.
     */
    fun executePostRequest(urlStr: String, requestBody: String): String {
        val url = URL(urlStr)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10_000
            readTimeout = 10_000
            doOutput = true
            doInput = true
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Origin", "https://music.youtube.com")
            setRequestProperty("Referer", "https://music.youtube.com/")
        }

        conn.outputStream.use { os ->
            os.write(requestBody.toByteArray(Charsets.UTF_8))
            os.flush()
        }

        val responseCode = conn.responseCode
        if (responseCode != 200) {
            throw HttpException("Server menolak permintaan (kode $responseCode)")
        }

        return conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    /**
     * Menjalankan permintaan HTTP POST pencarian ke YouTube Music.
     */
    private fun executeSearchRequest(query: String, params: String?): String {
        val requestBody = buildRequestBody(query, params)
        return executePostRequest(SEARCH_URL, requestBody)
    }

    /**
     * Membangun payload JSON untuk request YouTubei pencarian.
     */
    private fun buildRequestBody(query: String, params: String?): String {
        val clientObj = JSONObject().apply {
            put("clientName", CLIENT_NAME)
            put("clientVersion", CLIENT_VERSION)
            put("hl", "id")
            put("gl", "ID")
        }
        val contextObj = JSONObject().apply {
            put("client", clientObj)
        }
        val rootObj = JSONObject().apply {
            put("context", contextObj)
            put("query", query)
            if (!params.isNullOrBlank()) {
                put("params", params)
            }
        }
        return rootObj.toString()
    }

    /**
     * Membangun payload JSON untuk request radio (next endpoint).
     */
    private fun buildRadioRequestBody(videoId: String): String {
        val clientObj = JSONObject().apply {
            put("clientName", CLIENT_NAME)
            put("clientVersion", CLIENT_VERSION)
            put("hl", "id")
            put("gl", "ID")
        }
        val contextObj = JSONObject().apply {
            put("client", clientObj)
        }
        val rootObj = JSONObject().apply {
            put("context", contextObj)
            put("videoId", videoId)
            put("playlistId", "RDAMVM$videoId")
            put("isAudioOnly", true)
            put("params", "wAEB")
        }
        return rootObj.toString()
    }

    /**
     * Mengurai string JSON secara defensif mencari musicResponsiveListItemRenderer.
     */
    private fun parseSongsFromResponse(jsonStr: String): List<Song> {
        val rootJson = try {
            JSONObject(jsonStr)
        } catch (e: Exception) {
            Log.e(TAG, "Gagal mengurai respons JSON: ${e.message}")
            return emptyList()
        }

        val renderers = mutableListOf<JSONObject>()
        findItemRenderers(rootJson, renderers)

        val results = mutableListOf<Song>()
        val seenVideoIds = mutableSetOf<String>()

        for (item in renderers) {
            if (results.size >= 25) break

            // 1. Ambil videoId
            val videoId = item.optJSONObject("playlistItemData")?.optString("videoId")?.takeIf { it.isNotBlank() }
                ?: findWatchEndpointVideoId(item)

            // Item tanpa videoId dibuang (karena itu artis, album, atau playlist)
            if (videoId.isNullOrBlank() || !seenVideoIds.add(videoId)) {
                continue
            }

            // 2. Ambil judul dari kolom pertama
            val flexColumns = item.optJSONArray("flexColumns")
            val col0 = flexColumns?.optJSONObject(0)
            val text0 = col0?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")?.optJSONObject("text")
            val runs0 = text0?.optJSONArray("runs")
            val title = runs0?.optJSONObject(0)?.optString("text")?.trim().takeUnless { it.isNullOrBlank() }
                ?: "Tanpa Judul"

            // 3. Ambil informasi baris kedua (artis, album, durasi)
            val col1 = flexColumns?.optJSONObject(1)
            val text1 = col1?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")?.optJSONObject("text")
            val runs1 = text1?.optJSONArray("runs")

            var artistBrowseId: String? = null
            var albumBrowseId: String? = null
            val segments = mutableListOf<String>()
            val currentSeg = StringBuilder()
            if (runs1 != null) {
                for (i in 0 until runs1.length()) {
                    val runObj = runs1.optJSONObject(i) ?: continue
                    val runText = runObj.optString("text") ?: ""
                    val bId = runObj.optJSONObject("navigationEndpoint")
                        ?.optJSONObject("browseEndpoint")
                        ?.optString("browseId")

                    if (!bId.isNullOrBlank()) {
                        if (bId.startsWith("UC") && artistBrowseId == null) {
                            artistBrowseId = bId
                        } else if (bId.startsWith("MPRE") && albumBrowseId == null) {
                            albumBrowseId = bId
                        }
                    }

                    if (runText.contains("•")) {
                        val seg = currentSeg.toString().trim()
                        if (seg.isNotEmpty()) {
                            segments.add(seg)
                        }
                        currentSeg.clear()
                    } else {
                        currentSeg.append(runText)
                    }
                }
                val lastSeg = currentSeg.toString().trim()
                if (lastSeg.isNotEmpty()) {
                    segments.add(lastSeg)
                }
            }

            val typeMarkers = setOf("lagu", "video", "song", "track")
            val nonTypeSegments = segments.filter { seg ->
                seg.lowercase(Locale.ROOT) !in typeMarkers
            }

            var durationMs = 0L
            val infoSegments = nonTypeSegments.toMutableList()
            if (infoSegments.isNotEmpty()) {
                val last = infoSegments.last()
                if (last.matches(Regex("""^(\d{1,2}:)?\d{1,2}:\d{2}$"""))) {
                    durationMs = parseDurationMs(last)
                    infoSegments.removeAt(infoSegments.size - 1)
                }
            }

            val (artist, album) = when (infoSegments.size) {
                0 -> "Artis tidak diketahui" to ""
                1 -> infoSegments[0] to ""
                else -> {
                    val albumName = infoSegments.last()
                    val artistName = infoSegments.subList(0, infoSegments.size - 1).joinToString(", ")
                    artistName to albumName
                }
            }

            // 4. Ambil URL gambar sampul
            val thumbnailObj = item.optJSONObject("thumbnail")
                ?.optJSONObject("musicThumbnailRenderer")
                ?.optJSONObject("thumbnail")
            val thumbnails = thumbnailObj?.optJSONArray("thumbnails")
            var imageUrl: String? = null
            if (thumbnails != null && thumbnails.length() > 0) {
                val lastThumb = thumbnails.optJSONObject(thumbnails.length() - 1)
                imageUrl = lastThumb?.optString("url")
            }

            val finalThumbnailUrl = upgradeThumbnailUrl(imageUrl)

            results.add(
                Song(
                    id = "yt_$videoId",
                    title = title,
                    artist = if (artist.isBlank()) "Artis tidak diketahui" else artist,
                    album = album,
                    durationMs = durationMs,
                    thumbnailUrl = finalThumbnailUrl,
                    contentUri = null,
                    dateAdded = 0L,
                    albumId = 0L,
                    videoId = videoId,
                    artistBrowseId = artistBrowseId,
                    albumBrowseId = albumBrowseId
                )
            )
        }

        return results
    }

    /**
     * Meningkatkan resolusi thumbnail YouTube Music ke w544-h544.
     */
    fun upgradeThumbnailUrl(imageUrl: String?): String? {
        val url = imageUrl ?: return null
        val upgraded = when {
            url.contains("=w") || url.contains("=s") -> {
                url.replace(Regex("""=w\d+-h\d+.*$"""), "=w544-h544-l90-rj")
                    .replace(Regex("""=s\d+.*$"""), "=w544-h544-l90-rj")
            }
            else -> url
        }
        return if (upgraded.startsWith("//")) "https:$upgraded" else upgraded
    }

    /**
     * Mengurai string JSON respons radio mencari playlistPanelVideoRenderer.
     */
    private fun parseRadioFromResponse(jsonStr: String, sourceVideoId: String): List<Song> {
        val rootJson = try {
            JSONObject(jsonStr)
        } catch (e: Exception) {
            Log.e(TAG, "Gagal mengurai respons JSON radio: ${e.message}")
            return emptyList()
        }

        val renderers = mutableListOf<JSONObject>()
        findPlaylistPanelVideoRenderers(rootJson, renderers)

        val results = mutableListOf<Song>()
        val seenVideoIds = mutableSetOf<String>()
        // Jangan sertakan lagu sumber jika muncul di antrean awal
        seenVideoIds.add(sourceVideoId)

        for (item in renderers) {
            if (results.size >= 30) break

            val videoId = item.optString("videoId").takeIf { it.isNotBlank() }
                ?: findWatchEndpointVideoId(item)

            if (videoId.isNullOrBlank() || !seenVideoIds.add(videoId)) {
                continue
            }

            // Judul lagu
            val titleRuns = item.optJSONObject("title")?.optJSONArray("runs")
            val title = if (titleRuns != null && titleRuns.length() > 0) {
                val sb = StringBuilder()
                for (i in 0 until titleRuns.length()) {
                    sb.append(titleRuns.optJSONObject(i)?.optString("text") ?: "")
                }
                sb.toString().trim()
            } else {
                item.optJSONObject("title")?.optString("simpleText")?.trim() ?: "Tanpa Judul"
            }

            // Artis dan album
            val bylineObj = item.optJSONObject("longBylineText") ?: item.optJSONObject("shortBylineText")
            val bylineRuns = bylineObj?.optJSONArray("runs")
            var artistBrowseId: String? = null
            var albumBrowseId: String? = null
            val segments = mutableListOf<String>()
            val currentSeg = StringBuilder()

            if (bylineRuns != null) {
                for (i in 0 until bylineRuns.length()) {
                    val runObj = bylineRuns.optJSONObject(i) ?: continue
                    val runText = runObj.optString("text") ?: ""
                    val bId = runObj.optJSONObject("navigationEndpoint")
                        ?.optJSONObject("browseEndpoint")
                        ?.optString("browseId")

                    if (!bId.isNullOrBlank()) {
                        if (bId.startsWith("UC") && artistBrowseId == null) {
                            artistBrowseId = bId
                        } else if (bId.startsWith("MPRE") && albumBrowseId == null) {
                            albumBrowseId = bId
                        }
                    }

                    if (runText.contains("•")) {
                        val seg = currentSeg.toString().trim()
                        if (seg.isNotEmpty()) {
                            segments.add(seg)
                        }
                        currentSeg.clear()
                    } else {
                        currentSeg.append(runText)
                    }
                }
                val lastSeg = currentSeg.toString().trim()
                if (lastSeg.isNotEmpty()) {
                    segments.add(lastSeg)
                }
            }

            val typeMarkers = setOf("lagu", "video", "song", "track")
            val nonTypeSegments = segments.filter { seg ->
                seg.lowercase(Locale.ROOT) !in typeMarkers
            }

            val (artist, album) = when (nonTypeSegments.size) {
                0 -> "Artis tidak diketahui" to ""
                1 -> nonTypeSegments[0] to ""
                else -> {
                    val albumName = nonTypeSegments.last()
                    val artistName = nonTypeSegments.subList(0, nonTypeSegments.size - 1).joinToString(", ")
                    artistName to albumName
                }
            }

            // Durasi lagu
            val lengthRuns = item.optJSONObject("lengthText")?.optJSONArray("runs")
            val lengthStr = lengthRuns?.optJSONObject(0)?.optString("text")
                ?: item.optJSONObject("lengthText")?.optString("simpleText")
                ?: ""
            val durationMs = if (lengthStr.isNotBlank()) parseDurationMs(lengthStr) else 0L

            // Sampul
            val thumbnailObj = item.optJSONObject("thumbnail")
            val thumbnails = thumbnailObj?.optJSONArray("thumbnails")
            var imageUrl: String? = null
            if (thumbnails != null && thumbnails.length() > 0) {
                val lastThumb = thumbnails.optJSONObject(thumbnails.length() - 1)
                imageUrl = lastThumb?.optString("url")
            }
            val finalThumbnailUrl = upgradeThumbnailUrl(imageUrl)

            results.add(
                Song(
                    id = "yt_$videoId",
                    title = if (title.isBlank()) "Tanpa Judul" else title,
                    artist = if (artist.isBlank()) "Artis tidak diketahui" else artist,
                    album = album,
                    durationMs = durationMs,
                    thumbnailUrl = finalThumbnailUrl,
                    contentUri = null,
                    dateAdded = 0L,
                    albumId = 0L,
                    videoId = videoId,
                    artistBrowseId = artistBrowseId,
                    albumBrowseId = albumBrowseId
                )
            )
        }

        return results
    }

    /**
     * Mencari objek playlistPanelVideoRenderer secara rekursif dalam respons radio YouTubei.
     */
    private fun findPlaylistPanelVideoRenderers(json: Any, results: MutableList<JSONObject>) {
        when (json) {
            is JSONObject -> {
                val renderer = json.optJSONObject("playlistPanelVideoRenderer")
                if (renderer != null) {
                    results.add(renderer)
                } else {
                    val keys = json.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val child = json.opt(key)
                        if (child != null) {
                            findPlaylistPanelVideoRenderers(child, results)
                        }
                    }
                }
            }
            is JSONArray -> {
                for (i in 0 until json.length()) {
                    val child = json.opt(i)
                    if (child != null) {
                        findPlaylistPanelVideoRenderers(child, results)
                    }
                }
            }
        }
    }

    /**
     * Mencari objek musicResponsiveListItemRenderer secara rekursif dalam hierarki JSON.
     */
    private fun findItemRenderers(json: Any, results: MutableList<JSONObject>) {
        when (json) {
            is JSONObject -> {
                val renderer = json.optJSONObject("musicResponsiveListItemRenderer")
                if (renderer != null) {
                    results.add(renderer)
                } else {
                    val keys = json.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val child = json.opt(key)
                        if (child != null) {
                            findItemRenderers(child, results)
                        }
                    }
                }
            }
            is JSONArray -> {
                for (i in 0 until json.length()) {
                    val child = json.opt(i)
                    if (child != null) {
                        findItemRenderers(child, results)
                    }
                }
            }
        }
    }

    /**
     * Mencari watchEndpoint.videoId secara rekursif dalam suatu objek item.
     */
    private fun findWatchEndpointVideoId(json: Any): String? {
        when (json) {
            is JSONObject -> {
                val watchEndpoint = json.optJSONObject("watchEndpoint")
                val vId = watchEndpoint?.optString("videoId")
                if (!vId.isNullOrBlank()) {
                    return vId
                }

                val keys = json.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val child = json.opt(key) ?: continue
                    val res = findWatchEndpointVideoId(child)
                    if (res != null) {
                        return res
                    }
                }
            }
            is JSONArray -> {
                for (i in 0 until json.length()) {
                    val child = json.opt(i) ?: continue
                    val res = findWatchEndpointVideoId(child)
                    if (res != null) {
                        return res
                    }
                }
            }
        }
        return null
    }

    /**
     * Mengubah string format durasi (m:ss atau h:mm:ss) ke milidetik.
     */
    private fun parseDurationMs(durationStr: String): Long {
        val parts = durationStr.trim().split(":")
        return try {
            when (parts.size) {
                2 -> {
                    val min = parts[0].toLong()
                    val sec = parts[1].toLong()
                    (min * 60 + sec) * 1000L
                }
                3 -> {
                    val hour = parts[0].toLong()
                    val min = parts[1].toLong()
                    val sec = parts[2].toLong()
                    (hour * 3600 + min * 60 + sec) * 1000L
                }
                else -> 0L
            }
        } catch (_: Exception) {
            0L
        }
    }
}
