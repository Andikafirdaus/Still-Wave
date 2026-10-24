package com.example.still_wave_music_player.data.remote

import android.util.Log
import com.example.still_wave_music_player.data.model.AlbumDetails
import com.example.still_wave_music_player.data.model.AlbumSearchResult
import com.example.still_wave_music_player.data.model.ArtistDetails
import com.example.still_wave_music_player.data.model.ArtistSearchResult
import com.example.still_wave_music_player.data.model.SearchAllResult
import com.example.still_wave_music_player.data.model.Song
import com.example.still_wave_music_player.data.model.TopResult
import com.example.still_wave_music_player.data.model.TopResultType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Klien API pencarian lagu, artis, album, dan browse YouTube Music menggunakan endpoint internal YouTubei.
 */
object YouTubeMusicApi {
    private const val TAG = "MusikPlayer"

    // Konstanta konfigurasi permintaan YouTubei
    const val CLIENT_NAME = "WEB_REMIX"
    const val CLIENT_VERSION = "1.20250101.01.00"
    const val PARAMS_SONGS = "EgWKAQIIAWoMEA4QChADEAQQCRAF"
    const val PARAMS_ARTISTS = "EgWKAQIgAWoMEA4QChADEAQQCRAF"
    const val PARAMS_ALBUMS = "EgWKAQIYAWoMEA4QChADEAQQCRAF"

    private const val SEARCH_URL = "https://music.youtube.com/youtubei/v1/search?prettyPrint=false"
    private const val NEXT_URL = "https://music.youtube.com/youtubei/v1/next?prettyPrint=false"
    private const val BROWSE_URL = "https://music.youtube.com/youtubei/v1/browse?prettyPrint=false"
    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36"

    private val searchAllCache = ConcurrentHashMap<String, SearchAllResult>()
    private val artistBrowseCache = ConcurrentHashMap<String, ArtistDetails>()
    private val albumBrowseCache = ConcurrentHashMap<String, AlbumDetails>()

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
     * Mencari hasil gabungan ("Semua") dari YouTube Music: hasil teratas, lagu, artis, dan album.
     */
    suspend fun searchAll(kueri: String): Result<SearchAllResult> = withContext(Dispatchers.IO) {
        val trimmedQuery = kueri.trim()
        if (trimmedQuery.isEmpty()) {
            return@withContext Result.success(SearchAllResult())
        }

        val cached = searchAllCache[trimmedQuery]
        if (cached != null) {
            return@withContext Result.success(cached)
        }

        try {
            val responseText = executeSearchRequest(trimmedQuery, null)
            val result = parseSearchAllFromResponse(responseText)
            searchAllCache[trimmedQuery] = result
            Result.success(result)
        } catch (e: UnknownHostException) {
            Log.e(TAG, "Pencarian Semua gagal: tidak ada internet (${e.message})")
            Result.failure(Exception("Tidak ada koneksi internet"))
        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "Pencarian Semua gagal: batas waktu terlampaui (${e.message})")
            Result.failure(Exception("Koneksi terlalu lama"))
        } catch (e: HttpException) {
            Log.e(TAG, "Pencarian Semua gagal: ${e.message}")
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Pencarian Semua gagal: ${e.message}")
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
     * Mencari artis dari YouTube Music di Dispatchers.IO.
     */
    suspend fun searchArtists(kueri: String): Result<List<ArtistSearchResult>> = withContext(Dispatchers.IO) {
        val trimmedQuery = kueri.trim()
        if (trimmedQuery.isEmpty()) {
            return@withContext Result.success(emptyList())
        }

        try {
            var artists = parseArtistsFromResponse(executeSearchRequest(trimmedQuery, PARAMS_ARTISTS), filterByPageType = false)
            if (artists.isEmpty()) {
                artists = parseArtistsFromResponse(executeSearchRequest(trimmedQuery, null), filterByPageType = true)
            }
            Result.success(artists)
        } catch (e: UnknownHostException) {
            Log.e(TAG, "Pencarian artis gagal: tidak ada internet (${e.message})")
            Result.failure(Exception("Tidak ada koneksi internet"))
        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "Pencarian artis gagal: batas waktu terlampaui (${e.message})")
            Result.failure(Exception("Koneksi terlalu lama"))
        } catch (e: HttpException) {
            Log.e(TAG, "Pencarian artis gagal: ${e.message}")
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Pencarian artis gagal: ${e.message}")
            Result.failure(Exception("Gagal memuat artis"))
        }
    }

