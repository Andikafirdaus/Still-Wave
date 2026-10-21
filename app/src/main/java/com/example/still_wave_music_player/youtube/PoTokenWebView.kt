package com.example.still_wave_music_player.youtube

import android.annotation.SuppressLint
import android.content.Context
import android.util.Base64
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Hasil perolehan Proof-of-Origin Token (PoToken).
 */
data class PoTokenResult(
    val poToken: String,
    val visitorData: String,
    val isValid: Boolean,
    val streamingPoToken: String = ""
)

/**
 * Headless WebView untuk mengeksekusi JavaScript BotGuard YouTube dan menghasilkan PoToken.
 */
class PoTokenWebView(private val context: Context) {

    companion object {
        const val TAG = "PoTokenWebView"
        private const val GOOGLE_API_KEY = "AIzaSyDyT5W0Jh49F30Pqqtyfdf7pDLFKLJoAnw"
        private const val REQUEST_KEY = "O43z0dpjhgX20SCx4KAo"
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

        private val httpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Mengambil PoToken secara blocking dengan batas timeout 8 detik.
     */
    fun getPoTokenBlocking(videoId: String, visitorData: String = ""): PoTokenResult {
        return runBlocking {
            withTimeout(8000L) {
                getPoToken(videoId, visitorData)
            }
        }
    }

    /**
     * Mengambil PoToken secara asynchronous via Coroutine.
     * Menggunakan headless WebView yang di-cleanup di blok finally untuk mencegah memory leak.
     */
    @SuppressLint("SetJavaScriptEnabled")
    suspend fun getPoToken(videoId: String, visitorData: String = ""): PoTokenResult {
        val finalVisitorData = if (visitorData.isNotBlank()) {
            visitorData
        } else {
            DownloaderImpl.cachedVisitorData ?: withContext(Dispatchers.IO) { fetchVisitorData() }
        }

        val parsedChallengeJson = withContext(Dispatchers.IO) {
            fetchChallengeData()
        }

        var webView: WebView? = null
        try {
            val result = withContext(Dispatchers.Main.immediate) {
                val pageLoadedDeferred = CompletableDeferred<Unit>()
                val botguardDeferred = CompletableDeferred<String>()
                val streamingPotDeferred = CompletableDeferred<String>()
                val playerPotDeferred = CompletableDeferred<String>()

                val wv = WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = true
                    settings.userAgentString = USER_AGENT

                    webChromeClient = object : WebChromeClient() {
                        override fun onConsoleMessage(m: ConsoleMessage): Boolean {
                            if (m.message().contains("Uncaught", ignoreCase = true)) {
                                Log.e(TAG, "JS Error: ${m.message()} (${m.sourceId()}:${m.lineNumber()})")
                                val ex = IOException("BotGuard JS error: ${m.message()}")
                                botguardDeferred.completeExceptionally(ex)
                                streamingPotDeferred.completeExceptionally(ex)
                                playerPotDeferred.completeExceptionally(ex)
                            }
                            return super.onConsoleMessage(m)
                        }
                    }

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            if (url?.contains("po_token.html") == true) {
                                pageLoadedDeferred.complete(Unit)
                            }
                        }
                    }

                    addJavascriptInterface(object {
                        @JavascriptInterface
                        fun onRunBotguardResult(botguardResponse: String) {
                            botguardDeferred.complete(botguardResponse)
                        }

                        @JavascriptInterface
                        fun onObtainPoTokenResult(identifier: String, tokenU8Csv: String) {
                            if (identifier == "visitorData") {
                                streamingPotDeferred.complete(tokenU8Csv)
                            } else {
                                playerPotDeferred.complete(tokenU8Csv)
                            }
                        }

                        @JavascriptInterface
                        fun onJsError(error: String) {
                            val ex = IOException("BotGuard script error: $error")
                            botguardDeferred.completeExceptionally(ex)
                            streamingPotDeferred.completeExceptionally(ex)
                            playerPotDeferred.completeExceptionally(ex)
                        }
                    }, "JsBridge")
                }
                webView = wv

                wv.loadUrl("file:///android_asset/po_token.html")
                pageLoadedDeferred.await()

                // Jalankan BotGuard snapshot
                val runBotguardJs = """
                    try {
                        const challenge = $parsedChallengeJson;
                        runBotGuard(challenge).then(function(res) {
                            window.webPoSignalOutput = res.webPoSignalOutput;
                            window.JsBridge.onRunBotguardResult(res.botguardResponse);
                        }).catch(function(err) {
                            window.JsBridge.onJsError("runBotGuard error: " + err);
                        });
                    } catch(e) {
                        window.JsBridge.onJsError("challenge eval error: " + e);
                    }
                """.trimIndent()
                wv.evaluateJavascript(runBotguardJs, null)

                val botguardResponse = botguardDeferred.await()

                // Generate IT di background
                val (integrityTokenJs, _) = withContext(Dispatchers.IO) {
                    generateIntegrityToken(botguardResponse)
                }

                val u8Visitor = stringToU8(finalVisitorData)
                val u8VideoId = stringToU8(videoId)

                val obtainJs = """
                    try {
                        window.integrityToken = $integrityTokenJs;
                        const potStreaming = obtainPoToken(window.webPoSignalOutput, window.integrityToken, $u8Visitor);
                        window.JsBridge.onObtainPoTokenResult("visitorData", Array.from(potStreaming).join(","));

                        const potPlayer = obtainPoToken(window.webPoSignalOutput, window.integrityToken, $u8VideoId);
                        window.JsBridge.onObtainPoTokenResult("videoId", Array.from(potPlayer).join(","));
                    } catch (e) {
                        window.JsBridge.onJsError("obtainPoToken error: " + e);
                    }
                """.trimIndent()
                wv.evaluateJavascript(obtainJs, null)

                val streamingCsv = streamingPotDeferred.await()
                val playerCsv = playerPotDeferred.await()

                val streamingPot = u8ToBase64(streamingCsv)
                val playerPot = u8ToBase64(playerCsv)

                PoTokenResult(
                    poToken = playerPot,
                    visitorData = finalVisitorData,
                    isValid = playerPot.isNotBlank(),
                    streamingPoToken = streamingPot
                )
            }

