package com.example.still_wave_music_player.utils

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.util.LruCache
import android.util.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.still_wave_music_player.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Collections

/**
 * Pemuat sampul album lagu asli dari penyimpanan lokal perangkat.
 * Menggunakan memori cache LruCache dan mencatat lagu tanpa sampul agar tidak dimuat berulang.
 */
object AlbumArtLoader {
    private const val TAG = "MusikPlayer"

    // Ukuran cache maksimal sekitar 1/8 memori heap atau 24 MB, dihitung dari byteCount
    private val maxMemoryBytes = Runtime.getRuntime().maxMemory()
    private val cacheSizeBytes = (maxMemoryBytes / 8L)
        .coerceAtMost(24L * 1024L * 1024L)
        .coerceAtLeast(4L * 1024L * 1024L)
        .toInt()

    private val memoryCache = object : LruCache<String, ImageBitmap>(cacheSizeBytes) {
        override fun sizeOf(key: String, value: ImageBitmap): Int {
            return value.asAndroidBitmap().byteCount
        }
    }

    // Himpunan id lagu yang hasilnya null untuk mencegah pemanggilan berulang
    private val noArtSongIds = Collections.synchronizedSet(HashSet<String>())

    /**
     * Memeriksa dan mengambil sampul dari cache memori jika sudah tersedia.
     */
    fun getCached(songId: String, sizePx: Int): ImageBitmap? {
        val cacheKey = "$songId-$sizePx"
        return memoryCache.get(cacheKey)
    }

    /**
     * Memuat sampul album lagu di Dispatchers.IO.
     * Mengembalikan ImageBitmap jika berhasil, atau null jika tidak ada/gagal.
     */
    suspend fun loadAlbumArt(
        context: Context,
        song: Song,
        sizePx: Int
    ): ImageBitmap? = withContext(Dispatchers.IO) {
        val cacheKey = "${song.id}-$sizePx"

        // Cek cache memori terlebih dahulu
        val cached = memoryCache.get(cacheKey)
        if (cached != null) {
            return@withContext cached
        }

        // Jika lagu sudah tercatat tidak memiliki sampul, langsung kembalikan null
        if (noArtSongIds.contains(song.id)) {
            return@withContext null
        }

        try {
            val contentResolver = context.contentResolver

            // 1. Android 10 (API 29) ke atas: ContentResolver.loadThumbnail
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val uri = if (!song.contentUri.isNullOrBlank()) {
                    Uri.parse(song.contentUri)
                } else {
                    val idLong = song.id.toLongOrNull()
                    if (idLong != null) {
                        ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, idLong)
                    } else {
                        null
                    }
                }

                if (uri != null) {
                    try {
                        val bitmap = contentResolver.loadThumbnail(uri, Size(sizePx, sizePx), null)
                        val imageBitmap = bitmap.asImageBitmap()
                        memoryCache.put(cacheKey, imageBitmap)
                        return@withContext imageBitmap
                    } catch (e: Exception) {
                        Log.d(TAG, "loadThumbnail gagal untuk lagu id=${song.id}: ${e.message}")
                    }
                }
            }

            // 2. Android 8 sampai 9 (API 26-28) dan cadangan: buka content://media/external/audio/albumart/{albumId}
            if (song.albumId > 0L) {
                val albumArtUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    song.albumId
                )

                val boundsOptions = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }

                contentResolver.openInputStream(albumArtUri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, boundsOptions)
                }

                if (boundsOptions.outWidth > 0 && boundsOptions.outHeight > 0) {
                    var sampleSize = 1
                    if (boundsOptions.outHeight > sizePx || boundsOptions.outWidth > sizePx) {
                        val halfHeight = boundsOptions.outHeight / 2
                        val halfWidth = boundsOptions.outWidth / 2
                        while ((halfHeight / sampleSize) >= sizePx && (halfWidth / sampleSize) >= sizePx) {
                            sampleSize *= 2
                        }
                    }

                    val decodeOptions = BitmapFactory.Options().apply {
                        inSampleSize = sampleSize
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                    }

                    val bitmap = contentResolver.openInputStream(albumArtUri)?.use { stream ->
                        BitmapFactory.decodeStream(stream, null, decodeOptions)
                    }

                    if (bitmap != null) {
                        val imageBitmap = bitmap.asImageBitmap()
                        memoryCache.put(cacheKey, imageBitmap)
                        return@withContext imageBitmap
                    }
                }
            }

            // Jika sampul tidak ditemukan di kedua metode
            noArtSongIds.add(song.id)
            null
        } catch (e: Exception) {
            Log.d(TAG, "Gagal memuat sampul album untuk lagu id=${song.id}: ${e.message}")
            noArtSongIds.add(song.id)
            null
        } catch (t: Throwable) {
            Log.e(TAG, "Kesalahan sistem saat memuat sampul album id=${song.id}: ${t.message}", t)
            noArtSongIds.add(song.id)
            null
        }
    }
}

/**
 * Fungsi pembantu suspend level atas untuk memuat sampul album.
 */
suspend fun loadAlbumArt(
    context: Context,
    song: Song,
    sizePx: Int
): ImageBitmap? = AlbumArtLoader.loadAlbumArt(context, song, sizePx)
