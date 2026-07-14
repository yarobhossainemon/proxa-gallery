package com.emon.proxagallery.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TrashDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: TrashItem)

    @Query("DELETE FROM trash_items WHERE mediaId = :mediaId")
    suspend fun deleteByMediaId(mediaId: Long)

    @Query("SELECT * FROM trash_items ORDER BY deletedAt DESC")
    fun getAllFlow(): Flow<List<TrashItem>>

    @Query("SELECT * FROM trash_items ORDER BY deletedAt DESC")
    suspend fun getAll(): List<TrashItem>

    @Query("SELECT * FROM trash_items WHERE mediaId = :mediaId LIMIT 1")
    suspend fun getByMediaId(mediaId: Long): TrashItem?

    @Query("SELECT * FROM trash_items WHERE expiresAt <= :now")
    suspend fun getExpired(now: Long): List<TrashItem>

    @Query("DELETE FROM trash_items WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)
}
