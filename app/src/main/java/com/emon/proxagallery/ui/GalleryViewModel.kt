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
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val favoriteKeys: Set<String> = emptySet(),
    val favoriteItems: List<MediaItem> = emptyList(),
    val albums: List<Album> = emptyList(),
    val selectedAlbumId: Long? = null,
    val selectedTab: Int = 0,
    val viewerPhotoIds: List<Long> = emptyList()
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
        viewModelScope.launch {
            try {
                val loadedAlbums = withContext(Dispatchers.IO) {
                    galleryRepository.getAlbums()
                }
                _uiState.value = _uiState.value.copy(
                    albums = loadedAlbums
                )
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Unable to load albums."
                )
            }
        }
    }

    fun selectAlbum(albumId: Long?) {
        _currentAlbumId.value = albumId
        _uiState.value = _uiState.value.copy(selectedAlbumId = albumId)
    }

    fun selectTab(tabIndex: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = tabIndex)
    }

    private val mediaItemCache = java.util.concurrent.ConcurrentHashMap<Long, MediaItem>()

    suspend fun getMediaItem(id: Long): MediaItem? {
        return mediaItemCache[id] ?: withContext(Dispatchers.IO) {
            galleryRepository.getPhotoById(id)?.also {
                mediaItemCache[id] = it
            }
        }
    }

    fun prepareViewer(photoId: Long) {
        viewModelScope.launch {
            val ids = withContext(Dispatchers.IO) {
                val currentTab = _uiState.value.selectedTab
                val albumId = _uiState.value.selectedAlbumId
                val query = _uiState.value.searchQuery

                when {
                    albumId != null -> galleryRepository.getPhotoIdsForAlbum(albumId)
                    currentTab == 1 -> galleryRepository.getPhotoIdsForSearch(query)
                    currentTab == 3 -> _uiState.value.favoriteItems.map { it.id }
                    else -> galleryRepository.getAllPhotoIds()
                }
            }
            _uiState.value = _uiState.value.copy(viewerPhotoIds = ids)
        }
    }

    private fun observeFavorites() {
        viewModelScope.launch {
            favoritesRepository.favoriteKeys.collect { keys ->
                val favIds = keys.mapNotNull { key ->
                    key.substringAfter(":").toLongOrNull()
                }
                val items = withContext(Dispatchers.IO) {
                    galleryRepository.getPhotosByIds(favIds)
                }
                _uiState.value = _uiState.value.copy(
                    favoriteKeys = keys,
                    favoriteItems = items
                )
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
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }
}
