package com.emon.proxagallery.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emon.proxagallery.data.Album
import com.emon.proxagallery.data.FavoritesRepository
import com.emon.proxagallery.data.GalleryRepository
import com.emon.proxagallery.data.Photo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class GalleryUiState(
    val isLoading: Boolean = true,
    val photos: List<Photo> = emptyList(),
    val allPhotos: List<Photo> = emptyList(),
    val albumPhotos: List<Photo> = emptyList(),
    val error: String? = null,
    val searchQuery: String = "",
    val favoritePhotoIds: Set<Long> = emptySet(),
    val albums: List<Album> = emptyList(),
    val selectedAlbumId: Long? = null
)

private const val PAGE_SIZE = 200

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

    init {
        loadPhotos()
        observeFavorites()
    }

    fun loadPhotos() {
        if (isLoading || hasLoadedPhotos) return

        isLoading = true
        allPhotosLoaded = false
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

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
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    allPhotos = loadedPhotos,
                    albums = loadedAlbums,
                    photos = filterPhotos(_uiState.value.searchQuery, loadedPhotos)
                )
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
        val currentAllPhotos = _uiState.value.allPhotos
        if (currentAllPhotos.isEmpty() || _uiState.value.selectedAlbumId != null) return

        isLoadingMore = true
        viewModelScope.launch {
            try {
                val morePhotos = withContext(Dispatchers.IO) {
                    galleryRepository.getPhotos(offset = currentAllPhotos.size, limit = PAGE_SIZE)
                }
                if (morePhotos.isEmpty() || morePhotos.size < PAGE_SIZE) {
                    allPhotosLoaded = true
                }
                if (morePhotos.isNotEmpty()) {
                    val updatedAllPhotos = currentAllPhotos + morePhotos
                    _uiState.value = _uiState.value.copy(
                        allPhotos = updatedAllPhotos,
                        photos = filterPhotos(_uiState.value.searchQuery, updatedAllPhotos)
                    )
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
        if (albumId == null) {
            _uiState.value = _uiState.value.copy(
                selectedAlbumId = null,
                albumPhotos = emptyList(),
                photos = filterPhotos(_uiState.value.searchQuery, _uiState.value.allPhotos)
            )
        } else {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            viewModelScope.launch {
                try {
                    val loadedAlbumPhotos = withContext(Dispatchers.IO) {
                        galleryRepository.getPhotosForAlbum(albumId)
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        selectedAlbumId = albumId,
                        albumPhotos = loadedAlbumPhotos,
                        photos = filterPhotos(_uiState.value.searchQuery, loadedAlbumPhotos)
                    )
                } catch (exception: CancellationException) {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    throw exception
                } catch (exception: Exception) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to load album."
                    )
                }
            }
        }
    }

    private fun filterPhotos(query: String, sourceOverride: List<Photo>? = null): List<Photo> {
        val source = sourceOverride ?: if (_uiState.value.selectedAlbumId != null) {
            _uiState.value.albumPhotos
        } else {
            _uiState.value.allPhotos
        }
        val normalizedQuery = query.trim()
        if (normalizedQuery.isEmpty()) return source

        return source.filter { photo ->
            photo.displayName.contains(normalizedQuery, ignoreCase = true)
        }
    }

    private fun emitError(message: String) {
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            photos = emptyList(),
            error = message
        )
    }

    private fun observeFavorites() {
        viewModelScope.launch {
            favoritesRepository.favoriteIds.collect { ids ->
                _uiState.value = _uiState.value.copy(favoritePhotoIds = ids)
            }
        }
    }

    fun toggleFavorite(photoId: Long) {
        viewModelScope.launch {
            favoritesRepository.toggle(photoId)
        }
    }

    fun isFavorite(photoId: Long): Boolean =
        _uiState.value.favoritePhotoIds.contains(photoId)

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            photos = filterPhotos(query)
        )
    }
}