    /**
     * Mencari album dari YouTube Music di Dispatchers.IO.
     */
    suspend fun searchAlbums(kueri: String): Result<List<AlbumSearchResult>> = withContext(Dispatchers.IO) {
        val trimmedQuery = kueri.trim()
        if (trimmedQuery.isEmpty()) {
            return@withContext Result.success(emptyList())
        }

        try {
            var albums = parseAlbumsFromResponse(executeSearchRequest(trimmedQuery, PARAMS_ALBUMS), filterByPageType = false)
            if (albums.isEmpty()) {
                albums = parseAlbumsFromResponse(executeSearchRequest(trimmedQuery, null), filterByPageType = true)
            }
            Result.success(albums)
        } catch (e: UnknownHostException) {
            Log.e(TAG, "Pencarian album gagal: tidak ada internet (${e.message})")
            Result.failure(Exception("Tidak ada koneksi internet"))
        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "Pencarian album gagal: batas waktu terlampaui (${e.message})")
            Result.failure(Exception("Koneksi terlalu lama"))
        } catch (e: HttpException) {
            Log.e(TAG, "Pencarian album gagal: ${e.message}")
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Pencarian album gagal: ${e.message}")
            Result.failure(Exception("Gagal memuat album"))
        }
    }

    /**
     * Mengambil data mentah browse dari YouTube Music di Dispatchers.IO.
     */
    suspend fun browse(browseId: String): String = withContext(Dispatchers.IO) {
        executePostRequest(BROWSE_URL, buildBrowseRequestBody(browseId))
    }

    /**
     * Mengambil rincian album dari YouTube Music di Dispatchers.IO dengan cache memori sesi.
     */
    suspend fun browseAlbum(browseId: String): Result<AlbumDetails> = withContext(Dispatchers.IO) {
        val cached = albumBrowseCache[browseId]
        if (cached != null) {
            return@withContext Result.success(cached)
        }

        try {
            val responseText = browse(browseId)
            val details = parseAlbumDetails(browseId, responseText)
            albumBrowseCache[browseId] = details
            Result.success(details)
        } catch (e: UnknownHostException) {
            Log.e(TAG, "Browse album gagal: tidak ada internet (${e.message})")
            Result.failure(Exception("Tidak ada koneksi internet"))
        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "Browse album gagal: batas waktu terlampaui (${e.message})")
            Result.failure(Exception("Koneksi terlalu lama"))
        } catch (e: HttpException) {
            Log.e(TAG, "Browse album gagal: ${e.message}")
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Browse album gagal: ${e.message}")
            Result.failure(Exception("Gagal memuat album"))
        }
    }

    /**
     * Mengambil rincian artis dari YouTube Music di Dispatchers.IO dengan cache memori sesi.
     */
    suspend fun browseArtist(browseId: String): Result<ArtistDetails> = withContext(Dispatchers.IO) {
        val cached = artistBrowseCache[browseId]
        if (cached != null) {
            return@withContext Result.success(cached)
        }

        try {
            val responseText = browse(browseId)
            val details = parseArtistDetails(browseId, responseText)
            artistBrowseCache[browseId] = details
            Result.success(details)
        } catch (e: UnknownHostException) {
            Log.e(TAG, "Browse artis gagal: tidak ada internet (${e.message})")
            Result.failure(Exception("Tidak ada koneksi internet"))
        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "Browse artis gagal: batas waktu terlampaui (${e.message})")
            Result.failure(Exception("Koneksi terlalu lama"))
        } catch (e: HttpException) {
            Log.e(TAG, "Browse artis gagal: ${e.message}")
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Browse artis gagal: ${e.message}")
            Result.failure(Exception("Gagal memuat artis"))
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

    /**
     * Membangun payload JSON untuk request browse YouTubei.
     */
    private fun buildBrowseRequestBody(browseId: String): String {
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
            put("browseId", browseId)
        }
        return rootObj.toString()
    }

    /**
     * Mengurai hasil pencarian artis dari respons JSON YouTubei.
     */
    private fun parseArtistsFromResponse(jsonStr: String, filterByPageType: Boolean): List<ArtistSearchResult> {
        val rootJson = try {
            JSONObject(jsonStr)
        } catch (e: Exception) {
            Log.e(TAG, "Gagal mengurai JSON artis: ${e.message}")
            return emptyList()
        }

        val renderers = mutableListOf<JSONObject>()
        findItemRenderers(rootJson, renderers)
        val results = mutableListOf<ArtistSearchResult>()
        val seenBrowseIds = mutableSetOf<String>()

        for (item in renderers) {
            val flexCols = item.optJSONArray("flexColumns") ?: continue
            if (flexCols.length() == 0) continue

            var browseId: String? = null
            var isArtist = false

            val itemNav = item.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")
            val itemBrowseId = itemNav?.optString("browseId")
            val itemPageType = itemNav?.optJSONObject("browseEndpointContextSupportedConfigs")
                ?.optJSONObject("browseEndpointContextMusicConfig")
                ?.optString("pageType")

            if (!itemBrowseId.isNullOrBlank() && itemBrowseId.startsWith("UC")) {
                browseId = itemBrowseId
            }
            if (itemPageType == "MUSIC_PAGE_TYPE_ARTIST") {
                isArtist = true
            }

            val titleObj = flexCols.optJSONObject(0)
                ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                ?.optJSONObject("text")
            val name = extractRunsText(titleObj)

            val titleRuns = titleObj?.optJSONArray("runs")
            if (titleRuns != null && titleRuns.length() > 0) {
                val firstRun = titleRuns.optJSONObject(0)
                val runEndpoint = firstRun?.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")
                val runBrowseId = runEndpoint?.optString("browseId")
                val runPageType = runEndpoint?.optJSONObject("browseEndpointContextSupportedConfigs")
                    ?.optJSONObject("browseEndpointContextMusicConfig")
                    ?.optString("pageType")

                if (browseId == null && !runBrowseId.isNullOrBlank()) {
                    browseId = runBrowseId
                }
                if (runPageType == "MUSIC_PAGE_TYPE_ARTIST") {
                    isArtist = true
                }
            }

            if (browseId == null) {
                browseId = findBrowseEndpointWithPrefix(item, "UC")
            }

            if (filterByPageType && !isArtist && (browseId == null || !browseId.startsWith("UC"))) {
                continue
            }

            if (browseId.isNullOrBlank() || name.isBlank() || !seenBrowseIds.add(browseId)) {
                continue
            }

            val thumbnailObj = item.optJSONObject("thumbnail")
                ?.optJSONObject("musicThumbnailRenderer")
                ?.optJSONObject("thumbnail")
            val thumbnails = thumbnailObj?.optJSONArray("thumbnails")
            var imageUrl: String? = null
            if (thumbnails != null && thumbnails.length() > 0) {
                val lastThumb = thumbnails.optJSONObject(thumbnails.length() - 1)
                imageUrl = lastThumb?.optString("url")
            }

            results.add(
                ArtistSearchResult(
                    browseId = browseId,
                    name = name,
                    thumbnailUrl = upgradeThumbnailUrl(imageUrl)
                )
            )
        }
        return results
    }

    /**
     * Mengurai hasil pencarian album dari respons JSON YouTubei.
     */
    private fun parseAlbumsFromResponse(jsonStr: String, filterByPageType: Boolean): List<AlbumSearchResult> {
        val rootJson = try {
            JSONObject(jsonStr)
        } catch (e: Exception) {
            Log.e(TAG, "Gagal mengurai JSON album: ${e.message}")
            return emptyList()
        }

        val renderers = mutableListOf<JSONObject>()
        findItemRenderers(rootJson, renderers)
        val results = mutableListOf<AlbumSearchResult>()
        val seenBrowseIds = mutableSetOf<String>()

        for (item in renderers) {
            val flexCols = item.optJSONArray("flexColumns") ?: continue
            if (flexCols.length() == 0) continue

            var browseId: String? = null
            var isAlbum = false

            val itemNav = item.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")
            val itemBrowseId = itemNav?.optString("browseId")
            val itemPageType = itemNav?.optJSONObject("browseEndpointContextSupportedConfigs")
                ?.optJSONObject("browseEndpointContextMusicConfig")
                ?.optString("pageType")

            if (!itemBrowseId.isNullOrBlank() && (itemBrowseId.startsWith("MPRE") || itemBrowseId.startsWith("FEmusic_library_privately_owned_release"))) {
                browseId = itemBrowseId
            }
            if (itemPageType == "MUSIC_PAGE_TYPE_ALBUM") {
                isAlbum = true
            }

            val titleObj = flexCols.optJSONObject(0)
                ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                ?.optJSONObject("text")
            val title = extractRunsText(titleObj)

            val titleRuns = titleObj?.optJSONArray("runs")
            if (titleRuns != null && titleRuns.length() > 0) {
                val firstRun = titleRuns.optJSONObject(0)
                val runEndpoint = firstRun?.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")
                val runBrowseId = runEndpoint?.optString("browseId")
                val runPageType = runEndpoint?.optJSONObject("browseEndpointContextSupportedConfigs")
                    ?.optJSONObject("browseEndpointContextMusicConfig")
                    ?.optString("pageType")

                if (browseId == null && !runBrowseId.isNullOrBlank()) {
                    browseId = runBrowseId
                }
                if (runPageType == "MUSIC_PAGE_TYPE_ALBUM") {
                    isAlbum = true
                }
            }

            if (browseId == null) {
                browseId = findBrowseEndpointWithPrefix(item, "MPRE")
            }

            if (filterByPageType && !isAlbum && (browseId == null || !browseId.startsWith("MPRE"))) {
                continue
            }

            if (browseId.isNullOrBlank() || title.isBlank() || !seenBrowseIds.add(browseId)) {
                continue
            }

            var artist = "Artis tidak diketahui"
            var year: String? = null
            if (flexCols.length() > 1) {
                val subtitleRuns = flexCols.optJSONObject(1)
                    ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                    ?.optJSONObject("text")
                    ?.optJSONArray("runs")
                if (subtitleRuns != null) {
                    val segs = mutableListOf<String>()
                    val curr = StringBuilder()
                    for (i in 0 until subtitleRuns.length()) {
                        val r = subtitleRuns.optJSONObject(i) ?: continue
                        val t = r.optString("text") ?: ""
                        if (t.contains("•")) {
                            val s = curr.toString().trim()
                            if (s.isNotEmpty()) segs.add(s)
                            curr.clear()
                        } else {
                            curr.append(t)
                        }
                    }
                    val lastS = curr.toString().trim()
                    if (lastS.isNotEmpty()) segs.add(lastS)

                    val nonType = segs.filter { it.lowercase(Locale.ROOT) !in setOf("album", "ep", "single", "singel") }
                    if (nonType.isNotEmpty()) {
                        artist = nonType[0]
                        if (nonType.size > 1 && nonType.last().matches(Regex("""^\d{4}$"""))) {
                            year = nonType.last()
                        }
                    }
                }
            }

            val thumbnailObj = item.optJSONObject("thumbnail")
                ?.optJSONObject("musicThumbnailRenderer")
                ?.optJSONObject("thumbnail")
            val thumbnails = thumbnailObj?.optJSONArray("thumbnails")
            var imageUrl: String? = null
            if (thumbnails != null && thumbnails.length() > 0) {
                val lastThumb = thumbnails.optJSONObject(thumbnails.length() - 1)
                imageUrl = lastThumb?.optString("url")
            }

            results.add(
                AlbumSearchResult(
                    browseId = browseId,
                    title = title,
                    artist = artist,
                    year = year,
                    thumbnailUrl = upgradeThumbnailUrl(imageUrl)
                )
            )
        }
        return results
    }

    /**
     * Mengurai rincian album dan daftar lagunya.
     */
    private fun parseAlbumDetails(browseId: String, jsonStr: String): AlbumDetails {
        val rootJson = try {
            JSONObject(jsonStr)
        } catch (e: Exception) {
            Log.e(TAG, "Gagal mengurai respons album: ${e.message}")
            return AlbumDetails(browseId = browseId, title = "Album", artist = "Artis tidak diketahui")
        }

        val header = findFirstObjectNamed(rootJson, setOf("musicResponsiveHeaderRenderer", "musicDetailHeaderRenderer"))
        var albumTitle = "Album"
        var artistName = "Artis tidak diketahui"
        var artistBrowseId: String? = null
        var year: String? = null
        var albumThumbnailUrl: String? = null

        if (header != null) {
            val titleText = extractRunsText(header.optJSONObject("title"))
            if (titleText.isNotBlank()) albumTitle = titleText

            val subtitleObj = header.optJSONObject("subtitle")
                ?: header.optJSONObject("straplineTextOne")
                ?: header.optJSONObject("bylineText")
            val runs = subtitleObj?.optJSONArray("runs")
            if (runs != null) {
                val segs = mutableListOf<String>()
                val curr = StringBuilder()
                for (i in 0 until runs.length()) {
                    val r = runs.optJSONObject(i) ?: continue
                    val t = r.optString("text") ?: ""
                    val bId = r.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")?.optString("browseId")
                    if (!bId.isNullOrBlank() && bId.startsWith("UC") && artistBrowseId == null) {
                        artistBrowseId = bId
                        artistName = t
                    }
                    if (t.contains("•")) {
                        val s = curr.toString().trim()
                        if (s.isNotEmpty()) segs.add(s)
                        curr.clear()
                    } else {
                        curr.append(t)
                    }
                }
                val lastS = curr.toString().trim()
                if (lastS.isNotEmpty()) segs.add(lastS)

                for (s in segs) {
                    if (s.matches(Regex("""^\d{4}$"""))) {
                        year = s
                    } else if (artistName == "Artis tidak diketahui" && !s.equals("album", ignoreCase = true) && !s.equals("ep", ignoreCase = true)) {
                        artistName = s
                    }
                }
            }

            val thumbObj = header.optJSONObject("thumbnail")
                ?.optJSONObject("musicThumbnailRenderer")
                ?.optJSONObject("thumbnail")
            val thumbs = thumbObj?.optJSONArray("thumbnails")
            if (thumbs != null && thumbs.length() > 0) {
                val lastThumb = thumbs.optJSONObject(thumbs.length() - 1)
                albumThumbnailUrl = upgradeThumbnailUrl(lastThumb?.optString("url"))
            }
        }

        // Cari lagu di musicShelfRenderer
        val renderers = mutableListOf<JSONObject>()
        findItemRenderers(rootJson, renderers)
        val songs = mutableListOf<Song>()
        val seenVideoIds = mutableSetOf<String>()

        for (item in renderers) {
            val videoId = item.optString("videoId").takeIf { it.isNotBlank() }
                ?: findWatchEndpointVideoId(item)
            if (videoId.isNullOrBlank() || !seenVideoIds.add(videoId)) {
                continue
            }

            val flexCols = item.optJSONArray("flexColumns") ?: continue
            if (flexCols.length() == 0) continue

            val trackTitle = extractRunsText(
                flexCols.optJSONObject(0)
                    ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                    ?.optJSONObject("text")
            ).ifBlank { "Lagu ${songs.size + 1}" }

            // Durasi dari fixedColumns jika ada
            var durationMs = 0L
            val fixedCols = item.optJSONArray("fixedColumns")
            if (fixedCols != null && fixedCols.length() > 0) {
                val durStr = extractRunsText(
                    fixedCols.optJSONObject(0)
                        ?.optJSONObject("musicResponsiveListItemFixedColumnRenderer")
                        ?.optJSONObject("text")
                )
                if (durStr.isNotBlank()) {
                    durationMs = parseDurationMs(durStr)
                }
            }

            songs.add(
                Song(
                    id = "yt_$videoId",
                    title = trackTitle,
                    artist = artistName,
                    album = albumTitle,
                    durationMs = durationMs,
                    thumbnailUrl = albumThumbnailUrl,
                    contentUri = null,
                    dateAdded = 0L,
                    albumId = 0L,
                    videoId = videoId,
                    artistBrowseId = artistBrowseId,
                    albumBrowseId = browseId
                )
            )
        }

        return AlbumDetails(
            browseId = browseId,
            title = albumTitle,
            artist = artistName,
            artistBrowseId = artistBrowseId,
            year = year,
            thumbnailUrl = albumThumbnailUrl,
            songs = songs
        )
    }

    /**
     * Mengurai rincian artis, lagu populer, album, dan singel.
     */
    private fun parseArtistDetails(browseId: String, jsonStr: String): ArtistDetails {
        val rootJson = try {
            JSONObject(jsonStr)
        } catch (e: Exception) {
            Log.e(TAG, "Gagal mengurai respons artis: ${e.message}")
            return ArtistDetails(browseId = browseId, name = "Artis")
        }

        val header = findFirstObjectNamed(
            rootJson,
            setOf("musicImmersiveHeaderRenderer", "musicVisualHeaderRenderer", "musicHeaderRenderer", "musicResponsiveHeaderRenderer")
        )

        var artistName = "Artis"
        var artistThumbnailUrl: String? = null
        var listeners: String? = null

        if (header != null) {
            val nameText = extractRunsText(header.optJSONObject("title"))
            if (nameText.isNotBlank()) artistName = nameText

            val subBtn = header.optJSONObject("subscriptionButton")?.optJSONObject("subscribeButtonRenderer")
            val subText = extractRunsText(subBtn?.optJSONObject("subscriberCountText"))
            if (subText.isNotBlank()) {
                listeners = subText
            } else {
                val subtitleText = extractRunsText(header.optJSONObject("subtitle"))
                if (subtitleText.isNotBlank()) listeners = subtitleText
            }

            val thumbObj = (header.optJSONObject("thumbnail") ?: header.optJSONObject("foregroundThumbnail"))
                ?.optJSONObject("musicThumbnailRenderer")
                ?.optJSONObject("thumbnail")
            val thumbs = thumbObj?.optJSONArray("thumbnails")
            if (thumbs != null && thumbs.length() > 0) {
                val lastThumb = thumbs.optJSONObject(thumbs.length() - 1)
                artistThumbnailUrl = upgradeThumbnailUrl(lastThumb?.optString("url"))
            }
        }

        // Top songs dari musicShelfRenderer
        val renderers = mutableListOf<JSONObject>()
        findItemRenderers(rootJson, renderers)
        val topSongs = mutableListOf<Song>()
        val seenVideoIds = mutableSetOf<String>()

        for (item in renderers) {
            if (topSongs.size >= 10) break
            val videoId = item.optString("videoId").takeIf { it.isNotBlank() }
                ?: findWatchEndpointVideoId(item)
            if (videoId.isNullOrBlank() || !seenVideoIds.add(videoId)) {
                continue
            }

            val flexCols = item.optJSONArray("flexColumns") ?: continue
            if (flexCols.length() == 0) continue

            val title = extractRunsText(
                flexCols.optJSONObject(0)
                    ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                    ?.optJSONObject("text")
            ).ifBlank { "Lagu" }

            var albumTitle = ""
            var albumBrowseId: String? = null
            if (flexCols.length() > 1) {
                val runs = flexCols.optJSONObject(1)
                    ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                    ?.optJSONObject("text")
                    ?.optJSONArray("runs")
                if (runs != null) {
                    for (i in 0 until runs.length()) {
                        val r = runs.optJSONObject(i) ?: continue
                        val bId = r.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")?.optString("browseId")
                        if (!bId.isNullOrBlank() && bId.startsWith("MPRE")) {
                            albumBrowseId = bId
                            albumTitle = r.optString("text") ?: ""
                        }
                    }
                }
            }

            var durationMs = 0L
            val fixedCols = item.optJSONArray("fixedColumns")
            if (fixedCols != null && fixedCols.length() > 0) {
                val durStr = extractRunsText(
                    fixedCols.optJSONObject(0)
                        ?.optJSONObject("musicResponsiveListItemFixedColumnRenderer")
                        ?.optJSONObject("text")
                )
                if (durStr.isNotBlank()) durationMs = parseDurationMs(durStr)
            }

            val thumbObj = item.optJSONObject("thumbnail")
                ?.optJSONObject("musicThumbnailRenderer")
                ?.optJSONObject("thumbnail")
            val thumbs = thumbObj?.optJSONArray("thumbnails")
            var songThumb: String? = null
            if (thumbs != null && thumbs.length() > 0) {
                val lastThumb = thumbs.optJSONObject(thumbs.length() - 1)
                songThumb = upgradeThumbnailUrl(lastThumb?.optString("url"))
            }

            topSongs.add(
                Song(
                    id = "yt_$videoId",
                    title = title,
                    artist = artistName,
                    album = albumTitle,
                    durationMs = durationMs,
                    thumbnailUrl = songThumb ?: artistThumbnailUrl,
                    contentUri = null,
                    dateAdded = 0L,
                    albumId = 0L,
                    videoId = videoId,
                    artistBrowseId = browseId,
                    albumBrowseId = albumBrowseId
                )
            )
        }

        // Cari carousels untuk Album dan Single
        val carousels = mutableListOf<JSONObject>()
        findAllObjectsNamed(rootJson, "musicCarouselShelfRenderer", carousels)

        val albums = mutableListOf<AlbumSearchResult>()
        val singles = mutableListOf<AlbumSearchResult>()

        for (carousel in carousels) {
            val headerTitle = extractRunsText(
                carousel.optJSONObject("header")
                    ?.optJSONObject("musicCarouselShelfBasicHeaderRenderer")
                    ?.optJSONObject("title")
            ).lowercase(Locale.ROOT)

            val twoRowItems = mutableListOf<JSONObject>()
            findAllObjectsNamed(carousel, "musicTwoRowItemRenderer", twoRowItems)

            val isSingleShelf = headerTitle.contains("single") || headerTitle.contains("singel") || headerTitle.contains("ep")

            for (twoRow in twoRowItems) {
                val itemTitle = extractRunsText(twoRow.optJSONObject("title"))
                val subtitle = extractRunsText(twoRow.optJSONObject("subtitle"))
                val itemBrowseId = twoRow.optJSONObject("navigationEndpoint")
                    ?.optJSONObject("browseEndpoint")
                    ?.optString("browseId")
                    ?: findBrowseEndpointWithPrefix(twoRow, "MPRE")

                if (itemBrowseId.isNullOrBlank() || itemTitle.isBlank()) continue

                val year = Regex("""\b(19\d{2}|20\d{2})\b""").find(subtitle)?.value

                val thumbObj = twoRow.optJSONObject("thumbnailRenderer")
                    ?.optJSONObject("musicThumbnailRenderer")
                    ?.optJSONObject("thumbnail")
                val thumbs = thumbObj?.optJSONArray("thumbnails")
                var thumbUrl: String? = null
                if (thumbs != null && thumbs.length() > 0) {
                    val lastThumb = thumbs.optJSONObject(thumbs.length() - 1)
                    thumbUrl = upgradeThumbnailUrl(lastThumb?.optString("url"))
                }

                val albumItem = AlbumSearchResult(
                    browseId = itemBrowseId,
                    title = itemTitle,
                    artist = artistName,
                    year = year,
                    thumbnailUrl = thumbUrl
                )

                if (isSingleShelf) {
                    singles.add(albumItem)
                } else {
                    albums.add(albumItem)
                }
            }
        }

        return ArtistDetails(
            browseId = browseId,
            name = artistName,
            thumbnailUrl = artistThumbnailUrl,
            listeners = listeners,
            topSongs = topSongs,
            albums = albums,
            singles = singles
        )
    }

    /**
     * Mengekstrak teks dari runs JSON.
     */
    private fun extractRunsText(obj: JSONObject?): String {
        if (obj == null) return ""
        val runs = obj.optJSONArray("runs")
        if (runs != null && runs.length() > 0) {
            val sb = StringBuilder()
            for (i in 0 until runs.length()) {
                sb.append(runs.optJSONObject(i)?.optString("text") ?: "")
            }
            return sb.toString().trim()
        }
        return obj.optString("simpleText").trim()
    }

    /**
     * Mencari browseId dengan awalan tertentu secara rekursif.
     */
    private fun findBrowseEndpointWithPrefix(json: Any, prefix: String): String? {
        when (json) {
            is JSONObject -> {
                val bId = json.optJSONObject("navigationEndpoint")
                    ?.optJSONObject("browseEndpoint")
                    ?.optString("browseId")
                if (!bId.isNullOrBlank() && bId.startsWith(prefix)) {
                    return bId
                }
                val directBId = json.optJSONObject("browseEndpoint")?.optString("browseId")
                if (!directBId.isNullOrBlank() && directBId.startsWith(prefix)) {
                    return directBId
                }
                val keys = json.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val child = json.opt(key) ?: continue
                    val res = findBrowseEndpointWithPrefix(child, prefix)
                    if (res != null) return res
                }
            }
            is JSONArray -> {
                for (i in 0 until json.length()) {
                    val child = json.opt(i) ?: continue
                    val res = findBrowseEndpointWithPrefix(child, prefix)
                    if (res != null) return res
                }
            }
        }
        return null
    }

    /**
     * Mencari objek pertama dengan nama tertentu secara rekursif.
     */
    private fun findFirstObjectNamed(json: Any, names: Set<String>): JSONObject? {
        when (json) {
            is JSONObject -> {
                for (name in names) {
                    val target = json.optJSONObject(name)
                    if (target != null) return target
                }
                val keys = json.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val child = json.opt(key) ?: continue
                    val res = findFirstObjectNamed(child, names)
                    if (res != null) return res
                }
            }
            is JSONArray -> {
                for (i in 0 until json.length()) {
                    val child = json.opt(i) ?: continue
                    val res = findFirstObjectNamed(child, names)
                    if (res != null) return res
                }
            }
        }
        return null
    }

    /**
     * Mencari semua objek bernama targetName secara rekursif.
     */
    private fun findAllObjectsNamed(json: Any, targetName: String, results: MutableList<JSONObject>) {
        when (json) {
            is JSONObject -> {
                val target = json.optJSONObject(targetName)
                if (target != null) {
                    results.add(target)
                }
                val keys = json.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val child = json.opt(key) ?: continue
                    findAllObjectsNamed(child, targetName, results)
                }
            }
            is JSONArray -> {
                for (i in 0 until json.length()) {
                    val child = json.opt(i) ?: continue
                    findAllObjectsNamed(child, targetName, results)
                }
            }
        }
    }

    /**
     * Mengurai respons JSON YouTubei pencarian "Semua" secara defensif.
     */
    private fun parseSearchAllFromResponse(jsonStr: String): SearchAllResult {
        val rootJson = try {
            JSONObject(jsonStr)
        } catch (e: Exception) {
            Log.e(TAG, "Gagal mengurai respons JSON pencarian semua: ${e.message}")
            return SearchAllResult()
        }

        // 1. Cari musicCardShelfRenderer (hasil teratas)
        val cardRenderer = findFirstObjectNamed(rootJson, setOf("musicCardShelfRenderer"))
        var topResult: TopResult? = null
        if (cardRenderer != null) {
            topResult = parseTopResultFromCard(cardRenderer)
        }

        val songs = mutableListOf<Song>()
        val artists = mutableListOf<ArtistSearchResult>()
        val albums = mutableListOf<AlbumSearchResult>()
        val seenVideoIds = mutableSetOf<String>()
        val seenArtistBrowseIds = mutableSetOf<String>()
        val seenAlbumBrowseIds = mutableSetOf<String>()

        topResult?.let { tr ->
            when (tr.type) {
                TopResultType.LAGU -> seenVideoIds.add(tr.id)
                TopResultType.ARTIS -> seenArtistBrowseIds.add(tr.id)
                TopResultType.ALBUM -> seenAlbumBrowseIds.add(tr.id)
            }
        }

        // 2. Kumpulkan semua musicShelfRenderer
        val shelfRenderers = mutableListOf<JSONObject>()
        findAllObjectsNamed(rootJson, "musicShelfRenderer", shelfRenderers)

        val itemRenderers = mutableListOf<JSONObject>()
        if (shelfRenderers.isNotEmpty()) {
            for (shelf in shelfRenderers) {
                findItemRenderers(shelf, itemRenderers)
            }
        } else {
            findItemRenderers(rootJson, itemRenderers)
        }

        for (item in itemRenderers) {
            val videoId = item.optJSONObject("playlistItemData")?.optString("videoId")?.takeIf { it.isNotBlank() }
                ?: findWatchEndpointVideoId(item)

            if (!videoId.isNullOrBlank()) {
                if (seenVideoIds.add(videoId)) {
                    val s = parseSongItem(item, videoId)
                    if (s != null) songs.add(s)
                }
                continue
            }

            val (browseId, pageType) = extractBrowseIdAndPageType(item)

            if (pageType == "MUSIC_PAGE_TYPE_ARTIST" || (browseId != null && browseId.startsWith("UC"))) {
                val effectiveId = browseId ?: findBrowseEndpointWithPrefix(item, "UC")
                if (!effectiveId.isNullOrBlank() && seenArtistBrowseIds.add(effectiveId)) {
                    val art = parseArtistItem(item, effectiveId)
                    if (art != null) artists.add(art)
                }
                continue
            }

            if (pageType == "MUSIC_PAGE_TYPE_ALBUM" || (browseId != null && (browseId.startsWith("MPRE") || browseId.startsWith("FEmusic_library_privately_owned_release")))) {
                val effectiveId = browseId ?: findBrowseEndpointWithPrefix(item, "MPRE")
                if (!effectiveId.isNullOrBlank() && seenAlbumBrowseIds.add(effectiveId)) {
                    val alb = parseAlbumItem(item, effectiveId)
                    if (alb != null) albums.add(alb)
                }
                continue
            }
        }

        // 3. Fallback jika musicCardShelfRenderer tidak ada: pakai item pertama
        if (topResult == null) {
            if (songs.isNotEmpty()) {
                val s = songs.first()
                topResult = TopResult(
                    type = TopResultType.LAGU,
                    id = s.videoId ?: s.id,
                    title = s.title,
                    subtitle = "Lagu • ${s.artist}",
                    thumbnailUrl = s.thumbnailUrl,
                    song = s
                )
            } else if (artists.isNotEmpty()) {
                val a = artists.first()
                topResult = TopResult(
                    type = TopResultType.ARTIS,
                    id = a.browseId,
                    title = a.name,
                    subtitle = "Artis",
                    thumbnailUrl = a.thumbnailUrl,
                    artist = a
                )
            } else if (albums.isNotEmpty()) {
                val alb = albums.first()
                topResult = TopResult(
                    type = TopResultType.ALBUM,
                    id = alb.browseId,
                    title = alb.title,
                    subtitle = if (!alb.year.isNullOrBlank()) "Album • ${alb.artist} • ${alb.year}" else "Album • ${alb.artist}",
                    thumbnailUrl = alb.thumbnailUrl,
                    album = alb
                )
            }
        }

        return SearchAllResult(
            topResult = topResult,
            songs = songs,
            artists = artists,
            albums = albums
        )
    }

    /**
     * Mengurai kartu musicCardShelfRenderer menjadi TopResult.
     */
    private fun parseTopResultFromCard(card: JSONObject): TopResult? {
        val header = card.optJSONObject("header")?.optJSONObject("musicCardShelfHeaderBasicRenderer")
        val titleObj = header?.optJSONObject("title")
        val titleRuns = titleObj?.optJSONArray("runs")
        val title = extractRunsText(titleObj)
        if (title.isBlank()) return null

        val subtitleObj = card.optJSONObject("subtitle")
        val subtitle = extractRunsText(subtitleObj)

        val thumbnailObj = card.optJSONObject("thumbnail")
            ?.optJSONObject("musicThumbnailRenderer")
            ?.optJSONObject("thumbnail")
        val thumbnails = thumbnailObj?.optJSONArray("thumbnails")
        var imageUrl: String? = null
        if (thumbnails != null && thumbnails.length() > 0) {
            imageUrl = thumbnails.optJSONObject(thumbnails.length() - 1)?.optString("url")
        }
        val finalThumbnailUrl = upgradeThumbnailUrl(imageUrl)

        val firstRun = titleRuns?.optJSONObject(0)
        val navEndpoint = firstRun?.optJSONObject("navigationEndpoint")
            ?: header?.optJSONObject("navigationEndpoint")
            ?: card.optJSONObject("title")?.optJSONObject("navigationEndpoint")

        val watchEndpoint = navEndpoint?.optJSONObject("watchEndpoint")
            ?: findFirstObjectNamed(card, setOf("watchEndpoint"))
        val videoId = watchEndpoint?.optString("videoId")?.takeIf { it.isNotBlank() }

        val browseEndpoint = navEndpoint?.optJSONObject("browseEndpoint")
            ?: findFirstObjectNamed(card, setOf("browseEndpoint"))
        val browseId = browseEndpoint?.optString("browseId")?.takeIf { it.isNotBlank() }
        val pageType = browseEndpoint?.optJSONObject("browseEndpointContextSupportedConfigs")
            ?.optJSONObject("browseEndpointContextMusicConfig")
            ?.optString("pageType")

        val subLower = subtitle.lowercase(Locale.ROOT)

        return when {
            (browseId != null && browseId.startsWith("UC")) || pageType == "MUSIC_PAGE_TYPE_ARTIST" || subLower.startsWith("artis") || subLower.startsWith("artist") -> {
                val effectiveId = browseId ?: findBrowseEndpointWithPrefix(card, "UC") ?: ""
                val artist = ArtistSearchResult(
                    browseId = effectiveId,
                    name = title,
                    thumbnailUrl = finalThumbnailUrl
                )
                TopResult(
                    type = TopResultType.ARTIS,
                    id = effectiveId,
                    title = title,
                    subtitle = if (subtitle.isNotBlank()) subtitle else "Artis",
                    thumbnailUrl = finalThumbnailUrl,
                    artist = artist
                )
            }
            (browseId != null && browseId.startsWith("MPRE")) || pageType == "MUSIC_PAGE_TYPE_ALBUM" || subLower.startsWith("album") || subLower.startsWith("ep") || subLower.startsWith("single") -> {
                val effectiveId = browseId ?: findBrowseEndpointWithPrefix(card, "MPRE") ?: ""
                val artistName = subtitle.split("•").getOrNull(1)?.trim() ?: ""
                val album = AlbumSearchResult(
                    browseId = effectiveId,
                    title = title,
                    artist = artistName,
                    thumbnailUrl = finalThumbnailUrl
                )
                TopResult(
                    type = TopResultType.ALBUM,
                    id = effectiveId,
                    title = title,
                    subtitle = if (subtitle.isNotBlank()) subtitle else "Album",
                    thumbnailUrl = finalThumbnailUrl,
                    album = album
                )
            }
            !videoId.isNullOrBlank() || subLower.startsWith("lagu") || subLower.startsWith("song") || subLower.startsWith("video") -> {
                val effectiveVideoId = videoId ?: findWatchEndpointVideoId(card) ?: ""
                val artistName = subtitle.split("•").getOrNull(1)?.trim() ?: subtitle
                val song = Song(
                    id = "yt_$effectiveVideoId",
                    title = title,
                    artist = if (artistName.isBlank()) "Artis tidak diketahui" else artistName,
                    album = "",
                    durationMs = 0L,
                    thumbnailUrl = finalThumbnailUrl,
                    videoId = effectiveVideoId
                )
                TopResult(
                    type = TopResultType.LAGU,
                    id = effectiveVideoId,
                    title = title,
                    subtitle = if (subtitle.isNotBlank()) subtitle else "Lagu",
                    thumbnailUrl = finalThumbnailUrl,
                    song = song
                )
            }
            else -> {
                if (!videoId.isNullOrBlank()) {
                    val song = Song(
                        id = "yt_$videoId",
                        title = title,
                        artist = subtitle.ifBlank { "Artis tidak diketahui" },
                        thumbnailUrl = finalThumbnailUrl,
                        videoId = videoId
                    )
                    TopResult(
                        type = TopResultType.LAGU,
                        id = videoId,
                        title = title,
                        subtitle = subtitle.ifBlank { "Lagu" },
                        thumbnailUrl = finalThumbnailUrl,
                        song = song
                    )
                } else {
                    val effectiveId = browseId ?: ""
                    TopResult(
                        type = TopResultType.ARTIS,
                        id = effectiveId,
                        title = title,
                        subtitle = subtitle.ifBlank { "Artis" },
                        thumbnailUrl = finalThumbnailUrl,
                        artist = ArtistSearchResult(browseId = effectiveId, name = title, thumbnailUrl = finalThumbnailUrl)
                    )
                }
            }
        }
    }

    private fun extractBrowseIdAndPageType(item: JSONObject): Pair<String?, String?> {
        val flexCols = item.optJSONArray("flexColumns")
        val itemNav = item.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")
        var browseId = itemNav?.optString("browseId")?.takeIf { it.isNotBlank() }
        var pageType = itemNav?.optJSONObject("browseEndpointContextSupportedConfigs")
            ?.optJSONObject("browseEndpointContextMusicConfig")
            ?.optString("pageType")?.takeIf { it.isNotBlank() }

        if (browseId == null && flexCols != null && flexCols.length() > 0) {
            val titleObj = flexCols.optJSONObject(0)
                ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                ?.optJSONObject("text")
            val runs = titleObj?.optJSONArray("runs")
            if (runs != null && runs.length() > 0) {
                val runEp = runs.optJSONObject(0)?.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")
                if (runEp != null) {
                    browseId = runEp.optString("browseId")?.takeIf { it.isNotBlank() }
                    pageType = runEp.optJSONObject("browseEndpointContextSupportedConfigs")
                        ?.optJSONObject("browseEndpointContextMusicConfig")
                        ?.optString("pageType")?.takeIf { it.isNotBlank() }
                }
            }
        }
        return browseId to pageType
    }

    private fun parseSongItem(item: JSONObject, videoId: String): Song? {
        val flexColumns = item.optJSONArray("flexColumns")
        val col0 = flexColumns?.optJSONObject(0)
        val text0 = col0?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")?.optJSONObject("text")
        val title = extractRunsText(text0).ifBlank { "Tanpa Judul" }

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
                    if (seg.isNotEmpty()) segments.add(seg)
                    currentSeg.clear()
                } else {
                    currentSeg.append(runText)
                }
            }
            val lastSeg = currentSeg.toString().trim()
            if (lastSeg.isNotEmpty()) segments.add(lastSeg)
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

        val thumbnailObj = item.optJSONObject("thumbnail")
            ?.optJSONObject("musicThumbnailRenderer")
            ?.optJSONObject("thumbnail")
        val thumbnails = thumbnailObj?.optJSONArray("thumbnails")
        var imageUrl: String? = null
        if (thumbnails != null && thumbnails.length() > 0) {
            val lastThumb = thumbnails.optJSONObject(thumbnails.length() - 1)
            imageUrl = lastThumb?.optString("url")
        }

        return Song(
            id = "yt_$videoId",
            title = title,
            artist = if (artist.isBlank()) "Artis tidak diketahui" else artist,
            album = album,
            durationMs = durationMs,
            thumbnailUrl = upgradeThumbnailUrl(imageUrl),
            contentUri = null,
            dateAdded = 0L,
            albumId = 0L,
            videoId = videoId,
            artistBrowseId = artistBrowseId,
            albumBrowseId = albumBrowseId
        )
    }

    private fun parseArtistItem(item: JSONObject, browseId: String): ArtistSearchResult? {
        val flexCols = item.optJSONArray("flexColumns") ?: return null
        val titleObj = flexCols.optJSONObject(0)
            ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
            ?.optJSONObject("text")
        val name = extractRunsText(titleObj)
        if (name.isBlank()) return null

        val thumbnailObj = item.optJSONObject("thumbnail")
            ?.optJSONObject("musicThumbnailRenderer")
            ?.optJSONObject("thumbnail")
        val thumbnails = thumbnailObj?.optJSONArray("thumbnails")
        var imageUrl: String? = null
        if (thumbnails != null && thumbnails.length() > 0) {
            val lastThumb = thumbnails.optJSONObject(thumbnails.length() - 1)
            imageUrl = lastThumb?.optString("url")
        }

        return ArtistSearchResult(
            browseId = browseId,
            name = name,
            thumbnailUrl = upgradeThumbnailUrl(imageUrl)
        )
    }

    private fun parseAlbumItem(item: JSONObject, browseId: String): AlbumSearchResult? {
        val flexCols = item.optJSONArray("flexColumns") ?: return null
        val titleObj = flexCols.optJSONObject(0)
            ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
            ?.optJSONObject("text")
        val title = extractRunsText(titleObj)
        if (title.isBlank()) return null

        var artist = "Artis tidak diketahui"
        var year: String? = null
        if (flexCols.length() > 1) {
            val subtitleRuns = flexCols.optJSONObject(1)
                ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                ?.optJSONObject("text")
                ?.optJSONArray("runs")
            if (subtitleRuns != null) {
                val segs = mutableListOf<String>()
                val curr = StringBuilder()
                for (i in 0 until subtitleRuns.length()) {
                    val r = subtitleRuns.optJSONObject(i) ?: continue
                    val t = r.optString("text") ?: ""
                    if (t.contains("•")) {
                        val s = curr.toString().trim()
                        if (s.isNotEmpty()) segs.add(s)
                        curr.clear()
                    } else {
                        curr.append(t)
                    }
                }
                val lastS = curr.toString().trim()
                if (lastS.isNotEmpty()) segs.add(lastS)

                val nonType = segs.filter { it.lowercase(Locale.ROOT) !in setOf("album", "ep", "single", "singel") }
                if (nonType.isNotEmpty()) {
                    artist = nonType[0]
                    if (nonType.size > 1 && nonType[1].matches(Regex("""^\d{4}$"""))) {
                        year = nonType[1]
                    }
                }
            }
        }

        val thumbnailObj = item.optJSONObject("thumbnail")
            ?.optJSONObject("musicThumbnailRenderer")
            ?.optJSONObject("thumbnail")
        val thumbnails = thumbnailObj?.optJSONArray("thumbnails")
        var imageUrl: String? = null
        if (thumbnails != null && thumbnails.length() > 0) {
            val lastThumb = thumbnails.optJSONObject(thumbnails.length() - 1)
            imageUrl = lastThumb?.optString("url")
        }

        return AlbumSearchResult(
            browseId = browseId,
            title = title,
            artist = artist,
            year = year,
            thumbnailUrl = upgradeThumbnailUrl(imageUrl)
        )
    }
}
