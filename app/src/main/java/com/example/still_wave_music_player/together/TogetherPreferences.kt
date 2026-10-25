package com.example.still_wave_music_player.together

import android.content.Context
import android.content.SharedPreferences
import java.net.URI
import java.util.Locale
import java.util.UUID

/**
 * Pengelolaan preferensi lokal SharedPreferences "pengaturan" untuk Listening Together.
 */
class TogetherPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("pengaturan", Context.MODE_PRIVATE)

    var serverUrl: String
        get() = prefs.getString("serverUrl", "") ?: ""
        set(value) = prefs.edit().putString("serverUrl", value).apply()

    var nickname: String
        get() = prefs.getString("nickname", "Aku") ?: "Aku"
        set(value) = prefs.edit().putString("nickname", value.ifBlank { "Aku" }).apply()

    val deviceId: String
        get() {
            var id = prefs.getString("deviceId", null)
            if (id.isNullOrBlank()) {
                id = UUID.randomUUID().toString()
                prefs.edit().putString("deviceId", id).apply()
            }
            return id
        }

    companion object {
        /**
         * Validasi serverUrl:
         * - dipangkas spasi
         * - tanpa garis miring di akhir
         * - harus diawali "https://"
         * - host berakhiran "firebasedatabase.app" atau "firebaseio.com"
         * Selain itu tolak dengan pesan "Alamat server tidak valid".
         */
        fun validateServerUrl(rawUrl: String): Result<String> {
            val trimmed = rawUrl.trim().trimEnd('/')
            if (trimmed.isBlank() || !trimmed.startsWith("https://")) {
                return Result.failure(Exception("Alamat server tidak valid"))
            }
            val uri = try {
                URI(trimmed)
            } catch (e: Exception) {
                return Result.failure(Exception("Alamat server tidak valid"))
            }
            val host = uri.host?.lowercase(Locale.ROOT) ?: ""
            if (!host.endsWith("firebasedatabase.app") && !host.endsWith("firebaseio.com")) {
                return Result.failure(Exception("Alamat server tidak valid"))
            }
            return Result.success(trimmed)
        }
    }
}
