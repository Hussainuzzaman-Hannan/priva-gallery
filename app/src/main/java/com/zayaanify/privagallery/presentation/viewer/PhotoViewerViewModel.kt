package com.zayaanify.privagallery.presentation.viewer

import android.app.PendingIntent
import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zayaanify.privagallery.domain.model.Photo
import com.zayaanify.privagallery.domain.repository.RecycleBinRepository
import com.zayaanify.privagallery.domain.usecase.DeletePhotosUseCase
import com.zayaanify.privagallery.domain.usecase.GetPhotosInAlbumUseCase
import com.zayaanify.privagallery.domain.usecase.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    val currentIndex: Int = 0,
    val isLoading: Boolean = true,
    val deletePendingIntent: PendingIntent? = null
)

@HiltViewModel
class PhotoViewerViewModel @Inject constructor(
    private val getPhotosInAlbumUseCase: GetPhotosInAlbumUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val deletePhotosUseCase: DeletePhotosUseCase,
    private val recycleBinRepository: RecycleBinRepository,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val bucketId: String = savedStateHandle.get<String>("bucketId") ?: ""
    private val startMediaStoreId: Long = savedStateHandle.get<Long>("mediaStoreId") ?: -1L

    private val _uiState = MutableStateFlow(PhotoViewerUiState())
    val uiState: StateFlow<PhotoViewerUiState> = _uiState.asStateFlow()

    init {
        getPhotosInAlbumUseCase(bucketId)
            .onEach { photos ->
                val startIndex = photos.indexOfFirst {
                    it.mediaStoreId == startMediaStoreId
                }.coerceAtLeast(0)

                _uiState.value = PhotoViewerUiState(
                    photos = photos,
                    initialIndex = startIndex,
                    currentIndex = startIndex,
                    isLoading = false
                )
            }
            .launchIn(viewModelScope)
    }

    fun onPageChanged(index: Int) {
        _uiState.value = _uiState.value.copy(currentIndex = index)
    }

    fun toggleFavorite(mediaStoreId: Long) {
        viewModelScope.launch {
            toggleFavoriteUseCase(mediaStoreId)
        }
    }

    fun deleteCurrentPhoto() {
        viewModelScope.launch {
            val current = _uiState.value
            val photo = current.photos.getOrNull(current.currentIndex) ?: return@launch

            // Recycle Bin-এ copy করা
            recycleBinRepository.copyToRecycleBin(photo.mediaStoreId)

            // MediaStore থেকে delete request
            val imageUri = ContentUris.withAppendedId(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                photo.mediaStoreId
            )
            val videoUri = ContentUris.withAppendedId(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                photo.mediaStoreId
            )

            val isImage = context.contentResolver.query(
                imageUri,
                arrayOf(MediaStore.MediaColumns._ID),
                null, null, null
            )?.use { it.count > 0 } ?: false

            val uri = if (isImage) imageUri else videoUri
            val pendingIntent = deletePhotosUseCase(listOf(photo.mediaStoreId))
            _uiState.value = current.copy(deletePendingIntent = pendingIntent)
        }
    }

    fun onDeleteConfirmed() {
        val current = _uiState.value
        val newPhotos = current.photos.toMutableList()

        if (current.currentIndex < newPhotos.size) {
            newPhotos.removeAt(current.currentIndex)
        }

        val newIndex = when {
            newPhotos.isEmpty() -> 0
            current.currentIndex >= newPhotos.size -> newPhotos.size - 1
            else -> current.currentIndex
        }

        _uiState.value = current.copy(
            photos = newPhotos,
            currentIndex = newIndex,
            deletePendingIntent = null
        )
    }
}