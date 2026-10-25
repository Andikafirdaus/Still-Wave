package com.example.still_wave_music_player.viewmodels

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.still_wave_music_player.data.local.AppDatabase
import com.example.still_wave_music_player.data.model.LyricsUiState
import com.example.still_wave_music_player.data.model.Song
import com.example.still_wave_music_player.data.remote.YouTubeMusicApi
import com.example.still_wave_music_player.data.repository.FavoritesRepository
import com.example.still_wave_music_player.data.repository.HistoryRepository
import com.example.still_wave_music_player.data.repository.LyricsRepository
import com.example.still_wave_music_player.data.repository.SettingsRepository
import com.example.still_wave_music_player.playback.MusicService
import com.example.still_wave_music_player.together.ListenTogetherManager
import com.example.still_wave_music_player.together.createOnlineMediaItem
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Collections

/**
 * Mode pengulangan pemutar lagu.
 */
enum class RepeatMode {
    OFF, ALL, ONE
}

/**
 * Penanda sumber antrean pemutaran lagu.
 */
enum class QueueSource {
    SINGLE_ONLINE, COLLECTION, LIST, RADIO
}

/**
 * State pemutaran lagu UI.
 */
data class PlayerUiState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val progress: Float = 0f, // 0f .. 1f
    val currentPositionMs: Long = 0L,
    val queue: List<Song> = emptyList(),
    val isShuffle: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val isBuffering: Boolean = false,
    val isRadioQueue: Boolean = false
)

/**
 * ViewModel pengelola pemutaran lagu Media3 sesungguhnya yang terhubung ke MusicService
 * melalui MediaController. Mendukung pemutaran lagu lokal dan online.
 */
