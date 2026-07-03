package com.zayaanify.privagallery.presentation.viewer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zayaanify.privagallery.domain.model.Photo
import com.zayaanify.privagallery.domain.usecase.GetPhotosInAlbumUseCase
import com.zayaanify.privagallery.domain.usecase.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PhotoViewerUiState(
    val photos: List<Photo> = emptyList(),
    val initialIndex: Int = 0,
    val isLoading: Boolean = true
)

@HiltViewModel
class PhotoViewerViewModel @Inject constructor(
    private val getPhotosInAlbumUseCase: GetPhotosInAlbumUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val bucketId: String = savedStateHandle.get<String>("bucketId") ?: ""
    private val startMediaStoreId: Long = savedStateHandle.get<Long>("mediaStoreId") ?: -1L

    private val _uiState = MutableStateFlow(PhotoViewerUiState())
    val uiState: StateFlow<PhotoViewerUiState> = _uiState.asStateFlow()

    init {
        getPhotosInAlbumUseCase(bucketId)
            .onEach { photos ->
                val startIndex = photos.indexOfFirst { it.mediaStoreId == startMediaStoreId }.coerceAtLeast(0)
                _uiState.value = PhotoViewerUiState(
                    photos = photos,
                    initialIndex = startIndex,
                    isLoading = false
                )
            }
            .launchIn(viewModelScope)
    }

    fun toggleFavorite(mediaStoreId: Long) {
        viewModelScope.launch {
            toggleFavoriteUseCase(mediaStoreId)
        }
    }
}
