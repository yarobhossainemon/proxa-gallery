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
    val error: String? = null
)

class GalleryViewModel(
    private val galleryRepository: GalleryRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(GalleryUiState())

    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    init {
        loadPhotos()
    }

    fun loadPhotos() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val photos = withContext(Dispatchers.IO) {
                    galleryRepository.getPhotos()
                }
                _uiState.value = GalleryUiState(photos = photos)
            } catch (exception: SecurityException) {
                _uiState.value = GalleryUiState(
                    isLoading = false,
                    error = "Image access permission is required."
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.value = GalleryUiState(
                    isLoading = false,
                    error = "Unable to load images."
                )
            }
        }
    }
}
