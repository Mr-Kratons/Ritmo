package com.example.ritmo.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY updatedAt DESC")
    fun observePlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlist_songs ORDER BY playlistId, position, entryId")
    fun observeEntries(): Flow<List<PlaylistSongEntity>>

    @Insert
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Insert
    suspend fun insertEntries(entries: List<PlaylistSongEntity>)

    @Query("UPDATE playlists SET name = :name, updatedAt = :now WHERE id = :id")
    suspend fun rename(id: Long, name: String, now: Long)

    @Query("UPDATE playlists SET updatedAt = :now WHERE id = :id")
    suspend fun touch(id: Long, now: Long)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COALESCE(MAX(position), -1) FROM playlist_songs WHERE playlistId = :id")
    suspend fun maxPosition(id: Long): Int

    @Query("SELECT songId FROM playlist_songs WHERE playlistId = :id")
    suspend fun songIds(id: Long): List<Long>

    @Query("DELETE FROM playlist_songs WHERE entryId IN (:entryIds)")
    suspend fun deleteEntries(entryIds: List<Long>)

    @Query("UPDATE playlist_songs SET position = :position WHERE entryId = :entryId")
    suspend fun setPosition(entryId: Long, position: Int)
}

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<FavoriteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE songId = :songId")
    suspend fun remove(songId: Long)
}

@Dao
interface StatDao {
    @Query("SELECT * FROM play_stats")
    fun observeAll(): Flow<List<PlayStatEntity>>

    @Query("SELECT * FROM play_stats WHERE songId = :songId")
    suspend fun get(songId: Long): PlayStatEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(stat: PlayStatEntity)
}
