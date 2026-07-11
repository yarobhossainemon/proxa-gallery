package com.emon.proxagallery.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val error: String? = null,
    val searchQuery: String = ""
)

class GalleryViewModel(
    private val galleryRepository: GalleryRepository
) : ViewModel() {
    private var allPhotos: List<Photo> = emptyList()
    private var isLoading = false
    private var hasLoadedPhotos = false
    private val _uiState = MutableStateFlow(GalleryUiState())

    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    init {
        loadPhotos()
    }

    fun loadPhotos() {
        if (isLoading || hasLoadedPhotos) return

        isLoading = true
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val photos = withContext(Dispatchers.IO) {
                    galleryRepository.getPhotos()
                }
                allPhotos = photos
                hasLoadedPhotos = true
                isLoading = false
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    allPhotos = photos,
                    photos = filterPhotos(_uiState.value.searchQuery)
                )
            } catch (exception: SecurityException) {
                isLoading = false
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    photos = emptyList(),
                    error = "Image access permission is required."
                )
            } catch (exception: CancellationException) {
                isLoading = false
                throw exception
            } catch (exception: Exception) {
                isLoading = false
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    photos = emptyList(),
                    error = "Unable to load images."
                )
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            photos = filterPhotos(query)
        )
    }

    private fun filterPhotos(query: String): List<Photo> {
        val normalizedQuery = query.trim()

        return if (normalizedQuery.isEmpty()) {
            allPhotos
        } else {
            allPhotos.filter { photo ->
                photo.displayName.contains(normalizedQuery, ignoreCase = true)
            }
        }
    }
}
