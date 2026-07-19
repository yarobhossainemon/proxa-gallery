package com.emon.proxagallery.ai

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface AiIndexDao {
    @Upsert
    suspend fun upsertMediaIndex(entity: AiMediaIndexEntity)

    @Upsert
    suspend fun upsertMediaIndexes(entities: List<AiMediaIndexEntity>)

    @Upsert
    suspend fun upsertTextAttributes(entities: List<AiTextAttributeEntity>)

    @Upsert
    suspend fun upsertCategories(entities: List<AiCategoryEntity>)

    @Upsert
    suspend fun upsertObjects(entities: List<AiObjectEntity>)

    @Upsert
    suspend fun upsertTags(entities: List<AiTagEntity>)

    @Upsert
    suspend fun upsertDetectedApps(entities: List<AiDetectedAppEntity>)

    @Query("UPDATE ai_media_index SET indexingStatus = :status, updatedAt = :updatedAt WHERE photoId = :photoId")
    suspend fun updateStatus(photoId: Long, status: AiIndexStatus, updatedAt: Long)

    @Query(
        "UPDATE ai_media_index SET indexingStatus = :status, indexedAt = :indexedAt, aiVersion = :aiVersion, sourceModifiedAtMs = :sourceModifiedAtMs, updatedAt = :updatedAt WHERE photoId = :photoId"
    )
    suspend fun markIndexed(
        photoId: Long,
        status: AiIndexStatus,
        indexedAt: Long,
        aiVersion: Int,
        sourceModifiedAtMs: Long?,
        updatedAt: Long
    )

    @Query("UPDATE ai_media_index SET indexingStatus = :status, updatedAt = :updatedAt WHERE photoId = :photoId")
    suspend fun markFailed(photoId: Long, status: AiIndexStatus, updatedAt: Long)

    @Query("DELETE FROM ai_media_index WHERE photoId = :photoId")
    suspend fun deleteIndex(photoId: Long)

    @Query("DELETE FROM ai_text_attributes WHERE photoId = :photoId")
    suspend fun deleteTextAttributes(photoId: Long)

    @Query("DELETE FROM ai_categories WHERE photoId = :photoId")
    suspend fun deleteCategories(photoId: Long)

    @Query("DELETE FROM ai_objects WHERE photoId = :photoId")
    suspend fun deleteObjects(photoId: Long)

    @Query("DELETE FROM ai_tags WHERE photoId = :photoId")
    suspend fun deleteTags(photoId: Long)

    @Query("DELETE FROM ai_detected_apps WHERE photoId = :photoId")
    suspend fun deleteDetectedApps(photoId: Long)

    @Query("SELECT * FROM ai_media_index WHERE photoId = :photoId LIMIT 1")
    suspend fun getIndex(photoId: Long): AiMediaIndexEntity?

    @Query("SELECT * FROM ai_media_index WHERE photoId = :photoId LIMIT 1")
    suspend fun getIndexState(photoId: Long): AiIndexState?

    @Query(
        "SELECT * FROM ai_media_index WHERE indexingStatus IN ('NOT_INDEXED', 'FAILED') OR aiVersion < :currentVersion ORDER BY indexedAt IS NOT NULL, indexedAt ASC LIMIT :limit"
    )
    suspend fun getPendingIndexes(currentVersion: Int, limit: Int): List<AiMediaIndexEntity>

    @Query(
        "SELECT * FROM ai_media_index WHERE aiVersion < :currentVersion OR indexingStatus = 'FAILED' ORDER BY updatedAt ASC"
    )
    suspend fun getOutdatedIndexes(currentVersion: Int): List<AiMediaIndexEntity>

    @Query("SELECT COUNT(*) FROM ai_media_index WHERE indexingStatus = :status")
    suspend fun countByStatus(status: AiIndexStatus): Int

    @Query("SELECT * FROM ai_media_index WHERE photoId = :photoId LIMIT 1")
    suspend fun observeIndexState(photoId: Long): AiIndexState?

    @Query("DELETE FROM ai_media_index")
    suspend fun clearAllIndexes()

    @Query("DELETE FROM ai_text_attributes")
    suspend fun clearAllTextAttributes()

    @Query("DELETE FROM ai_categories")
    suspend fun clearAllCategories()

    @Query("DELETE FROM ai_objects")
    suspend fun clearAllObjects()

    @Query("DELETE FROM ai_tags")
    suspend fun clearAllTags()

    @Query("DELETE FROM ai_detected_apps")
    suspend fun clearAllDetectedApps()
}
