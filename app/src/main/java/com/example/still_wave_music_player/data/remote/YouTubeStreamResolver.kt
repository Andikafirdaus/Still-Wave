package com.example.still_wave_music_player.data.remote

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.still_wave_music_player.youtube.DownloaderImpl
import com.example.still_wave_music_player.youtube.NewPipePoTokenProvider
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeStreamExtractor
import org.schabi.newpipe.extractor.stream.StreamInfo
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/**
 * Data hasil resolusi alamat audio YouTube.
 *
 * Ditambahkan field [resolvedAtMs] untuk menghitung umur cache
 * dan memudahkan debugging saat URL kedaluwarsa.
 */
data class ResolvedStream(
    val url: String,
    val userAgent: String,
    val expiresAtMs: Long,
    val resolvedAtMs: Long = System.currentTimeMillis(),
    val itag: Int = 0,
    val clientName: String = ""
)

/**
 * Pengambil alamat audio YouTube online menggunakan NewPipeExtractor dan mekanisme PoToken (BotGuard).
 */
object YouTubeStreamResolver {
    private const val TAG = "MusikPlayer"

    private val cache = ConcurrentHashMap<String, ResolvedStream>()
    private var isInitialized = false
    private val initLock = Any()

    private fun getAppContext(): Context {
        return try {
            val activityThread = Class.forName("android.app.ActivityThread")
            val currentApp = activityThread.getMethod("currentApplication").invoke(null) as? Context
            currentApp ?: throw IllegalStateException("Application context not available")
        } catch (e: Exception) {
            throw IllegalStateException("Failed to obtain Application context for NewPipe: ${e.message}", e)
        }
    }

    private fun ensureInitialized() {
        if (isInitialized) return
        synchronized(initLock) {
            if (isInitialized) return
            val context = getAppContext()
            val downloader = DownloaderImpl.init()
            NewPipe.init(downloader)
            YoutubeStreamExtractor.setPoTokenProvider(NewPipePoTokenProvider(context))
            isInitialized = true
            Log.i(TAG, "NewPipeExtractor initialized with DownloaderImpl and NewPipePoTokenProvider")
        }
    }

    /**
     * Memeriksa cache memori yang belum kedaluwarsa.
     *
     * Menambahkan log age (umur cache) dan remaining (sisa waktu valid)
     * untuk memudahkan deteksi cache stale saat debugging.
     */
    fun getCached(videoId: String): ResolvedStream? {
        val cached = cache[videoId] ?: return null
        val now = System.currentTimeMillis()

        if (now < cached.expiresAtMs) {
            val ageMs = now - cached.resolvedAtMs
            val remainingMs = cached.expiresAtMs - now
            Log.d(
                TAG,
                "cache hit videoId=$videoId client=${cached.clientName} itag=${cached.itag} " +
                        "age=${ageMs / 1000}s remaining=${remainingMs / 1000}s"
            )
            return cached
        }

        // Cache sudah kedaluwarsa
        val ageMs = now - cached.resolvedAtMs
        Log.w(
            TAG,
            "cache expired videoId=$videoId client=${cached.clientName} " +
                    "age=${ageMs / 1000}s, removing"
        )
        cache.remove(videoId)
        return null
    }

    /**
     * Menghapus cache stream untuk videoId tertentu (misalnya saat terjadi HTTP 403/410).
     */
    fun invalidate(videoId: String, failedClient: String? = null) {
        cache.remove(videoId)
        Log.d(TAG, "Cache di-invalidate untuk videoId=$videoId (failedClient=$failedClient)")
    }

    /**
     * Mengosongkan seluruh memori cache stream.
     */
    fun clearCache() {
        cache.clear()
    }

    /**
     * Fungsi blocking untuk mencari alamat audio YouTube.
     * Dipanggil dari thread pemuat ExoPlayer / background thread.
     * Menggunakan NewPipeExtractor + BotGuard PoToken dan menerapkan fail-fast.
     */
    @Throws(IOException::class)
    fun resolve(videoId: String): ResolvedStream {
        getCached(videoId)?.let { cached ->
            Log.d(TAG, "resolve cache hit videoId=$videoId client=${cached.clientName} itag=${cached.itag}")
            return cached
        }

        Log.d(TAG, "resolve mulai videoId=$videoId menggunakan NewPipeExtractor + PoToken")
        ensureInitialized()

        val watchUrl = "https://www.youtube.com/watch?v=$videoId"
        val streamInfo = try {
            StreamInfo.getInfo(ServiceList.YouTube, watchUrl)
        } catch (e: Exception) {
            val msg = e.message ?: e.javaClass.simpleName
            Log.e(TAG, "StreamInfo.getInfo error untuk videoId=$videoId: $msg")

            // Fail-fast: Deteksi bot-check, LOGIN_REQUIRED, atau 403
            val isBotCheckOrAuth = msg.contains("LOGIN_REQUIRED", ignoreCase = true) ||
                    msg.contains("bot", ignoreCase = true) ||
                    msg.contains("Sign in", ignoreCase = true) ||
                    msg.contains("403", ignoreCase = true)

            if (isBotCheckOrAuth) {
                Log.e(TAG, "FAIL-FAST: Bot-check / LOGIN_REQUIRED terdeteksi. Menghentikan request tanpa retry.")
                throw IOException("YouTube memblokir pemutaran (bot-check / LOGIN_REQUIRED): $msg", e)
            }
            throw if (e is IOException) e else IOException("Gagal mengekstrak audio stream YouTube: $msg", e)
        }

        val audioStreams = streamInfo.audioStreams
        if (audioStreams.isNullOrEmpty()) {
            Log.e(TAG, "Tidak ada audio stream yang ditemukan untuk videoId=$videoId")
            throw IllegalStateException("No audio stream available for videoId=$videoId")
        }

        val chosenStream = audioStreams
            .filter { it.averageBitrate > 0 }
            .maxByOrNull { it.averageBitrate }
            ?: audioStreams.first()

        val streamUrl = chosenStream.content.takeIf { it.isNotBlank() } ?: chosenStream.getUrl()
        ?: throw IllegalStateException("Audio stream URL is null for videoId=$videoId")

        // Ekstrak waktu expire dari URL
        val expireParam = Uri.parse(streamUrl).getQueryParameter("expire")
        val expiresAtMs = if (!expireParam.isNullOrBlank()) {
            val expireSec = expireParam.toLongOrNull()
            if (expireSec != null) {
                (expireSec * 1000L) - (5 * 60 * 1000L)
            } else {
                System.currentTimeMillis() + (5 * 3600 * 1000L)
            }
        } else {
            System.currentTimeMillis() + (5 * 3600 * 1000L)
        }

        val resolved = ResolvedStream(
            url = streamUrl,
            userAgent = DownloaderImpl.USER_AGENT,
            expiresAtMs = expiresAtMs,
            resolvedAtMs = System.currentTimeMillis(),
            itag = chosenStream.itag,
            clientName = "NEWPIPE_POTOKEN"
        )

        cache[videoId] = resolved
        val validForSec = (expiresAtMs - System.currentTimeMillis()) / 1000
        Log.i(
            TAG,
            "resolve sukses videoId=$videoId itag=${resolved.itag} client=${resolved.clientName} " +
                    "validFor=${validForSec}s"
        )
        return resolved
    }
}