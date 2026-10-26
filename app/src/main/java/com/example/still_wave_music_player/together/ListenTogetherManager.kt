package com.example.still_wave_music_player.together

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.ExoPlayer
import com.example.still_wave_music_player.data.model.Song
import com.example.still_wave_music_player.playback.SleepTimerManager // TAMBAHAN IMPORT
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.UnknownHostException
import java.util.Collections
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

/**
 * Status koneksi Listening Together.
 */
enum class TogetherStatus {
    TidakTerhubung, Menghubungkan, Terhubung
}

/**
 * State UI Listening Together.
 */
data class TogetherUiState(
    val status: TogetherStatus = TogetherStatus.TidakTerhubung,
    val kodeRuang: String? = null,
    val isPartnerOnline: Boolean = false,
    val partnerName: String? = null,
    val pesanGalat: String? = null,
    val isHost: Boolean = false,
    val localOnlyNote: String? = null
)

/**
 * Pengelola Listening Together via Firebase Realtime Database REST API.
 * Kedua perangkat dapat mengontrol pemutaran musik secara peer-to-peer.
 */
object ListenTogetherManager {

    private const val TAG = "MusikPlayer"
    private const val ROOM_CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _uiState = MutableStateFlow(TogetherUiState())
    val uiState: StateFlow<TogetherUiState> = _uiState.asStateFlow()

    private var exoPlayer: ExoPlayer? = null
    private var appContext: Context? = null

    private var sseJob: Job? = null
    private var heartbeatJob: Job? = null
    private var calibrationJob: Job? = null
    private var driftJob: Job? = null
    private var debounceSyncJob: Job? = null
    private var debounceRefetchJob: Job? = null

    private val localRev = AtomicLong(0L)
    @Volatile
    private var applyingRemote = false

    @Volatile
    private var isAutoPaused = false
    private var skipNextSync = false

    private var lastSyncedQueueHash: Int = 0
    private var lastRemoteState: RoomState? = null

    // Estimasi selisih jam server vs lokal
    private val timeOffsets = Collections.synchronizedList(mutableListOf<Long>())
    @Volatile
    private var currentClockOffsetMs: Long = 0L

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val sseHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .build()

    fun isConnected(): Boolean = _uiState.value.status == TogetherStatus.Terhubung

