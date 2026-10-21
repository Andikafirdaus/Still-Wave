package com.example.still_wave_music_player.youtube

import android.content.Context
import android.util.Log
import org.schabi.newpipe.extractor.services.youtube.PoTokenProvider
import org.schabi.newpipe.extractor.services.youtube.PoTokenResult

/**
 * Implementasi PoTokenProvider untuk NewPipeExtractor.
 */
class NewPipePoTokenProvider(private val context: Context) : PoTokenProvider {

    companion object {
        private const val TAG = "NewPipePoTokenProvider"
    }

    override fun getAndroidClientPoToken(videoId: String): PoTokenResult? {
        Log.d(TAG, "getAndroidClientPoToken dipanggil untuk videoId=$videoId")
        return try {
            val res = PoTokenWebView(context).getPoTokenBlocking(videoId)
            if (!res.isValid) {
                Log.w(TAG, "PoToken yang dihasilkan tidak valid untuk videoId=$videoId")
                return null
            }
            PoTokenResult(
                res.visitorData,
                res.poToken,
                res.streamingPoToken.ifEmpty { res.poToken }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Gagal mendapatkan Android PoToken: ${e.message}", e)
            null
        }
    }

    override fun getWebClientPoToken(videoId: String): PoTokenResult? {
        Log.d(TAG, "getWebClientPoToken dipanggil untuk videoId=$videoId")
        return getAndroidClientPoToken(videoId)
    }

    override fun getWebEmbedClientPoToken(videoId: String): PoTokenResult? {
        Log.d(TAG, "getWebEmbedClientPoToken dipanggil untuk videoId=$videoId")
        return getAndroidClientPoToken(videoId)
    }

    override fun getIosClientPoToken(videoId: String): PoTokenResult? {
        // iOS client tidak membutuhkan PoToken
        return null
    }
}
