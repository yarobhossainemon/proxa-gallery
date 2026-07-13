package com.emon.proxagallery.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emon.proxagallery.data.Album
import com.emon.proxagallery.data.FavoritesRepository
import com.emon.proxagallery.data.GalleryRepository
import com.emon.proxagallery.data.MediaItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.emon.proxagallery.data.MediaPagingSource
import com.emon.proxagallery.data.AlbumPagingSource
import com.emon.proxagallery.data.SearchPagingSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest


data class GalleryUiState(
    val isLoading: Boolean = true,
    val photos: List<MediaItem> = emptyList(),
    val allPhotos: List<MediaItem> = emptyList(),
    val albumPhotos: List<MediaItem> = emptyList(),
    val error: String? = null,
    val searchQuery: String = "",
    val favoriteKeys: Set<String> = emptySet(),
    val albums: List<Album> = emptyList(),
    val selectedAlbumId: Long? = null,
    val selectedTab: Int = 0
)

private const val PAGE_SIZE = 100
private const val PREFETCH_DISTANCE = 30

class GalleryViewModel(
    private val galleryRepository: GalleryRepository,
    private val favoritesRepository: FavoritesRepository
) : ViewModel() {
    private var isLoading = false
    private var hasLoadedPhotos = false
    private var isLoadingMore = false
    private var allPhotosLoaded = false
    private val _uiState = MutableStateFlow(GalleryUiState())

    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    private val _currentAlbumId = MutableStateFlow<Long?>(null)
    private val _searchQuery = MutableStateFlow("")

    val allPhotosFlow: Flow<PagingData<MediaItem>> = Pager(
        config = PagingConfig(
            pageSize = PAGE_SIZE,
            enablePlaceholders = false,
            prefetchDistance = PREFETCH_DISTANCE
        )
    ) {
        MediaPagingSource(galleryRepository)
    }.flow.cachedIn(viewModelScope)

    @OptIn(ExperimentalCoroutinesApi::class)
    val albumPhotosFlow: Flow<PagingData<MediaItem>> = _currentAlbumId.flatMapLatest { albumId ->
        Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                enablePlaceholders = false,
                prefetchDistance = PREFETCH_DISTANCE
            )
        ) {
            if (albumId != null) {
                AlbumPagingSource(galleryRepository, albumId)
            } else {
                MediaPagingSource(galleryRepository)
            }
        }.flow
    }.cachedIn(viewModelScope)

    @OptIn(ExperimentalCoroutinesApi::class)
    val searchPhotosFlow: Flow<PagingData<MediaItem>> = _searchQuery.flatMapLatest { query ->
        Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                enablePlaceholders = false,
                prefetchDistance = PREFETCH_DISTANCE
            )
        ) {
            SearchPagingSource(galleryRepository, query)
        }.flow
    }.cachedIn(viewModelScope)


    init {
        loadPhotos()
        observeFavorites()
    }

    fun loadPhotos() {
        if (isLoading || hasLoadedPhotos) return

        isLoading = true
        allPhotosLoaded = false
        viewModelScope.launch {
            val loadingState = _uiState.value.copy(isLoading = true, error = null)
            if (_uiState.value != loadingState) {
                _uiState.value = loadingState
            }

            try {
                val loadedPhotos = withContext(Dispatchers.IO) {
                    galleryRepository.getPhotos(offset = 0, limit = PAGE_SIZE)
                }
                val loadedAlbums = withContext(Dispatchers.IO) {
                    galleryRepository.getAlbums()
                }
                hasLoadedPhotos = true
                if (loadedPhotos.size < PAGE_SIZE) {
                    allPhotosLoaded = true
                }
                val updatedState = _uiState.value.copy(
                    isLoading = false,
                    allPhotos = loadedPhotos,
                    albums = loadedAlbums
                )
                val newState = updatedState.copy(
                    photos = withContext(Dispatchers.Default) {
                        filterPhotos(updatedState.searchQuery, updatedState, loadedPhotos)
                    }
                )
                if (_uiState.value != newState) {
                    _uiState.value = newState
                }
            } catch (exception: SecurityException) {
                emitError("Image access permission is required.")
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                emitError("Unable to load images.")
            } finally {
                isLoading = false
            }
        }
    }

    fun loadNextPage() {
        if (isLoadingMore || !hasLoadedPhotos || allPhotosLoaded) return
        
        val isAlbum = _uiState.value.selectedAlbumId != null
        val currentList = if (isAlbum) _uiState.value.albumPhotos else _uiState.value.allPhotos
        if (currentList.isEmpty()) return

        isLoadingMore = true
        viewModelScope.launch {
            try {
                val morePhotos = withContext(Dispatchers.IO) {
                    if (isAlbum) {
                        galleryRepository.getPhotosForAlbum(
                            bucketId = _uiState.value.selectedAlbumId!!,
                            offset = currentList.size,
                            limit = PAGE_SIZE
                        )
                    } else {
                        galleryRepository.getPhotos(
                            offset = currentList.size,
                            limit = PAGE_SIZE
                        )
                    }
                }
                if (morePhotos.isEmpty() || morePhotos.size < PAGE_SIZE) {
                    allPhotosLoaded = true
                }
                if (morePhotos.isNotEmpty()) {
                    val updatedList = currentList + morePhotos
                    val updatedState = if (isAlbum) {
                        _uiState.value.copy(albumPhotos = updatedList)
                    } else {
                        _uiState.value.copy(allPhotos = updatedList)
                    }
                    val newState = updatedState.copy(
                        photos = withContext(Dispatchers.Default) {
                            filterPhotos(updatedState.searchQuery, updatedState, updatedList)
                        }
                    )
                    if (_uiState.value != newState) {
                        _uiState.value = newState
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
            } finally {
                isLoadingMore = false
            }
        }
    }

    fun selectAlbum(albumId: Long?) {
        _currentAlbumId.value = albumId
        allPhotosLoaded = false
        if (albumId == null) {
            val updatedState = _uiState.value.copy(
                selectedAlbumId = null,
                albumPhotos = emptyList()
            )
            viewModelScope.launch {
                val filtered = withContext(Dispatchers.Default) {
                    filterPhotos(updatedState.searchQuery, updatedState, updatedState.allPhotos)
                }
                val newState = updatedState.copy(photos = filtered)
                if (_uiState.value != newState) {
                    _uiState.value = newState
                }
            }
        } else {
            val loadingState = _uiState.value.copy(isLoading = true, error = null)
            if (_uiState.value != loadingState) {
                _uiState.value = loadingState
            }
            viewModelScope.launch {
                try {
                    val loadedAlbumPhotos = withContext(Dispatchers.IO) {
                        galleryRepository.getPhotosForAlbum(albumId, offset = 0, limit = PAGE_SIZE)
                    }
                    if (loadedAlbumPhotos.size < PAGE_SIZE) {
                        allPhotosLoaded = true
                    }
                    val updatedState = _uiState.value.copy(
                        isLoading = false,
                        selectedAlbumId = albumId,
                        albumPhotos = loadedAlbumPhotos
                    )
                    val newState = updatedState.copy(
                        photos = withContext(Dispatchers.Default) {
                            filterPhotos(updatedState.searchQuery, updatedState, loadedAlbumPhotos)
                        }
                    )
                    if (_uiState.value != newState) {
                        _uiState.value = newState
                    }
                } catch (exception: CancellationException) {
                    val errorState = _uiState.value.copy(isLoading = false)
                    if (_uiState.value != errorState) {
                        _uiState.value = errorState
                    }
                    throw exception
                } catch (exception: Exception) {
                    val errorState = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to load album."
                    )
                    if (_uiState.value != errorState) {
                        _uiState.value = errorState
                    }
                }
            }
        }
    }

    fun selectTab(tabIndex: Int) {
        val updatedState = _uiState.value.copy(selectedTab = tabIndex)
        viewModelScope.launch {
            val filtered = withContext(Dispatchers.Default) {
                filterPhotos(updatedState.searchQuery, updatedState)
            }
            val newState = updatedState.copy(photos = filtered)
            if (_uiState.value != newState) {
                _uiState.value = newState
            }
        }
    }

    private fun filterPhotos(
        query: String,
        state: GalleryUiState = _uiState.value,
        sourceOverride: List<MediaItem>? = null
    ): List<MediaItem> {
        val source = sourceOverride ?: if (state.selectedAlbumId != null) {
            state.albumPhotos
        } else {
            state.allPhotos
        }
        val normalizedQuery = query.trim()
        val filtered = if (normalizedQuery.isEmpty()) {
            source
        } else {
            val queryLower = normalizedQuery.lowercase()
            source.filter { item ->
                item.displayName.contains(queryLower, ignoreCase = true) ||
                    item.mimeType.contains(queryLower, ignoreCase = true) ||
                    item.bucketDisplayName?.contains(queryLower, ignoreCase = true) == true ||
                    item.fileExtension.contains(queryLower)
            }
        }

        return if (state.selectedAlbumId == null && state.selectedTab == 2) {
            filtered.sortedWith { a, b ->
                val aTime = maxOf(a.dateTakenMs ?: 0L, (a.dateModifiedSec ?: 0L) * 1000L)
                val bTime = maxOf(b.dateTakenMs ?: 0L, (b.dateModifiedSec ?: 0L) * 1000L)
                bTime.compareTo(aTime)
            }
        } else {
            filtered
        }
    }

    private fun emitError(message: String) {
        val newState = _uiState.value.copy(
            isLoading = false,
            photos = emptyList(),
            error = message
        )
        if (_uiState.value != newState) {
            _uiState.value = newState
        }
    }

    private fun observeFavorites() {
        viewModelScope.launch {
            favoritesRepository.favoriteKeys.collect { keys ->
                val newState = _uiState.value.copy(favoriteKeys = keys)
                if (_uiState.value != newState) {
                    _uiState.value = newState
                }
            }
        }
    }

    fun toggleFavorite(id: Long, isVideo: Boolean) {
        viewModelScope.launch {
            favoritesRepository.toggle(id, isVideo)
        }
    }

    fun isFavorite(id: Long, isVideo: Boolean): Boolean {
        val key = if (isVideo) "v:$id" else "i:$id"
        return _uiState.value.favoriteKeys.contains(key)
    }

    fun onSearchQueryChange(query: String) {
        if (_uiState.value.searchQuery == query) return
        _searchQuery.value = query
        val updatedState = _uiState.value.copy(searchQuery = query)
        viewModelScope.launch {
            val filtered = withContext(Dispatchers.Default) {
                filterPhotos(query, updatedState)
            }
            val newState = updatedState.copy(photos = filtered)
            if (_uiState.value != newState) {
                _uiState.value = newState
            }
        }
    }
}
