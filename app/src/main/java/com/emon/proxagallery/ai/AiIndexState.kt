package com.emon.proxagallery.ai

data class AiIndexState(
    val photoId: Long,
    val uri: String,
    val indexingStatus: AiIndexStatus,
    val indexedAt: Long?,
    val aiVersion: Int,
    val sourceModifiedAtMs: Long?
)