    /**
     * Menentukan apakah perangkat ini boleh memperpanjang radio otomatis saat di ruang.
     * Hanya host yang boleh memperpanjang radio, perangkat tamu menyelaraskan via antrean.
     */
    fun canExtendRadio(): Boolean {
        return !isConnected() || _uiState.value.isHost
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            onLocalPlayerEvent("onIsPlayingChanged")
        }
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            if (!playWhenReady && (reason == Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS || reason == Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_BECOMING_NOISY)) {
                // Jeda karena telepon/alarm/headset lepas
                isAutoPaused = true
            } else if (playWhenReady) {
                if (isAutoPaused) {
                    isAutoPaused = false
                    skipNextSync = true // Cegah pengiriman state lama ke ruang

                    // Geser posisi ke tempat yang seharusnya (posisi teman)
                    val state = lastRemoteState
                    if (state != null && state.isPlaying) {
                        val nowServer = serverNow()
                        val expectedPositionMs = state.positionMs + (nowServer - state.atServerMs).coerceAtLeast(0L)
                        mainHandler.post {
                            exoPlayer?.seekTo(expectedPositionMs.coerceAtLeast(0L))
                        }
                    }
                }
            }
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            if (reason == Player.DISCONTINUITY_REASON_SEEK) {
                onLocalPlayerEvent("onPositionDiscontinuity_SEEK")
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            onLocalPlayerEvent("onMediaItemTransition")
        }

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            onLocalPlayerEvent("onTimelineChanged")
        }
    }

    /**
     * Mendaftarkan ExoPlayer dari MusicService.
     */
    fun attachPlayer(context: Context, player: ExoPlayer) {
        mainHandler.post {
            appContext = context.applicationContext
            exoPlayer = player
            player.addListener(playerListener)
        }
    }

    /**
     * Melepas ExoPlayer saat MusicService dihancurkan.
     */
    fun detachPlayer() {
        mainHandler.post {
            exoPlayer?.removeListener(playerListener)
            exoPlayer = null
        }
    }

    private fun onLocalPlayerEvent(reason: String) {
        // Cek isAutoPaused (jika sedang dijeda oleh telepon/headset)
        if (applyingRemote || !isConnected() || SleepTimerManager.isFadingOut || isAutoPaused) return

        // Jika baru lanjut dari jeda otomatis, abaikan sinkronisasi pertama
        if (skipNextSync) {
            skipNextSync = false
            return
        }

        debounceSyncJob?.cancel()
        debounceSyncJob = managerScope.launch {
            delay(300L)
            // Cek ulang setelah delay
            if (!applyingRemote && isConnected() && !SleepTimerManager.isFadingOut && !isAutoPaused) {
                sendLocalState(reason)
            }
        }
    }


    private fun serverNow(): Long = System.currentTimeMillis() + currentClockOffsetMs

    /**
     * Membuat ruang baru.
     */
    fun createRoom(context: Context) {
        val prefs = TogetherPreferences(context)
        val rawServerUrl = prefs.serverUrl.ifBlank { TogetherPreferences.DEFAULT_SERVER_URL }
        val validUrlResult = TogetherPreferences.validateServerUrl(rawServerUrl)
        if (validUrlResult.isFailure) {
            _uiState.update { it.copy(pesanGalat = "Alamat server tidak valid") }
            return
        }
        val serverUrl = validUrlResult.getOrThrow()

        _uiState.update { it.copy(status = TogetherStatus.Menghubungkan, pesanGalat = null) }

        managerScope.launch {
            try {
                var generatedCode = ""
                var foundAvailableCode = false

                for (attempt in 1..5) {
                    val candidate = (1..6).map { ROOM_CODE_CHARS[Random.nextInt(ROOM_CODE_CHARS.length)] }.joinToString("")
                    val checkUrl = "$serverUrl/rooms/$candidate.json"
                    val request = Request.Builder().url(checkUrl).get().build()
                    val response = httpClient.newCall(request).execute()
                    val body = response.body?.string()?.trim()
                    if (response.isSuccessful && (body == null || body == "null")) {
                        generatedCode = candidate
                        foundAvailableCode = true
                        break
                    }
                }

                if (!foundAvailableCode) {
                    _uiState.update { it.copy(status = TogetherStatus.TidakTerhubung, pesanGalat = "Tidak dapat membuat kode ruang") }
                    return@launch
                }

                val deviceId = prefs.deviceId
                val nickname = prefs.nickname

                // Dapatkan antrean dan lagu aktif saat ini dari player
                var localQueue = emptyList<RoomSong>()
                var currentVideoId = ""
                var currentIndex = 0
                var currentPositionMs = 0L
                var isPlaying = false
                var hasLocalOnly = false

                withContext(Dispatchers.Main) {
                    val p = exoPlayer
                    if (p != null) {
                        val count = p.mediaItemCount
                        val q = mutableListOf<RoomSong>()
                        for (i in 0 until count) {
                            val item = p.getMediaItemAt(i)
                            val vId = item.mediaId
                            val isOnline = item.requestMetadata.mediaUri?.scheme == "ytm" ||
                                    item.localConfiguration?.uri?.scheme == "ytm" ||
                                    !vId.startsWith("content://") && !vId.startsWith("file://")

                            if (isOnline && vId.isNotBlank()) {
                                val meta = item.mediaMetadata
                                q.add(
                                    RoomSong(
                                        videoId = vId,
                                        title = meta.title?.toString() ?: "Tanpa Judul",
                                        artist = meta.artist?.toString() ?: "Artis tidak diketahui",
                                        album = meta.albumTitle?.toString(),
                                        thumbnailUrl = meta.artworkUri?.toString()
                                    )
                                )
                            } else {
                                hasLocalOnly = true
                            }
                        }
                        localQueue = q
                        currentIndex = p.currentMediaItemIndex.coerceIn(0, (count - 1).coerceAtLeast(0))
                        currentPositionMs = p.currentPosition.coerceAtLeast(0L)
                        isPlaying = p.isPlaying
                        val currentItem = p.currentMediaItem
                        currentVideoId = currentItem?.mediaId?.takeIf { !it.startsWith("content://") } ?: ""
                    }
                }

                localRev.set(1L)
                val initialRev = 1L
                lastSyncedQueueHash = localQueue.map { it.videoId }.hashCode()

                val rootJson = JSONObject().apply {
                    put("host", deviceId)
                    put("members", JSONObject().apply {
                        put(deviceId, JSONObject().apply {
                            put("name", nickname)
                            put("lastSeen", JSONObject().put(".sv", "timestamp"))
                        })
                    })
                    val queueArr = JSONArray()
                    localQueue.forEach { rs ->
                        queueArr.put(JSONObject().apply {
                            put("videoId", rs.videoId)
                            put("title", rs.title)
                            put("artist", rs.artist)
                            if (rs.album != null) put("album", rs.album)
                            if (rs.thumbnailUrl != null) put("thumbnailUrl", rs.thumbnailUrl)
                        })
                    }
                    put("queue", queueArr)
                    put("state", JSONObject().apply {
                        put("videoId", currentVideoId)
                        put("index", currentIndex)
                        put("positionMs", currentPositionMs)
                        put("isPlaying", isPlaying)
                        put("at", JSONObject().put(".sv", "timestamp"))
                        put("by", deviceId)
                        put("rev", initialRev)
                    })
                }

                val patchUrl = "$serverUrl/rooms/$generatedCode.json?print=silent"
                val patchReq = Request.Builder()
                    .url(patchUrl)
                    .patch(rootJson.toString().toRequestBody(JSON_MEDIA_TYPE))
                    .build()

                val patchRes = httpClient.newCall(patchReq).execute()
                if (!patchRes.isSuccessful) {
                    _uiState.update { it.copy(status = TogetherStatus.TidakTerhubung, pesanGalat = "Tidak dapat menghubungi server") }
                    return@launch
                }

                Log.d(TAG, "ListenTogether: ruang berhasil dibuat")

                _uiState.update {
                    it.copy(
                        status = TogetherStatus.Terhubung,
                        kodeRuang = generatedCode,
                        isHost = true,
                        isPartnerOnline = false,
                        partnerName = null,
                        pesanGalat = null,
                        localOnlyNote = if (hasLocalOnly) "Lagu lokal tidak dibagikan" else null
                    )
                }

                startRoomServices(serverUrl, generatedCode, deviceId, nickname)

            } catch (e: UnknownHostException) {
                _uiState.update { it.copy(status = TogetherStatus.TidakTerhubung, pesanGalat = "Tidak ada koneksi internet") }
            } catch (e: Exception) {
                _uiState.update { it.copy(status = TogetherStatus.TidakTerhubung, pesanGalat = "Tidak dapat menghubungi server") }
            }
        }
    }

    /**
     * Masuk ke ruang yang sudah ada.
     */
    fun joinRoom(context: Context, rawCode: String) {
        val code = rawCode.trim().uppercase(Locale.ROOT)
        if (code.length != 6) {
            _uiState.update { it.copy(pesanGalat = "Kode ruang tidak valid") }
            return
        }

        val prefs = TogetherPreferences(context)
        val rawServerUrl = prefs.serverUrl.ifBlank { TogetherPreferences.DEFAULT_SERVER_URL }
        val validUrlResult = TogetherPreferences.validateServerUrl(rawServerUrl)
        if (validUrlResult.isFailure) {
            _uiState.update { it.copy(pesanGalat = "Alamat server tidak valid") }
            return
        }
        val serverUrl = validUrlResult.getOrThrow()

        _uiState.update { it.copy(status = TogetherStatus.Menghubungkan, pesanGalat = null) }

        managerScope.launch {
            try {
                val getUrl = "$serverUrl/rooms/$code.json"
                val req = Request.Builder().url(getUrl).get().build()
                val res = httpClient.newCall(req).execute()
                val bodyStr = res.body?.string()?.trim()

                if (!res.isSuccessful || bodyStr == null || bodyStr == "null") {
                    _uiState.update { it.copy(status = TogetherStatus.TidakTerhubung, pesanGalat = "Kode ruang tidak ditemukan") }
                    return@launch
                }

                val roomObj = JSONObject(bodyStr)
                val deviceId = prefs.deviceId
                val nickname = prefs.nickname
                val hostId = roomObj.optString("host")
                val isHost = hostId == deviceId

                // Daftarkan sebagai member
                val memberUrl = "$serverUrl/rooms/$code/members/$deviceId.json?print=silent"
                val memberJson = JSONObject().apply {
                    put("name", nickname)
                    put("lastSeen", JSONObject().put(".sv", "timestamp"))
                }
                val memberReq = Request.Builder()
                    .url(memberUrl)
                    .patch(memberJson.toString().toRequestBody(JSON_MEDIA_TYPE))
                    .build()
                httpClient.newCall(memberReq).execute()

                Log.d(TAG, "ListenTogether: bergabung ke ruang")

                _uiState.update {
                    it.copy(
                        status = TogetherStatus.Terhubung,
                        kodeRuang = code,
                        isHost = isHost,
                        pesanGalat = null
                    )
                }

                // Terapkan keadaan awal dari ruang
                applyRoomSnapshot(roomObj, deviceId)

                startRoomServices(serverUrl, code, deviceId, nickname)

            } catch (e: UnknownHostException) {
                _uiState.update { it.copy(status = TogetherStatus.TidakTerhubung, pesanGalat = "Tidak ada koneksi internet") }
            } catch (e: Exception) {
                _uiState.update { it.copy(status = TogetherStatus.TidakTerhubung, pesanGalat = "Tidak dapat menghubungi server") }
            }
        }
    }

    private fun startRoomServices(
        serverUrl: String,
        code: String,
        deviceId: String,
        nickname: String
    ) {
        startCalibration(serverUrl, code, deviceId)
        startHeartbeat(serverUrl, code, deviceId)
        startDriftCorrection()
        startSseStream(serverUrl, code, deviceId)
    }

    private fun startCalibration(serverUrl: String, code: String, deviceId: String) {
        calibrationJob?.cancel()
        calibrationJob = managerScope.launch {
            while (isActive && isConnected()) {
                calibrateClockOffset(serverUrl, code, deviceId)
                delay(60_000L)
            }
        }
    }

    private suspend fun calibrateClockOffset(serverUrl: String, code: String, deviceId: String) {
        try {
            val patchUrl = "$serverUrl/rooms/$code/members/$deviceId.json?print=silent"
            val patchBody = JSONObject().apply {
                put("lastSeen", JSONObject().put(".sv", "timestamp"))
            }.toString().toRequestBody(JSON_MEDIA_TYPE)

            val t0 = System.currentTimeMillis()
            httpClient.newCall(Request.Builder().url(patchUrl).patch(patchBody).build()).execute()

            val getUrl = "$serverUrl/rooms/$code/members/$deviceId/lastSeen.json"
            val getRes = httpClient.newCall(Request.Builder().url(getUrl).get().build()).execute()
            val t1 = System.currentTimeMillis()

            val body = getRes.body?.string()?.trim()
            val serverTs = body?.toLongOrNull()
            if (serverTs != null && serverTs > 0L) {
                val roundTrip = (t0 + t1) / 2
                val sampleOffset = serverTs - roundTrip
                timeOffsets.add(sampleOffset)
                if (timeOffsets.size > 3) timeOffsets.removeAt(0)
                val sorted = timeOffsets.sorted()
                currentClockOffsetMs = sorted[sorted.size / 2]
            }
        } catch (_: Exception) {
        }
    }

    private fun startHeartbeat(serverUrl: String, code: String, deviceId: String) {
        heartbeatJob?.cancel()
        heartbeatJob = managerScope.launch {
            while (isActive && isConnected()) {
                delay(15_000L)
                if (!isConnected()) break
                try {
                    val patchUrl = "$serverUrl/rooms/$code/members/$deviceId.json?print=silent"
                    val patchBody = JSONObject().apply {
                        put("lastSeen", JSONObject().put(".sv", "timestamp"))
                    }.toString().toRequestBody(JSON_MEDIA_TYPE)
                    httpClient.newCall(Request.Builder().url(patchUrl).patch(patchBody).build()).execute()
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun startDriftCorrection() {
        driftJob?.cancel()
        driftJob = managerScope.launch {
            while (isActive && isConnected()) {
                delay(5_000L)
                if (!isConnected()) break

                val state = lastRemoteState ?: continue
                if (state.by == TogetherPreferences(appContext ?: continue).deviceId) continue
                if (!state.isPlaying) continue

                val nowServer = serverNow()
                val expectedPosition = state.positionMs + (nowServer - state.atServerMs).coerceAtLeast(0L)

                withContext(Dispatchers.Main) {
                    val p = exoPlayer ?: return@withContext
                    if (!p.isPlaying) return@withContext
                    val currentPos = p.currentPosition
                    val diff = Math.abs(currentPos - expectedPosition)
                    if (diff > 1500L) {
                        applyingRemote = true
                        p.seekTo(expectedPosition.coerceAtLeast(0L))
                        mainHandler.postDelayed({ applyingRemote = false }, 250L)
                    }
                }
            }
        }
    }

    private fun startSseStream(serverUrl: String, code: String, deviceId: String) {
        sseJob?.cancel()
        sseJob = managerScope.launch {
            val backoffs = listOf(1000L, 2000L, 4000L, 8000L, 15000L)
            var backoffIndex = 0

            while (isActive && isConnected()) {
                try {
                    val streamUrl = "$serverUrl/rooms/$code.json"
                    val request = Request.Builder()
                        .url(streamUrl)
                        .header("Accept", "text/event-stream")
                        .build()

                    val response: Response = sseHttpClient.newCall(request).execute()
                    if (!response.isSuccessful) {
                        throw IOException("SSE HTTP ${response.code}")
                    }

                    backoffIndex = 0 // Reset jeda jika sukses terhubung
                    triggerFullRefetch(serverUrl, code, deviceId, debounceMs = 0L)

                    val stream = response.body?.byteStream()
                    if (stream != null) {
                        val reader = BufferedReader(InputStreamReader(stream, Charsets.UTF_8))
                        var line: String? = null
                        var currentEvent: String? = null

                        while (isActive && isConnected() && reader.readLine().also { line = it } != null) {
                            val l = line?.trim() ?: continue
                            if (l.isEmpty()) {
                                currentEvent = null
                                continue
                            }
                            if (l.startsWith("event:")) {
                                currentEvent = l.removePrefix("event:").trim()
                            } else if (l.startsWith("data:")) {
                                if (currentEvent == "put" || currentEvent == "patch") {
                                    triggerFullRefetch(serverUrl, code, deviceId, debounceMs = 150L)
                                } else if (currentEvent == "cancel") {
                                    _uiState.update { it.copy(pesanGalat = "Koneksi ruang dibatalkan oleh server") }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    if (!isActive || !isConnected()) break
                    val waitMs = backoffs[backoffIndex.coerceIn(0, backoffs.lastIndex)]
                    if (backoffIndex < backoffs.lastIndex) backoffIndex++
                    delay(waitMs)
                    if (isConnected()) {
                        triggerFullRefetch(serverUrl, code, deviceId, debounceMs = 0L)
                    }
                }
            }
        }
    }

    private fun triggerFullRefetch(serverUrl: String, code: String, deviceId: String, debounceMs: Long) {
        debounceRefetchJob?.cancel()
        debounceRefetchJob = managerScope.launch {
            if (debounceMs > 0L) delay(debounceMs)
            if (!isConnected()) return@launch
            try {
                val getUrl = "$serverUrl/rooms/$code.json"
                val res = httpClient.newCall(Request.Builder().url(getUrl).get().build()).execute()
                val bodyStr = res.body?.string()?.trim()
                if (res.isSuccessful && bodyStr != null && bodyStr != "null") {
                    val roomObj = JSONObject(bodyStr)
                    applyRoomSnapshot(roomObj, deviceId)
                }
            } catch (_: Exception) {
            }
        }
    }

    private suspend fun applyRoomSnapshot(roomObj: JSONObject, myDeviceId: String) {
        val membersObj = roomObj.optJSONObject("members")
        var partnerOnline = false
        var pName: String? = null

        val nowServer = serverNow()
        if (membersObj != null) {
            val keys = membersObj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                if (k != myDeviceId) {
                    val m = membersObj.optJSONObject(k) ?: continue
                    pName = m.optString("name").ifBlank { "Teman" }
                    val lastSeen = m.optLong("lastSeen", 0L)
                    if (lastSeen > 0L && Math.abs(nowServer - lastSeen) <= 45_000L) {
                        partnerOnline = true
                    }
                }
            }
        }

        _uiState.update {
            it.copy(
                isPartnerOnline = partnerOnline,
                partnerName = pName
            )
        }

        val queueArr = roomObj.optJSONArray("queue")
        val remoteQueue = mutableListOf<RoomSong>()
        if (queueArr != null) {
            for (i in 0 until queueArr.length()) {
                val item = queueArr.optJSONObject(i) ?: continue
                val vId = item.optString("videoId")
                if (vId.isNotBlank()) {
                    remoteQueue.add(
                        RoomSong(
                            videoId = vId,
                            title = item.optString("title", "Tanpa Judul"),
                            artist = item.optString("artist", "Artis tidak diketahui"),
                            album = item.optString("album").takeIf { it.isNotBlank() },
                            thumbnailUrl = item.optString("thumbnailUrl").takeIf { it.isNotBlank() }
                        )
                    )
                }
            }
        }

        val stateObj = roomObj.optJSONObject("state") ?: return
        val byDeviceId = stateObj.optString("by")
        val remoteRev = stateObj.optLong("rev", 0L)
        val videoId = stateObj.optString("videoId")
        val index = stateObj.optInt("index", 0)
        val positionMs = stateObj.optLong("positionMs", 0L)
        val isPlaying = stateObj.optBoolean("isPlaying", false)
        val atServerMs = stateObj.optLong("at", nowServer)

        val roomState = RoomState(
            videoId = videoId,
            index = index,
            positionMs = positionMs,
            isPlaying = isPlaying,
            atServerMs = atServerMs,
            by = byDeviceId,
            rev = remoteRev
        )
        lastRemoteState = roomState

        // Hanya terapkan jika state berasal dari pasangan (bukan diri sendiri)
        if (byDeviceId == myDeviceId) return

        val expectedPositionMs = if (isPlaying) {
            positionMs + (nowServer - atServerMs).coerceAtLeast(0L)
        } else {
            positionMs
        }

        withContext(Dispatchers.Main) {
            val player = exoPlayer ?: return@withContext
            applyingRemote = true

            try {
                // Periksa apakah antrean berbeda
                val localVideoIds = (0 until player.mediaItemCount).map { player.getMediaItemAt(it).mediaId }
                val remoteVideoIds = remoteQueue.map { it.videoId }

                if (remoteVideoIds.isNotEmpty() && remoteVideoIds != localVideoIds) {
                    val mediaItems = remoteQueue.map { createOnlineMediaItem(it) }
                    val targetIndex = index.coerceIn(0, mediaItems.lastIndex)
                    player.setMediaItems(mediaItems, targetIndex, expectedPositionMs.coerceAtLeast(0L))
                    player.prepare()
                    if (isPlaying) player.play() else player.pause()
                    lastSyncedQueueHash = remoteVideoIds.hashCode()
                } else {
                    val currentIndex = player.currentMediaItemIndex
                    val currentItem = player.currentMediaItem
                    val currentVideoId = currentItem?.mediaId

                    if (currentIndex != index || (videoId.isNotBlank() && currentVideoId != videoId)) {
                        val targetIndex = index.coerceIn(0, (player.mediaItemCount - 1).coerceAtLeast(0))
                        player.seekTo(targetIndex, expectedPositionMs.coerceAtLeast(0L))
                        if (isPlaying) player.play() else player.pause()
                    } else {
                        val posDiff = Math.abs(player.currentPosition - expectedPositionMs)
                        if (posDiff > 1000L) {
                            player.seekTo(expectedPositionMs.coerceAtLeast(0L))
                        }
                        if (player.isPlaying != isPlaying) {
                            if (isPlaying) player.play() else player.pause()
                        }
                    }
                }
            } finally {
                mainHandler.postDelayed({
                    applyingRemote = false
                }, 300L)
            }
        }
    }

    private suspend fun sendLocalState(reason: String) {
        val serverUrl = TogetherPreferences(appContext ?: return).serverUrl
        val code = _uiState.value.kodeRuang ?: return
        val deviceId = TogetherPreferences(appContext ?: return).deviceId

        var localQueue = emptyList<RoomSong>()
        var currentVideoId = ""
        var currentIndex = 0
        var currentPositionMs = 0L
        var isPlaying = false
        var hasLocalOnly = false

        withContext(Dispatchers.Main) {
            val p = exoPlayer ?: return@withContext
            val count = p.mediaItemCount
            val q = mutableListOf<RoomSong>()
            for (i in 0 until count) {
                val item = p.getMediaItemAt(i)
                val vId = item.mediaId
                val isOnline = item.requestMetadata.mediaUri?.scheme == "ytm" ||
                        item.localConfiguration?.uri?.scheme == "ytm" ||
                        (!vId.startsWith("content://") && !vId.startsWith("file://"))

                if (isOnline && vId.isNotBlank()) {
                    val meta = item.mediaMetadata
                    q.add(
                        RoomSong(
                            videoId = vId,
                            title = meta.title?.toString() ?: "Tanpa Judul",
                            artist = meta.artist?.toString() ?: "Artis tidak diketahui",
                            album = meta.albumTitle?.toString(),
                            thumbnailUrl = meta.artworkUri?.toString()
                        )
                    )
                } else {
                    hasLocalOnly = true
                }
            }
            localQueue = q
            currentIndex = p.currentMediaItemIndex.coerceIn(0, (count - 1).coerceAtLeast(0))
            currentPositionMs = p.currentPosition.coerceAtLeast(0L)
            isPlaying = p.isPlaying
            val currentItem = p.currentMediaItem
            currentVideoId = currentItem?.mediaId?.takeIf { !it.startsWith("content://") } ?: ""
        }

        if (hasLocalOnly) {
            _uiState.update { it.copy(localOnlyNote = "Lagu lokal tidak dibagikan") }
        }

        val queueHash = localQueue.map { it.videoId }.hashCode()
        val queueChanged = queueHash != lastSyncedQueueHash
        val rev = localRev.incrementAndGet()

        try {
            if (queueChanged && localQueue.isNotEmpty()) {
                val rootPatch = JSONObject().apply {
                    val queueArr = JSONArray()
                    localQueue.forEach { rs ->
                        queueArr.put(JSONObject().apply {
                            put("videoId", rs.videoId)
                            put("title", rs.title)
                            put("artist", rs.artist)
                            if (rs.album != null) put("album", rs.album)
                            if (rs.thumbnailUrl != null) put("thumbnailUrl", rs.thumbnailUrl)
                        })
                    }
                    put("queue", queueArr)
                    put("state", JSONObject().apply {
                        put("videoId", currentVideoId)
                        put("index", currentIndex)
                        put("positionMs", currentPositionMs)
                        put("isPlaying", isPlaying)
                        put("at", JSONObject().put(".sv", "timestamp"))
                        put("by", deviceId)
                        put("rev", rev)
                    })
                }

                val patchUrl = "$serverUrl/rooms/$code.json?print=silent"
                httpClient.newCall(
                    Request.Builder()
                        .url(patchUrl)
                        .patch(rootPatch.toString().toRequestBody(JSON_MEDIA_TYPE))
                        .build()
                ).execute()

                lastSyncedQueueHash = queueHash
            } else {
                val statePatch = JSONObject().apply {
                    put("videoId", currentVideoId)
                    put("index", currentIndex)
                    put("positionMs", currentPositionMs)
                    put("isPlaying", isPlaying)
                    put("at", JSONObject().put(".sv", "timestamp"))
                    put("by", deviceId)
                    put("rev", rev)
                }

                val patchUrl = "$serverUrl/rooms/$code/state.json?print=silent"
                httpClient.newCall(
                    Request.Builder()
                        .url(patchUrl)
                        .patch(statePatch.toString().toRequestBody(JSON_MEDIA_TYPE))
                        .build()
                ).execute()
            }
        } catch (_: Exception) {
        }
    }

    /**
     * Keluar dari ruang bersama. Pemutaran lokal tetap berjalan.
     */
    fun leaveRoom(context: Context) {
        val serverUrl = TogetherPreferences(context).serverUrl
        val code = _uiState.value.kodeRuang
        val deviceId = TogetherPreferences(context).deviceId

        sseJob?.cancel()
        heartbeatJob?.cancel()
        calibrationJob?.cancel()
        driftJob?.cancel()
        debounceSyncJob?.cancel()
        debounceRefetchJob?.cancel()

        _uiState.update {
            TogetherUiState(
                status = TogetherStatus.TidakTerhubung,
                kodeRuang = null,
                isPartnerOnline = false,
                partnerName = null,
                pesanGalat = null,
                isHost = false
            )
        }

        if (serverUrl.isNotBlank() && !code.isNullOrBlank()) {
            managerScope.launch {
                try {
                    // Hapus data member sendiri
                    val delMemberUrl = "$serverUrl/rooms/$code/members/$deviceId.json?print=silent"
                    httpClient.newCall(Request.Builder().url(delMemberUrl).delete().build()).execute()

                    // Periksa apakah masih ada member lain yang online
                    val getUrl = "$serverUrl/rooms/$code/members.json"
                    val res = httpClient.newCall(Request.Builder().url(getUrl).get().build()).execute()
                    val body = res.body?.string()?.trim()

                    var anyOtherOnline = false
                    if (res.isSuccessful && body != null && body != "null") {
                        val membersObj = JSONObject(body)
                        val nowServer = serverNow()
                        val keys = membersObj.keys()
                        while (keys.hasNext()) {
                            val k = keys.next()
                            if (k != deviceId) {
                                val m = membersObj.optJSONObject(k) ?: continue
                                val lastSeen = m.optLong("lastSeen", 0L)
                                if (lastSeen > 0L && Math.abs(nowServer - lastSeen) <= 45_000L) {
                                    anyOtherOnline = true
                                    break
                                }
                            }
                        }
                    }

                    if (!anyOtherOnline) {
                        val delRoomUrl = "$serverUrl/rooms/$code.json?print=silent"
                        httpClient.newCall(Request.Builder().url(delRoomUrl).delete().build()).execute()
                        Log.d(TAG, "ListenTogether: ruang dihapus karena tidak ada anggota aktif")
                    }
                } catch (_: Exception) {
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(pesanGalat = null) }
    }
}