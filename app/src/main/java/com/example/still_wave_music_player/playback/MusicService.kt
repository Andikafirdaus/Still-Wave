package com.example.still_wave_music_player.playback

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.still_wave_music_player.MainActivity
import com.example.still_wave_music_player.data.remote.YouTubeStreamResolver
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * Service pemutaran audio latar belakang dan notifikasi berbasis Media3 MediaSessionService.
 * Mendukung pemutaran lagu lokal dan lagu online YouTube Music via ResolvingDataSource.
 */
class MusicService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var lastRetriedVideoId: String? = null
    private var lastResolvedClient: String? = null
    private var retryCountForCurrentVideo = 0

    override fun onCreate() {
        super.onCreate()
        YouTubeStreamResolver.clearCache()

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        // 1. Siapkan HTTP DataSource Factory dasar
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(15_000)

        // 2. Siapkan DefaultDataSource.Factory (menangani lokal content:// dan online http(s)://)
        val defaultDataSourceFactory = DefaultDataSource.Factory(this, httpDataSourceFactory)

        // 3. Bungkus dengan ResolvingDataSource.Factory untuk skema khusus "ytm://"
        val resolvingDataSourceFactory = ResolvingDataSource.Factory(
            defaultDataSourceFactory,
            object : ResolvingDataSource.Resolver {
                override fun resolveDataSpec(dataSpec: DataSpec): DataSpec {
                    val uri = dataSpec.uri
                    if (uri.scheme == "ytm") {
                        val videoId = uri.host?.takeIf { it.isNotBlank() }
                            ?: uri.path?.trimStart('/')?.takeIf { it.isNotBlank() }
                            ?: ""

                        Log.d("MusikPlayer", "resolver dipanggil videoId=$videoId")

                        val resolved = try {
                            YouTubeStreamResolver.resolve(videoId)
                        } catch (e: Exception) {
                            val shortMsg = e.message ?: e.javaClass.simpleName
                            Log.e("MusikPlayer", "resolver gagal: $shortMsg")
                            if (e is IOException) throw e else throw IOException(shortMsg, e)
                        }

                        lastResolvedClient = resolved.clientName
                        Log.d("MusikPlayer", "resolver selesai")

                        val cleanUserAgent = resolved.userAgent.replace(" gzip", "").trim()
                        val mediaHeaders = mapOf(
                            "User-Agent" to cleanUserAgent
                        )

                        httpDataSourceFactory.setUserAgent(cleanUserAgent)
                        httpDataSourceFactory.setDefaultRequestProperties(mediaHeaders)

                        return dataSpec.withUri(Uri.parse(resolved.url))
                            .withAdditionalHeaders(mediaHeaders)
                    }
                    // Lagu lokal atau skema lain dikembalikan apa adanya tanpa perubahan
                    return dataSpec
                }
            }
        )

        val mediaSourceFactory = DefaultMediaSourceFactory(resolvingDataSourceFactory)

        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()

        // 4. Pasang listener penanganan error dan retry kedaluwarsa pada ExoPlayer
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val mediaId = mediaItem?.mediaId
                if (mediaId != null && mediaId != lastRetriedVideoId) {
                    lastRetriedVideoId = null
                    retryCountForCurrentVideo = 0
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                val currentItem = player.currentMediaItem
                val uri = currentItem?.requestMetadata?.mediaUri ?: currentItem?.localConfiguration?.uri
                val isYtm = uri?.scheme == "ytm"

                val videoId = if (isYtm) {
                    uri?.host?.takeIf { it.isNotBlank() } ?: uri?.path?.trimStart('/') ?: ""
                } else {
                    ""
                }

                var cause: Throwable? = error.cause
                var isHttpExpireOrForbidden = false
                var httpCode = 0

                while (cause != null) {
                    if (cause is HttpDataSource.InvalidResponseCodeException) {
                        httpCode = cause.responseCode
                        if (httpCode == 403 || httpCode == 410) {
                            isHttpExpireOrForbidden = true
                            break
                        }
                    }
                    cause = cause.cause
                }

                val causeClassName = error.cause?.javaClass?.name ?: "null"
                val errorCodeName = error.errorCodeName
                val httpInfo = if (httpCode != 0) " kodeHTTP=$httpCode" else ""
                Log.e(
                    "MusikPlayer",
                    "onPlayerError: cause=$causeClassName errorCodeName=$errorCodeName$httpInfo"
                )

                // Jika error 403/410: coba ulang dengan jeda backoff
                if (isYtm && isHttpExpireOrForbidden && videoId.isNotBlank() && retryCountForCurrentVideo < 3) {
                    retryCountForCurrentVideo++
                    Log.w("MusikPlayer", "Stream YouTube kedaluwarsa/ditolak (HTTP $httpCode) untuk videoId=$videoId. Backoff 2 detik sebelum retry ke-$retryCountForCurrentVideo...")

                    YouTubeStreamResolver.invalidate(videoId, failedClient = lastResolvedClient)
                    val lastPosition = player.currentPosition.coerceAtLeast(0L)

                    // BACKOFF: Jeda 2 detik menggunakan Coroutine sebelum retry
                    CoroutineScope(Dispatchers.Main).launch {
                        delay(2000)
                        Log.d("MusikPlayer", "Melanjutkan retry untuk videoId=$videoId")
                        player.prepare()
                        player.seekTo(lastPosition)
                        player.play()
                    }
                    return
                }

                // Jika gagal lagi atau error lain: lompat ke lagu berikutnya atau hentikan
                Log.e("MusikPlayer", "Pemutaran gagal untuk videoId=$videoId: cause=$causeClassName errorCodeName=$errorCodeName$httpInfo")
                lastRetriedVideoId = null
                if (player.hasNextMediaItem()) {
                    player.seekToNextMediaItem()
                    player.play()
                } else {
                    player.stop()
                }
            }
        })

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // 5. Callback MediaSession untuk memastikan MediaItem dari MediaController tidak kehilangan URI
        val sessionCallback = object : MediaSession.Callback {
            override fun onAddMediaItems(
                mediaSession: MediaSession,
                controller: MediaSession.ControllerInfo,
                mediaItems: List<MediaItem>
            ): ListenableFuture<List<MediaItem>> {
                val updatedMediaItems = mediaItems.map { item ->
                    if (item.localConfiguration == null) {
                        val uri = item.requestMetadata.mediaUri
                            ?: if (item.mediaId.isNotBlank()) {
                                if (item.mediaId.startsWith("content://") || item.mediaId.startsWith("file://")) {
                                    Uri.parse(item.mediaId)
                                } else {
                                    Uri.parse("ytm://${item.mediaId}")
                                }
                            } else null

                        if (uri != null) {
                            item.buildUpon()
                                .setUri(uri)
                                .setRequestMetadata(
                                    item.requestMetadata.buildUpon()
                                        .setMediaUri(uri)
                                        .build()
                                )
                                .build()
                        } else {
                            item
                        }
                    } else {
                        item
                    }
                }
                return Futures.immediateFuture(updatedMediaItems)
            }
        }

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(pendingIntent)
            .setCallback(sessionCallback)
            .build()

        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this).build()
        )
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        val player = mediaSession?.player
        if (player == null || !player.isPlaying || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}