            Log.i(TAG, "PoToken generated successfully: isValid=${result.isValid}, poToken=${result.poToken.take(12)}..., visitorData=${result.visitorData}")
            return result
        } catch (e: Exception) {
            Log.e(TAG, "Gagal mendapatkan PoToken: ${e.message}", e)
            return PoTokenResult(
                poToken = "",
                visitorData = finalVisitorData,
                isValid = false,
                streamingPoToken = ""
            )
        } finally {
            withContext(NonCancellable + Dispatchers.Main.immediate) {
                try {
                    webView?.stopLoading()
                    webView?.loadUrl("about:blank")
                    webView?.clearHistory()
                    webView?.clearCache(true)
                    webView?.destroy()
                } catch (_: Throwable) {}
            }
        }
    }

    private fun fetchVisitorData(): String {
        val payload = """{"context":{"client":{"clientName":"WEB","clientVersion":"2.20241021.01.00","hl":"en","gl":"US"}}}"""
        val request = Request.Builder()
            .url("https://www.youtube.com/youtubei/v1/visitor_id")
            .post(payload.toRequestBody("application/json".toMediaType()))
            .header("User-Agent", USER_AGENT)
            .build()
        httpClient.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("Failed to fetch visitor_id: HTTP ${resp.code}")
            val body = resp.body?.string() ?: throw IOException("Empty visitor_id body")
            val json = JSONObject(body)
            val vd = json.optJSONObject("responseContext")?.optString("visitorData")
                ?: throw IOException("No visitorData in responseContext")
            DownloaderImpl.cachedVisitorData = vd
            return vd
        }
    }

    private fun fetchChallengeData(): String {
        val payload = JSONArray().put(REQUEST_KEY).toString()
        val request = Request.Builder()
            .url("https://www.youtube.com/api/jnn/v1/Create")
            .post(payload.toRequestBody("application/json+protobuf".toMediaType()))
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
            .header("x-goog-api-key", GOOGLE_API_KEY)
            .header("x-user-agent", "grpc-web-javascript/0.1")
            .build()
        httpClient.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("Failed to Create BotGuard: HTTP ${resp.code}")
            val body = resp.body?.string() ?: throw IOException("Empty Create body")
            return parseChallengeData(body)
        }
    }

    private fun generateIntegrityToken(botguardResponse: String): Pair<String, Long> {
        val payload = JSONArray().put(REQUEST_KEY).put(botguardResponse).toString()
        val request = Request.Builder()
            .url("https://www.youtube.com/api/jnn/v1/GenerateIT")
            .post(payload.toRequestBody("application/json+protobuf".toMediaType()))
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
            .header("x-goog-api-key", GOOGLE_API_KEY)
            .header("x-user-agent", "grpc-web-javascript/0.1")
            .build()
        httpClient.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("Failed to GenerateIT: HTTP ${resp.code}")
            val body = resp.body?.string() ?: throw IOException("Empty GenerateIT body")
            return parseIntegrityTokenData(body)
        }
    }

    private fun parseChallengeData(rawChallengeData: String): String {
        val scrambled = JSONArray(rawChallengeData)
        val challengeData = if (scrambled.length() > 1 && scrambled.opt(1) is String) {
            val descrambled = descramble(scrambled.getString(1))
            JSONArray(descrambled)
        } else {
            scrambled.getJSONArray(0)
        }

        val messageId = challengeData.getString(0)
        val interpreterHash = challengeData.getString(3)
        val program = challengeData.getString(4)
        val globalName = challengeData.getString(5)
        val clientExperimentsStateBlob = challengeData.optString(7, "")

        val arr1 = challengeData.optJSONArray(1)
        var privateSafeScript: String? = null
        if (arr1 != null) {
            for (i in 0 until arr1.length()) {
                if (arr1.opt(i) is String) {
                    privateSafeScript = arr1.getString(i)
                    break
                }
            }
        }

        val arr2 = challengeData.optJSONArray(2)
        var privateTrustedUrl: String? = null
        if (arr2 != null) {
            for (i in 0 until arr2.length()) {
                if (arr2.opt(i) is String) {
                    privateTrustedUrl = arr2.getString(i)
                    break
                }
            }
        }

        val interpreterJsObj = JSONObject().apply {
            put("privateDoNotAccessOrElseSafeScriptWrappedValue", privateSafeScript)
            put("privateDoNotAccessOrElseTrustedResourceUrlWrappedValue", privateTrustedUrl)
        }

        val resultObj = JSONObject().apply {
            put("messageId", messageId)
            put("interpreterJavascript", interpreterJsObj)
            put("interpreterHash", interpreterHash)
            put("program", program)
            put("globalName", globalName)
            put("clientExperimentsStateBlob", clientExperimentsStateBlob)
        }

        return resultObj.toString()
    }

    private fun descramble(scrambledChallenge: String): String {
        val base64Mod = scrambledChallenge
            .replace('-', '+')
            .replace('_', '/')
            .replace('.', '=')
        val decoded = Base64.decode(base64Mod, Base64.DEFAULT)
        val shifted = ByteArray(decoded.size) { i -> ((decoded[i].toInt() + 97) and 0xFF).toByte() }
        return String(shifted, Charsets.UTF_8)
    }

    private fun parseIntegrityTokenData(rawIntegrityTokenData: String): Pair<String, Long> {
        val arr = JSONArray(rawIntegrityTokenData)
        val b64 = arr.getString(0)
        val expirationSeconds = arr.optLong(1, 7200L)
        val base64Mod = b64.replace('-', '+').replace('_', '/').replace('.', '=')
        val bytes = Base64.decode(base64Mod, Base64.DEFAULT)
        val u8String = "new Uint8Array([" + bytes.joinToString(",") { (it.toInt() and 0xFF).toString() } + "])"
        return Pair(u8String, expirationSeconds)
    }

    private fun stringToU8(identifier: String): String {
        val bytes = identifier.toByteArray(Charsets.UTF_8)
        return "new Uint8Array([" + bytes.joinToString(",") { (it.toInt() and 0xFF).toString() } + "])"
    }

    private fun u8ToBase64(poTokenCsv: String): String {
        val bytes = poTokenCsv.split(",")
            .filter { it.isNotBlank() }
            .map { (it.trim().toInt() and 0xFF).toByte() }
            .toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
            .replace("+", "-")
            .replace("/", "_")
            .trimEnd('=')
    }
}
