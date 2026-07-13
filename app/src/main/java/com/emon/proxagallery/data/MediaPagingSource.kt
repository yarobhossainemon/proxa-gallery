package com.emon.proxagallery.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException

class MediaPagingSource(
    private val repository: GalleryRepository
) : PagingSource<Int, MediaItem>() {

    override fun getRefreshKey(state: PagingState<Int, MediaItem>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val pageLimit = state.config.pageSize
            (anchorPosition / pageLimit) * pageLimit
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MediaItem> {
        val position = params.key ?: 0
        return try {
            val response = withContext(Dispatchers.IO) {
                repository.getPhotos(offset = position, limit = params.loadSize)
            }
            val nextKey = if (response.size < params.loadSize) {
                null
            } else {
                position + response.size
            }
            LoadResult.Page(
                data = response,
                prevKey = if (position == 0) null else maxOf(0, position - params.loadSize),
                nextKey = nextKey
            )
        } catch (e: Exception) {
            if (e is CancellationException) {
                throw e
            }
            LoadResult.Error(e)
        }
    }
}

class AlbumPagingSource(
    private val repository: GalleryRepository,
    private val bucketId: Long
) : PagingSource<Int, MediaItem>() {

    override fun getRefreshKey(state: PagingState<Int, MediaItem>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val pageLimit = state.config.pageSize
            (anchorPosition / pageLimit) * pageLimit
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MediaItem> {
        val position = params.key ?: 0
        return try {
            val response = withContext(Dispatchers.IO) {
                repository.getPhotosForAlbum(bucketId = bucketId, offset = position, limit = params.loadSize)
            }
            val nextKey = if (response.size < params.loadSize) {
                null
            } else {
                position + response.size
            }
            LoadResult.Page(
                data = response,
                prevKey = if (position == 0) null else maxOf(0, position - params.loadSize),
                nextKey = nextKey
            )
        } catch (e: Exception) {
            if (e is CancellationException) {
                throw e
            }
            LoadResult.Error(e)
        }
    }
}

class SearchPagingSource(
    private val repository: GalleryRepository,
    private val query: String
) : PagingSource<Int, MediaItem>() {

    override fun getRefreshKey(state: PagingState<Int, MediaItem>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val pageLimit = state.config.pageSize
            (anchorPosition / pageLimit) * pageLimit
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MediaItem> {
        val position = params.key ?: 0
        return try {
            val response = withContext(Dispatchers.IO) {
                repository.getPhotosForSearch(query = query, offset = position, limit = params.loadSize)
            }
            val nextKey = if (response.size < params.loadSize) {
                null
            } else {
                position + response.size
            }
            LoadResult.Page(
                data = response,
                prevKey = if (position == 0) null else maxOf(0, position - params.loadSize),
                nextKey = nextKey
            )
        } catch (e: Exception) {
            if (e is CancellationException) {
                throw e
            }
            LoadResult.Error(e)
        }
    }
}
