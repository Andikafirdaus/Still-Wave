package com.example.still_wave_music_player.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: PlayHistory)

    @Query("SELECT * FROM play_history ORDER BY playedAt DESC LIMIT :limit")
    fun getRecent(limit: Int = 100): Flow<List<PlayHistory>>

    @Query("SELECT * FROM play_history ORDER BY playedAt DESC LIMIT :limit")
    suspend fun getRecentSuspend(limit: Int = 100): List<PlayHistory>

    @Query("SELECT * FROM play_history WHERE playedAt >= :since ORDER BY playedAt DESC")
    suspend fun getSince(since: Long): List<PlayHistory>

    @Query("DELETE FROM play_history")
    suspend fun deleteAll()
}

@Dao
interface FavoriteSongDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(song: FavoriteSong)

    @Query("DELETE FROM favorite_songs WHERE songKey = :songKey")
    suspend fun deleteByKey(songKey: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_songs WHERE songKey = :songKey)")
    fun isFavorite(songKey: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_songs WHERE songKey = :songKey)")
    suspend fun isFavoriteSuspend(songKey: String): Boolean

    @Query("SELECT * FROM favorite_songs ORDER BY addedAt DESC")
    fun getAll(): Flow<List<FavoriteSong>>
}

@Dao
interface PlaylistDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(playlist: Playlist): Long

    @Update
    suspend fun update(playlist: Playlist)

    @Delete
    suspend fun delete(playlist: Playlist)

    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAll(): Flow<List<Playlist>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getById(id: Long): Playlist?
}

@Dao
interface PlaylistSongDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(song: PlaylistSong)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songKey = :songKey")
    suspend fun delete(playlistId: Long, songKey: String)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun deleteAllForPlaylist(playlistId: Long)

    @Query("SELECT * FROM playlist_songs WHERE playlistId = :playlistId ORDER BY position ASC")
    fun getSongsForPlaylist(playlistId: Long): Flow<List<PlaylistSong>>

    @Query("SELECT * FROM playlist_songs WHERE playlistId = :playlistId ORDER BY position ASC")
    suspend fun getSongsForPlaylistSuspend(playlistId: Long): List<PlaylistSong>
}
