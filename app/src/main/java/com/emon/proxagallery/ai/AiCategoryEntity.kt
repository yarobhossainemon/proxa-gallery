package com.emon.proxagallery.ai

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "ai_categories",
    primaryKeys = ["photoId", "value"],
    foreignKeys = [
        ForeignKey(
            entity = AiMediaIndexEntity::class,
            parentColumns = ["photoId"],
            childColumns = ["photoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("photoId")]
)
data class AiCategoryEntity(
    val photoId: Long,
    val value: String
)
