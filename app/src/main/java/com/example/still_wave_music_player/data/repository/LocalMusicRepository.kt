package com.example.still_wave_music_player.data.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.example.still_wave_music_player.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repositori untuk membaca lagu asli dari penyimpanan lokal perangkat menggunakan MediaStore.
 */
class LocalMusicRepository(private val context: Context) {

    /**
     * Membaca daftar lagu audio dari MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.
     * Hanya mengambil file musik (IS_MUSIC != 0) dengan durasi minimal 30 detik (>= 30000 ms).
     */
    suspend fun getSongs(): List<Song> = withContext(Dispatchers.IO) {
        val songList = mutableListOf<Song>()
        val contentResolver: ContentResolver = context.contentResolver
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.ALBUM_ID
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= ?"
        val selectionArgs = arrayOf("30000")
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        try {
            contentResolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

                while (cursor.moveToNext()) {
                    val idLong = cursor.getLong(idColumn)
                    val rawTitle = cursor.getString(titleColumn) ?: ""
                    val rawArtist = cursor.getString(artistColumn) ?: ""
                    val rawAlbum = cursor.getString(albumColumn) ?: ""
                    val duration = cursor.getLong(durationColumn)
                    val dateAdded = cursor.getLong(dateAddedColumn)
                    val albumId = cursor.getLong(albumIdColumn)

                    val title = if (rawTitle.isBlank()) "Tanpa Judul" else rawTitle
                    val artist = if (rawArtist.isBlank() || rawArtist.equals("<unknown>", ignoreCase = true)) {
                        "Artis tidak diketahui"
                    } else {
                        rawArtist
                    }
                    val album = if (rawAlbum.isBlank() || rawAlbum.equals("<unknown>", ignoreCase = true)) {
                        "Album tidak diketahui"
                    } else {
                        rawAlbum
                    }

                    val uri = ContentUris.withAppendedId(collection, idLong).toString()

                    songList.add(
                        Song(
                            id = idLong.toString(),
                            title = title,
                            artist = artist,
                            album = album,
                            durationMs = duration,
                            thumbnailUrl = null,
                            contentUri = uri,
                            dateAdded = dateAdded,
                            albumId = albumId
                        )
                    )
                }
            }
        } catch (e: SecurityException) {
            return@withContext emptyList()
        } catch (e: Exception) {
            return@withContext emptyList()
        }

        songList
    }
}