class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val lyricsRepository = LyricsRepository.getInstance(application)
    private val _lyricsState = MutableStateFlow<LyricsUiState>(LyricsUiState.Loading)
    val lyricsState: StateFlow<LyricsUiState> = _lyricsState.asStateFlow()
    private var lyricsJob: Job? = null
    private var lastLyricsSongId: String? = null

    private val settingsRepository = SettingsRepository.getInstance(application)
    val isAutoRadioEnabled: StateFlow<Boolean> = settingsRepository.isAutoRadioEnabled
    val crossfadeSeconds: StateFlow<Int> = settingsRepository.crossfadeSeconds

    fun setAutoRadioEnabled(enabled: Boolean) {
        settingsRepository.setAutoRadioEnabled(enabled)
    }

    fun setCrossfadeSeconds(seconds: Int) {
        settingsRepository.setCrossfadeSeconds(seconds)
    }

    // Room repositories
    private val db = AppDatabase.getDatabase(application)
    val historyRepository = HistoryRepository(db.playHistoryDao())
    val favoritesRepository = FavoritesRepository(db.favoriteSongDao())

    fun getFavorites(): Flow<List<Song>> = favoritesRepository.getAll()
    fun isFavorite(song: Song): Flow<Boolean> = favoritesRepository.isFavorite(song)

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            favoritesRepository.toggleFavorite(song)
        }
    }

    // Pencatatan "putaran": lagu dianggap diputar jika minimal min(30detik, 50% durasi) terdengar, satu kali per transisi lagu
    private var historyTrackSongId: String? = null
    private var historyTrackStartMs: Long = 0L
    private var historyRecordedForCurrent: Boolean = false

    private var queueSource: QueueSource = QueueSource.COLLECTION
    private val playedVideoIds: MutableSet<String> = Collections.synchronizedSet(mutableSetOf())
    private var isFetchingRadio = false
    private var queueGeneration: Long = 0L
    private var radioJob: Job? = null

    private var mediaController: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private val pendingActions = mutableListOf<(MediaController) -> Unit>()
    private var positionTrackerJob: Job? = null
    private var lastFailedMediaIdToast: String? = null

    private fun loadLyricsForSong(song: Song?) {
        if (song == null) {
            _lyricsState.value = LyricsUiState.NotFound
            lastLyricsSongId = null
            return
        }
        val songKey = song.videoId?.takeIf { it.isNotBlank() } ?: song.id
        if (songKey == lastLyricsSongId) return
        lastLyricsSongId = songKey

        lyricsJob?.cancel()
        _lyricsState.value = LyricsUiState.Loading
        lyricsJob = viewModelScope.launch {
            val state = lyricsRepository.getLyrics(song)
            _lyricsState.value = state
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _uiState.update { it.copy(isPlaying = isPlaying) }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val mediaId = mediaItem?.mediaId
            val matchedSong = mediaId?.let { id ->
                _uiState.value.queue.find { it.id == id || it.videoId == id }
            } ?: mediaItem?.let { item ->
                val metadata = item.mediaMetadata
                val isOnline = item.requestMetadata.mediaUri?.scheme == "ytm" ||
                    item.localConfiguration?.uri?.scheme == "ytm"
                Song(
                    id = if (isOnline) "yt_${item.mediaId}" else item.mediaId,
                    title = metadata.title?.toString() ?: "Tanpa Judul",
                    artist = metadata.artist?.toString() ?: "Artis tidak diketahui",
                    album = metadata.albumTitle?.toString() ?: "Album tidak diketahui",
                    durationMs = 0L,
                    thumbnailUrl = metadata.artworkUri?.toString(),
                    contentUri = if (isOnline) null else item.requestMetadata.mediaUri?.toString(),
                    videoId = if (isOnline) item.mediaId else null
                )
            }

            matchedSong?.videoId?.let { playedVideoIds.add(it) }
            loadLyricsForSong(matchedSong)

            // Reset pelacak riwayat untuk lagu baru saat transisi
            historyTrackSongId = null
            historyRecordedForCurrent = false

            _uiState.update {
                it.copy(
                    currentSong = matchedSong,
                    progress = 0f,
                    currentPositionMs = 0L,
                    isBuffering = false
                )
            }

            // Cek kebutuhan penambahan radio otomatis saat sisa antrean <= 2
            if (queueSource == QueueSource.SINGLE_ONLINE && settingsRepository.isAutoRadioEnabled.value && _uiState.value.repeatMode != RepeatMode.ONE && ListenTogetherManager.canExtendRadio()) {
                mediaController?.let { controller ->
                    val remaining = controller.mediaItemCount - controller.currentMediaItemIndex - 1
                    if (remaining <= 2 && !isFetchingRadio) {
                        val lastVideoId = _uiState.value.queue.lastOrNull { !it.videoId.isNullOrBlank() }?.videoId ?: matchedSong?.videoId
                        if (lastVideoId != null) {
                            fetchAndAppendRadio(lastVideoId, queueGeneration)
                        }
                    }
                }
            }
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _uiState.update { it.copy(isShuffle = shuffleModeEnabled) }
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            val mode = when (repeatMode) {
                Player.REPEAT_MODE_ALL -> RepeatMode.ALL
                Player.REPEAT_MODE_ONE -> RepeatMode.ONE
                else -> RepeatMode.OFF
            }
            _uiState.update { it.copy(repeatMode = mode) }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val isBuffering = playbackState == Player.STATE_BUFFERING
            _uiState.update { it.copy(isBuffering = isBuffering) }

            if (playbackState == Player.STATE_ENDED) {
                val state = _uiState.value
                if (state.repeatMode == RepeatMode.OFF) {
                    _uiState.update { it.copy(isPlaying = false, progress = 1f) }
                }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            val causeClassName = error.cause?.javaClass?.name ?: "null"
            val errorCodeName = error.errorCodeName
            Log.e("MusikPlayer", "Kesalahan pemutar Media3: cause=$causeClassName errorCodeName=$errorCodeName")
            val controller = mediaController
            val failedMediaId = controller?.currentMediaItem?.mediaId
            if (failedMediaId != null && failedMediaId != lastFailedMediaIdToast) {
                lastFailedMediaIdToast = failedMediaId
                viewModelScope.launch(Dispatchers.Main) {
                    Toast.makeText(
                        getApplication(),
                        "Lagu tidak dapat diputar",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            if (controller != null && controller.hasNextMediaItem()) {
                controller.seekToNextMediaItem()
                controller.play()
            }
        }
    }

    init {
        val sessionToken = SessionToken(
            application,
            ComponentName(application, MusicService::class.java)
        )
        val future = MediaController.Builder(application, sessionToken).buildAsync()
        controllerFuture = future

        future.addListener(
            {
                try {
                    val controller = future.get()
                    mediaController = controller
                    controller.addListener(playerListener)
                    syncStateFromPlayer(controller)
                    startPositionTracker()

                    val actions = pendingActions.toList()
                    pendingActions.clear()
                    actions.forEach { action ->
                        action(controller)
                    }
                } catch (e: Exception) {
                    Log.e("MusikPlayer", "Gagal menginisialisasi MediaController: ${e.message}", e)
                }
            },
            ContextCompat.getMainExecutor(application)
        )
    }

    private fun withController(action: (MediaController) -> Unit) {
        val controller = mediaController
        if (controller != null) {
            action(controller)
        } else {
            pendingActions.add(action)
        }
    }

    private fun syncStateFromPlayer(controller: MediaController) {
        val currentItem = controller.currentMediaItem
        val currentMediaId = currentItem?.mediaId
        val matchedSong = currentMediaId?.let { id ->
            _uiState.value.queue.find { it.id == id || it.videoId == id }
        } ?: currentItem?.let { item ->
            val metadata = item.mediaMetadata
            val isOnline = item.requestMetadata.mediaUri?.scheme == "ytm" ||
                item.localConfiguration?.uri?.scheme == "ytm"
            Song(
                id = if (isOnline) "yt_${item.mediaId}" else item.mediaId,
                title = metadata.title?.toString() ?: "Tanpa Judul",
                artist = metadata.artist?.toString() ?: "Artis tidak diketahui",
                album = metadata.albumTitle?.toString() ?: "Album tidak diketahui",
                durationMs = 0L,
                thumbnailUrl = metadata.artworkUri?.toString(),
                contentUri = if (isOnline) null else item.requestMetadata.mediaUri?.toString(),
                videoId = if (isOnline) item.mediaId else null
            )
        }

        val repeatMode = when (controller.repeatMode) {
            Player.REPEAT_MODE_ALL -> RepeatMode.ALL
            Player.REPEAT_MODE_ONE -> RepeatMode.ONE
            else -> RepeatMode.OFF
        }

        loadLyricsForSong(matchedSong)

        _uiState.update {
            it.copy(
                currentSong = matchedSong ?: it.currentSong,
                isPlaying = controller.isPlaying,
                isShuffle = controller.shuffleModeEnabled,
                repeatMode = repeatMode,
                isBuffering = controller.playbackState == Player.STATE_BUFFERING
            )
        }
    }

    private fun startPositionTracker() {
        positionTrackerJob?.cancel()
        positionTrackerJob = viewModelScope.launch {
            while (isActive) {
                delay(250L)
                val controller = mediaController
                if (controller != null && controller.isPlaying) {
                    val currentPosition = controller.currentPosition.coerceAtLeast(0L)
                    val exoDuration = controller.duration
                    val currentSong = _uiState.value.currentSong
                    val duration = if (exoDuration != C.TIME_UNSET && exoDuration > 0L) {
                        exoDuration
                    } else {
                        currentSong?.durationMs ?: C.TIME_UNSET
                    }

                    val updatedSong = if (exoDuration != C.TIME_UNSET && exoDuration > 0L && currentSong != null && currentSong.durationMs != exoDuration) {
                        currentSong.copy(durationMs = exoDuration)
                    } else {
                        currentSong
                    }

                    val progress = if (duration > 0L) {
                        (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                    } else {
                        0f
                    }

                    _uiState.update {
                        it.copy(
                            currentSong = updatedSong,
                            progress = progress,
                            currentPositionMs = currentPosition
                        )
                    }

                    // Pelacak riwayat: catat saat minimal min(30000ms, 50% durasi) telah terdengar
                    if (!historyRecordedForCurrent && currentSong != null) {
                        val songKey = currentSong.videoId?.takeIf { it.isNotBlank() } ?: currentSong.id
                        if (historyTrackSongId != songKey) {
                            historyTrackSongId = songKey
                            historyTrackStartMs = currentPosition
                        } else {
                            val listened = currentPosition - historyTrackStartMs
                            val threshold = if (duration > 0L) {
                                minOf(30_000L, duration / 2)
                            } else {
                                30_000L
                            }
                            if (listened >= threshold) {
                                historyRecordedForCurrent = true
                                val songToRecord = updatedSong ?: currentSong
                                Log.d("MusikPlayer", "riwayat tersimpan songKey=$songKey")
                                viewModelScope.launch(Dispatchers.IO) {
                                    historyRepository.addHistory(songToRecord)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Memulai pemutaran lagu sesungguhnya dengan Media3 ExoPlayer.
     * Mendukung lagu lokal (dengan contentUri) dan lagu online YouTube Music (dengan videoId).
     */
    fun putar(song: Song, daftar: List<Song> = emptyList(), source: QueueSource? = null) {
        val currentGen = ++queueGeneration
        radioJob?.cancel()
        radioJob = null
        isFetchingRadio = false

        val sourceList = if (daftar.isNotEmpty()) daftar else listOf(song)
        val playableSongs = sourceList.filter { !it.contentUri.isNullOrEmpty() || !it.videoId.isNullOrEmpty() }
        if (playableSongs.isEmpty()) {
            Log.e("MusikPlayer", "Tidak ada lagu yang valid untuk diputar.")
            return
        }

        val isSingleOnline = (daftar.isEmpty() || daftar.size == 1) && !song.videoId.isNullOrBlank()
        queueSource = source ?: if (isSingleOnline) QueueSource.SINGLE_ONLINE else QueueSource.COLLECTION
        val isRadio = queueSource == QueueSource.SINGLE_ONLINE || queueSource == QueueSource.RADIO
        song.videoId?.let { playedVideoIds.add(it) }

        // Reset pelacak riwayat untuk lagu baru
        historyTrackSongId = null
        historyRecordedForCurrent = false

        val initialIndex = playableSongs.indexOfFirst {
            (!song.videoId.isNullOrBlank() && it.videoId == song.videoId) || it.id == song.id
        }.let { if (it == -1) 0 else it }

        Log.d("MusikPlayer", "putar() videoId=${song.videoId ?: song.id} idx=$initialIndex gen=$currentGen sumber=${queueSource.name}")

        val mediaItems = playableSongs.map { createMediaItem(it) }

        val targetSong = playableSongs[initialIndex]
        loadLyricsForSong(targetSong)

        _uiState.update {
            it.copy(
                queue = playableSongs,
                currentSong = targetSong,
                progress = 0f,
                currentPositionMs = 0L,
                isBuffering = true,
                isRadioQueue = isRadio
            )
        }

        withController { controller ->
            val currentMediaId = controller.currentMediaItem?.mediaId
            val currentSong = _uiState.value.currentSong
            val isSameSong = when {
                !song.videoId.isNullOrBlank() -> {
                    currentMediaId == song.videoId && currentSong?.videoId == song.videoId
                }
                else -> {
                    currentMediaId == song.id && currentSong?.id == song.id
                }
            }
            val sameCount = controller.mediaItemCount == mediaItems.size
            val sameIndex = controller.currentMediaItemIndex == initialIndex

            if (isSameSong && sameCount && sameIndex) {
                if (!controller.isPlaying) {
                    controller.play()
                }
            } else {
                controller.setMediaItems(mediaItems, initialIndex, 0L)
                controller.prepare()
                controller.play()
            }
        }

        if (isSingleOnline && settingsRepository.isAutoRadioEnabled.value && ListenTogetherManager.canExtendRadio()) {
            song.videoId?.let { fetchAndAppendRadio(it, currentGen) }
        }
    }

    private fun createMediaItem(s: Song): MediaItem {
        return if (!s.videoId.isNullOrEmpty() && s.contentUri.isNullOrEmpty()) {
            createOnlineMediaItem(s)
        } else {
            val localUri = Uri.parse(s.contentUri ?: "")
            MediaItem.Builder()
                .setMediaId(s.id)
                .setUri(localUri)
                .setRequestMetadata(
                    MediaItem.RequestMetadata.Builder()
                        .setMediaUri(localUri)
                        .build()
                )
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(s.title)
                        .setArtist(s.artist)
                        .setAlbumTitle(s.album)
                        .build()
                )
                .build()
        }
    }

    private fun fetchAndAppendRadio(seedVideoId: String, generation: Long) {
        if (isFetchingRadio) return
        isFetchingRadio = true
        val seedArtist = _uiState.value.currentSong?.artist
        radioJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                if (generation != queueGeneration) return@launch
                val radioResult = YouTubeMusicApi.radio(seedVideoId, seedArtist)
                if (generation != queueGeneration) return@launch
                val radioSongs = radioResult.getOrNull().orEmpty()
                val currentQueueVideoIds = _uiState.value.queue.mapNotNull { it.videoId }.toSet()

                val filtered = radioSongs.filter { song ->
                    val vId = song.videoId
                    !vId.isNullOrBlank() && !playedVideoIds.contains(vId) && !currentQueueVideoIds.contains(vId)
                }

                val precedingArtists = _uiState.value.queue.takeLast(2).map { it.artist }
                val newSongs = YouTubeMusicApi.applyRadioArtistVariation(filtered, seedArtist, precedingArtists)

                if (newSongs.isNotEmpty()) {
                    if (generation != queueGeneration) return@launch
                    val newMediaItems = newSongs.map { createMediaItem(it) }
                    withContext(Dispatchers.Main) {
                        if (generation != queueGeneration) return@withContext
                        withController { controller ->
                            if (generation != queueGeneration) return@withController
                            controller.addMediaItems(newMediaItems)
                            _uiState.update {
                                it.copy(
                                    queue = it.queue + newSongs,
                                    isRadioQueue = true
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("MusikPlayer", "Gagal memuat auto radio untuk videoId=$seedVideoId: ${e.message}")
            } finally {
                isFetchingRadio = false
            }
        }
    }

    /**
     * Melompat ke indeks lagu tertentu di antrean.
     */
    fun lompatKeAntrean(index: Int) {
        val currentQueue = _uiState.value.queue
        if (index !in currentQueue.indices) return
        val targetSong = currentQueue[index]
        _uiState.update {
            it.copy(
                currentSong = targetSong,
                progress = 0f,
                currentPositionMs = 0L,
                isBuffering = true
            )
        }
        withController { controller ->
            if (index < controller.mediaItemCount) {
                controller.seekToDefaultPosition(index)
                controller.play()
            }
        }
        loadLyricsForSong(targetSong)
    }

    /**
     * Menghapus lagu pada indeks tertentu dari antrean.
     */
    fun hapusDariAntrean(index: Int) {
        val currentQueue = _uiState.value.queue
        if (index !in currentQueue.indices) return
        val updatedQueue = currentQueue.toMutableList()
        updatedQueue.removeAt(index)
        _uiState.update { it.copy(queue = updatedQueue) }
        withController { controller ->
            if (index < controller.mediaItemCount) {
                controller.removeMediaItem(index)
            }
        }
    }

    /**
     * Membersihkan antrean dan menyisakan lagu yang sedang diputar.
     */
    fun bersihkanAntrean() {
        val currentSong = _uiState.value.currentSong ?: return
        val currentQueue = _uiState.value.queue
        val currentIndex = currentQueue.indexOfFirst {
            (it.id == currentSong.id && it.id.isNotBlank()) ||
            (!it.videoId.isNullOrBlank() && it.videoId == currentSong.videoId)
        }.takeIf { it != -1 } ?: 0

        _uiState.update { it.copy(queue = listOf(currentSong)) }

        withController { controller ->
            val count = controller.mediaItemCount
            if (count > 1) {
                val currentMediaIndex = controller.currentMediaItemIndex
                if (currentMediaIndex + 1 < count) {
                    controller.removeMediaItems(currentMediaIndex + 1, count)
                }
                if (currentMediaIndex > 0) {
                    controller.removeMediaItems(0, currentMediaIndex)
                }
            }
        }
    }

    /**
     * Mengalihkan status putar/jeda (Play/Pause).
     */
    fun togglePlayPause() {
        val currentState = _uiState.value
        if (currentState.currentSong == null) {
            val firstSong = currentState.queue.firstOrNull() ?: return
            putar(firstSong, currentState.queue)
            return
        }

        withController { controller ->
            if (controller.isPlaying) {
                controller.pause()
            } else {
                if (controller.playbackState == Player.STATE_IDLE) {
                    controller.prepare()
                }
                controller.play()
            }
        }
    }

    /**
     * Memutar lagu berikutnya pada antrean.
     */
    fun berikutnya() {
        withController { controller ->
            controller.seekToNextMediaItem()
        }
    }

    /**
     * Memutar lagu sebelumnya, atau mengulang dari awal jika progres sudah berjalan > 3 detik.
     */
    fun sebelumnya() {
        withController { controller ->
            controller.seekToPrevious()
        }
    }

    /**
     * Menggeser progres pemutaran ke nilai fraksi tertentu (0f..1f).
     */
    fun geserProgres(progres: Float) {
        withController { controller ->
            val exoDuration = controller.duration
            val currentSong = _uiState.value.currentSong
            val duration = if (exoDuration != C.TIME_UNSET && exoDuration > 0L) {
                exoDuration
            } else {
                currentSong?.durationMs ?: 0L
            }

            if (duration > 0L) {
                val targetPosition = (progres.coerceIn(0f, 1f) * duration).toLong()
                controller.seekTo(targetPosition)
                _uiState.update {
                    it.copy(
                        progress = progres.coerceIn(0f, 1f),
                        currentPositionMs = targetPosition
                    )
                }
            } else {
                _uiState.update { it.copy(progress = progres.coerceIn(0f, 1f)) }
            }
        }
    }

    /**
     * Melompat langsung ke posisi waktu tertentu (dalam milidetik).
     */
    fun seekToPosition(positionMs: Long) {
        withController { controller ->
            val targetMs = positionMs.coerceAtLeast(0L)
            controller.seekTo(targetMs)
            val exoDuration = controller.duration
            val currentSong = _uiState.value.currentSong
            val duration = if (exoDuration != C.TIME_UNSET && exoDuration > 0L) {
                exoDuration
            } else {
                currentSong?.durationMs ?: 0L
            }
            val progress = if (duration > 0L) {
                (targetMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }
            _uiState.update {
                it.copy(
                    progress = progress,
                    currentPositionMs = targetMs
                )
            }
        }
    }

    /**
     * Mengalihkan mode acak (Shuffle).
     */
    fun toggleAcak() {
        withController { controller ->
            val newShuffle = !controller.shuffleModeEnabled
            controller.shuffleModeEnabled = newShuffle
            _uiState.update { it.copy(isShuffle = newShuffle) }
        }
    }

    /**
     * Mengganti mode pengulangan lagu secara bergantian (mati -> semua -> satu lagu -> mati).
     */
    fun gantiModeUlang() {
        withController { controller ->
            val nextPlayerMode = when (controller.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_OFF
                else -> Player.REPEAT_MODE_OFF
            }
            controller.repeatMode = nextPlayerMode
            val nextUiMode = when (nextPlayerMode) {
                Player.REPEAT_MODE_ALL -> RepeatMode.ALL
                Player.REPEAT_MODE_ONE -> RepeatMode.ONE
                else -> RepeatMode.OFF
            }
            _uiState.update { it.copy(repeatMode = nextUiMode) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        positionTrackerJob?.cancel()
        positionTrackerJob = null
        mediaController?.removeListener(playerListener)
        mediaController = null
        controllerFuture?.let { future ->
            MediaController.releaseFuture(future)
            controllerFuture = null
        }
    }
}
