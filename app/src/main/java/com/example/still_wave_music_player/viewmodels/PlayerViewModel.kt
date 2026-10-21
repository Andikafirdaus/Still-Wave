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
import com.example.still_wave_music_player.data.model.LyricsUiState
import com.example.still_wave_music_player.data.model.Song
import com.example.still_wave_music_player.data.repository.LyricsRepository
import com.example.still_wave_music_player.playback.MusicService
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Mode pengulangan pemutar lagu.
 */
enum class RepeatMode {
    OFF, ALL, ONE
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
    val isBuffering: Boolean = false
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

            loadLyricsForSong(matchedSong)

            _uiState.update {
                it.copy(
                    currentSong = matchedSong,
                    progress = 0f,
                    currentPositionMs = 0L,
                    isBuffering = false
                )
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
                }
            }
        }
    }

    /**
     * Memulai pemutaran lagu sesungguhnya dengan Media3 ExoPlayer.
     * Mendukung lagu lokal (dengan contentUri) dan lagu online YouTube Music (dengan videoId).
     */
    fun putar(song: Song, daftar: List<Song> = emptyList()) {
        val sourceList = if (daftar.isNotEmpty()) daftar else _uiState.value.queue.ifEmpty { listOf(song) }
        val playableSongs = sourceList.filter { !it.contentUri.isNullOrEmpty() || !it.videoId.isNullOrEmpty() }
        if (playableSongs.isEmpty()) {
            Log.e("MusikPlayer", "Tidak ada lagu yang valid untuk diputar.")
            return
        }

        val onlineVideoId = song.videoId?.takeIf { it.isNotBlank() }
        if (onlineVideoId != null) {
            Log.d("MusikPlayer", "putar() dipanggil lagu online videoId=$onlineVideoId antrean=${playableSongs.size}")
        }

        val initialIndex = playableSongs.indexOfFirst {
            it.id == song.id || (!it.videoId.isNullOrEmpty() && it.videoId == song.videoId)
        }.let { if (it == -1) 0 else it }

        val mediaItems = playableSongs.map { s ->
            if (!s.videoId.isNullOrEmpty() && s.contentUri.isNullOrEmpty()) {
                // Lagu online YouTube Music
                val metaBuilder = MediaMetadata.Builder()
                    .setTitle(s.title)
                    .setArtist(s.artist)

                if (!s.album.isNullOrBlank()) {
                    metaBuilder.setAlbumTitle(s.album)
                }
                if (!s.thumbnailUrl.isNullOrBlank()) {
                    metaBuilder.setArtworkUri(Uri.parse(s.thumbnailUrl))
                }

                val onlineUri = Uri.parse("ytm://${s.videoId}")
                MediaItem.Builder()
                    .setMediaId(s.videoId)
                    .setUri(onlineUri)
                    .setRequestMetadata(
                        MediaItem.RequestMetadata.Builder()
                            .setMediaUri(onlineUri)
                            .build()
                    )
                    .setMediaMetadata(metaBuilder.build())
                    .build()
            } else {
                // Lagu lokal (kode lama tidak diubah)
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

        val targetSong = playableSongs[initialIndex]
        loadLyricsForSong(targetSong)

        _uiState.update {
            it.copy(
                queue = playableSongs,
                currentSong = targetSong,
                progress = 0f,
                currentPositionMs = 0L,
                isBuffering = true
            )
        }

        withController { controller ->
            val currentMediaId = controller.currentMediaItem?.mediaId
            val sameSong = currentMediaId != null && (currentMediaId == song.id || currentMediaId == song.videoId)
            val sameCount = controller.mediaItemCount == mediaItems.size

            if (sameSong && sameCount) {
                if (!controller.isPlaying) {
                    controller.play()
                }
            } else {
                controller.setMediaItems(mediaItems, initialIndex, 0L)
                controller.prepare()
                controller.play()
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
