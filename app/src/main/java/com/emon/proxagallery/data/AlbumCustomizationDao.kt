package com.emon.proxagallery.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface AlbumCustomizationDao {

    @Upsert
    suspend fun upsert(customization: AlbumCustomization)

    @Query("SELECT * FROM album_customizations WHERE bucketId = :bucketId LIMIT 1")
    suspend fun getByBucketId(bucketId: Long): AlbumCustomization?

    @Query("SELECT * FROM album_customizations")
    fun getAllFlow(): Flow<List<AlbumCustomization>>

    @Query("SELECT * FROM album_customizations")
    suspend fun getAll(): List<AlbumCustomization>

    @Query("DELETE FROM album_customizations WHERE bucketId = :bucketId")
    suspend fun deleteByBucketId(bucketId: Long)

    @Query("DELETE FROM album_customizations WHERE bucketId IN (:bucketIds)")
    suspend fun deleteByBucketIds(bucketIds: List<Long>)

    @Query("SELECT * FROM album_customizations WHERE isHidden = 1")
    fun getHiddenFlow(): Flow<List<AlbumCustomization>>
}
