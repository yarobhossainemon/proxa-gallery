package com.emon.proxagallery.ai

class EmptyAiSearchRepository : AiSearchRepository {
    override suspend fun search(query: String): List<Long> = emptyList()

    override suspend fun searchByTags(tags: List<String>): List<Long> = emptyList()

    override suspend fun searchByCategory(categories: List<String>): List<Long> = emptyList()

    override suspend fun searchByOCR(text: String): List<Long> = emptyList()
}
