package com.emon.proxagallery.ai

interface AiSearchRepository {
    suspend fun search(query: String): List<Long>
    suspend fun searchByTags(tags: List<String>): List<Long>
    suspend fun searchByCategory(categories: List<String>): List<Long>
    suspend fun searchByOCR(text: String): List<Long>
}
