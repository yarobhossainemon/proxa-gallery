package com.emon.proxagallery.ai

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_media_index")
data class AiMediaIndexEntity(
    @PrimaryKey val photoId: Long,
    val uri: String,
    val indexedAt: Long? = null,
    val aiVersion: Int = 0,
    val indexingStatus: AiIndexStatus = AiIndexStatus.NOT_INDEXED,
    val ocrText: String? = null,
    val scene: String? = null,
    val embeddingReference: String? = null,
    val containsSensitiveInfo: Boolean = false,
    val containsPassword: Boolean = false,
    val containsDocument: Boolean = false,
    val containsQRCode: Boolean = false,
    val sourceModifiedAtMs: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
