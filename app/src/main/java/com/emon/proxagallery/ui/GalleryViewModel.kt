package com.emon.proxagallery.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emon.proxagallery.data.Album
import com.emon.proxagallery.data.DeleteResult
import com.emon.proxagallery.data.FavoritesRepository
import com.emon.proxagallery.data.GalleryRepository
import com.emon.proxagallery.data.MediaDetails
import com.emon.proxagallery.data.MediaItem
import com.emon.proxagallery.data.TrashRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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
    private val favoritesRepository: FavoritesRepository,
    private val trashRepository: TrashRepository
) : ViewModel() {
    private var isLoading = false
    private var hasLoadedPhotos = false
    private var isLoadingMore = false
    private var allPhotosLoaded = false
    private val _uiState = MutableStateFlow(GalleryUiState())

    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    private val _currentAlbumId = MutableStateFlow<Long?>(null)
    private val _searchQuery = MutableStateFlow("")

    /** One-time side effects for the viewer screen (delete dialog, navigate-back, etc.). */
    private val _viewerEffects = MutableSharedFlow<ViewerEffect>(extraBufferCapacity = 1)
    val viewerEffects: SharedFlow<ViewerEffect> = _viewerEffects.asSharedFlow()

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

    /**
     * Holds [MediaItem] metadata captured before a delete dialog is launched.
     * Keyed by media ID so concurrent or rapid deletions are always handled correctly.
     * Consumed (and removed) only after RESULT_OK is confirmed.
     */
    private val pendingTrashItems = java.util.concurrent.ConcurrentHashMap<Long, MediaItem>()

    suspend fun getMediaItem(id: Long): MediaItem? {
        return mediaItemCache[id] ?: withContext(Dispatchers.IO) {
            galleryRepository.getPhotoById(id)?.also {
                mediaItemCache[id] = it
            }
        }
    }

    suspend fun getMediaDetails(id: Long): MediaDetails? = withContext(Dispatchers.IO) {
        galleryRepository.getMediaDetails(id)
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

    /**
     * Initiates deletion for the current photo.
     *
     * 1. Captures the full [MediaItem] metadata into [pendingTrashItems] BEFORE any deletion
     *    attempt — this guarantees the data is available even if the file is physically removed
     *    during the system dialog (Android 11+).
     * 2. Delegates to [GalleryRepository.deletePhoto] which follows Scoped Storage rules.
     * 3. On [DeleteResult.RequiresPermission] emits [ViewerEffect.LaunchSystemDeleteDialog] so
     *    the UI can present the OS confirmation dialog via the existing [deleteLauncher].
     * 4. Does NOT update the UI or insert into Room until RESULT_OK is confirmed.
     */
    fun deleteCurrentPhoto(id: Long, uri: Uri) {
        viewModelScope.launch {
            // Capture metadata now, before any file operation, so we never read a deleted file.
            val item = mediaItemCache[id]
            if (item != null) {
                pendingTrashItems[id] = item
            }

            val result = withContext(Dispatchers.IO) {
                galleryRepository.deletePhoto(uri)
            }

            when (result) {
                is DeleteResult.RequiresPermission -> {
                    // Android 11+: retryUri is null (OS handles the physical deletion).
                    // Android 10:  retryUri is non-null (we must retry after permission).
                    _viewerEffects.tryEmit(
                        ViewerEffect.LaunchSystemDeleteDialog(
                            intentSender = result.intentSender,
                            retryUri = result.retryUri
                        )
                    )
                }
                is DeleteResult.Success -> {
                    // Immediate success (own-file deletion without a dialog, uncommon path).
                    commitDeletion(id)
                }
                is DeleteResult.Error -> {
                    // Deletion failed — discard pending metadata, leave UI unchanged.
                    pendingTrashItems.remove(id)
                }
            }
        }
    }

    /**
     * Called after the user confirmed the system delete-permission dialog (RESULT_OK).
     *
     * - Android 11+: the OS already physically deleted the file during the dialog; we only
     *   need to record the deletion in Room and update the UI.
     * - Android 10: [retryUri] is non-null; we must call [GalleryRepository.retryDelete] first.
     */
    fun confirmDeleteAfterPermission(id: Long, retryUri: Uri?) {
        viewModelScope.launch {
            if (retryUri != null) {
                // Android 10 — perform the actual ContentResolver deletion now.
                withContext(Dispatchers.IO) {
                    galleryRepository.retryDelete(retryUri)
                }
            }
            commitDeletion(id)
        }
    }

    /**
     * Inserts the pre-captured [MediaItem] into Room, then updates the UI.
     * Safe to call from any coroutine context; Room and UI updates are dispatched correctly.
     */
    private suspend fun commitDeletion(id: Long) {
        val item = pendingTrashItems.remove(id)
        if (item != null) {
            withContext(Dispatchers.IO) {
                trashRepository.moveToTrash(
                    mediaId = item.id,
                    uri = item.uri,
                    displayName = item.displayName,
                    mimeType = item.mimeType,
                    originalAlbum = item.bucketDisplayName
                )
            }
        }
        onPhotoDeletedSuccess(id)
    }

    private fun onPhotoDeletedSuccess(id: Long) {
        mediaItemCache.remove(id)
        val updatedIds = _uiState.value.viewerPhotoIds.filter { it != id }
        _uiState.value = _uiState.value.copy(viewerPhotoIds = updatedIds)
        if (updatedIds.isEmpty()) {
            _viewerEffects.tryEmit(ViewerEffect.NavigateBack)
        } else {
            _viewerEffects.tryEmit(ViewerEffect.PhotoDeleted)
        }
    }
}
