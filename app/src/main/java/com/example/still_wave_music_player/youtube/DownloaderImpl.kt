package com.example.still_wave_music_player.youtube

import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Implementasi Downloader NewPipeExtractor menggunakan OkHttpClient.
 */
class DownloaderImpl private constructor(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
) : Downloader() {

    companion object {
        const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

        @Volatile
        private var instance: DownloaderImpl? = null

        @Volatile
        var cachedVisitorData: String? = null

        fun init(client: OkHttpClient? = null): DownloaderImpl {
            return instance ?: synchronized(this) {
                instance ?: DownloaderImpl(
                    client ?: OkHttpClient.Builder()
                        .connectTimeout(15, TimeUnit.SECONDS)
                        .readTimeout(20, TimeUnit.SECONDS)
                        .build()
                ).also { instance = it }
            }
        }

        fun getInstance(): DownloaderImpl {
            return instance ?: init()
        }
    }

    private val cookies = ConcurrentHashMap<String, String>()

    fun setCookie(key: String, value: String) {
        cookies[key] = value
    }

    fun getCookie(key: String): String? = cookies[key]

    fun removeCookie(key: String) {
        cookies.remove(key)
    }

    fun getClient(): OkHttpClient = client

    @Throws(IOException::class, ReCaptchaException::class)
    override fun execute(request: Request): Response {
        val httpMethod = request.httpMethod()
        val url = request.url()
        val headers = request.headers()
        val dataToSend = request.dataToSend()

        val requestBody = dataToSend?.toRequestBody()

        val requestBuilder = okhttp3.Request.Builder()
            .method(httpMethod, requestBody)
            .url(url)
            .addHeader("User-Agent", USER_AGENT)

        cachedVisitorData?.let { visitor ->
            if (url.contains("youtube.com")) {
                requestBuilder.addHeader("X-Goog-Visitor-Id", visitor)
            }
        }

        if (cookies.isNotEmpty()) {
            val cookieHeader = cookies.entries.joinToString("; ") { "${it.key}=${it.value}" }
            requestBuilder.addHeader("Cookie", cookieHeader)
        }

        headers.forEach { (name, values) ->
            requestBuilder.removeHeader(name)
            values.forEach { v -> requestBuilder.addHeader(name, v) }
        }

        client.newCall(requestBuilder.build()).execute().use { response ->
            if (response.code == 429) {
                throw ReCaptchaException("reCaptcha Challenge requested", url)
            }

            val responseBody = response.body?.string() ?: ""
            val latestUrl = response.request.url.toString()

            return Response(
                response.code,
                response.message,
                response.headers.toMultimap(),
                responseBody,
                latestUrl
            )
        }
    }
}
