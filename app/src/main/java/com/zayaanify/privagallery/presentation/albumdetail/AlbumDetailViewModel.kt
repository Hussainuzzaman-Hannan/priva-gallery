package com.zayaanify.privagallery.presentation.albumdetail

import android.app.PendingIntent
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zayaanify.privagallery.domain.model.Photo
import com.zayaanify.privagallery.domain.usecase.DeletePhotosUseCase
import com.zayaanify.privagallery.domain.usecase.GetPhotosInAlbumUseCase
import com.zayaanify.privagallery.domain.usecase.MoveToVaultUseCase
import com.zayaanify.privagallery.domain.usecase.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlbumDetailUiState(
    val albumName: String = "",
    val photos: List<Photo> = emptyList(),
    val selectedIds: Set<Long> = emptySet(),
    val isSelectionMode: Boolean = false,
    val isLoading: Boolean = true,
    val deletePendingIntent: PendingIntent? = null,
    val message: String? = null
)

@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    private val getPhotosInAlbumUseCase: GetPhotosInAlbumUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val deletePhotosUseCase: DeletePhotosUseCase,
    private val moveToVaultUseCase: MoveToVaultUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val bucketId: String = savedStateHandle.get<String>("bucketId") ?: ""
    private val albumName: String = (savedStateHandle.get<String>("albumName") ?: "")
        .let { java.net.URLDecoder.decode(it, "UTF-8") }

    private val _uiState = MutableStateFlow(AlbumDetailUiState(albumName = albumName))
    val uiState: StateFlow<AlbumDetailUiState> = _uiState.asStateFlow()

    init {
        getPhotosInAlbumUseCase(bucketId)
            .onEach { photos ->
                _uiState.value = _uiState.value.copy(photos = photos, isLoading = false)
            }
            .launchIn(viewModelScope)
    }

    fun onPhotoLongPress(mediaStoreId: Long) {
        val newSelected = _uiState.value.selectedIds + mediaStoreId
        _uiState.value = _uiState.value.copy(
            selectedIds = newSelected,
            isSelectionMode = true
        )
    }

    fun onPhotoTapInSelectionMode(mediaStoreId: Long) {
        val current = _uiState.value
        val newSelected = if (mediaStoreId in current.selectedIds) {
            current.selectedIds - mediaStoreId
        } else {
            current.selectedIds + mediaStoreId
        }
        _uiState.value = current.copy(
            selectedIds = newSelected,
            isSelectionMode = newSelected.isNotEmpty()
        )
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(
            selectedIds = emptySet(),
            isSelectionMode = false
        )
    }

    fun toggleFavorite(mediaStoreId: Long) {
        viewModelScope.launch {
            toggleFavoriteUseCase(mediaStoreId)
        }
    }

    fun requestDeleteSelected() {
        viewModelScope.launch {
            val ids = _uiState.value.selectedIds.toList()
            if (ids.isEmpty()) return@launch
            val pendingIntent = deletePhotosUseCase(ids)
            _uiState.value = _uiState.value.copy(deletePendingIntent = pendingIntent)
        }
    }

    fun consumeDeletePendingIntent() {
        _uiState.value = _uiState.value.copy(deletePendingIntent = null)
        clearSelection()
    }

    fun moveSelectedToVault() {
        viewModelScope.launch {
            val selectedPhotos = _uiState.value.photos
                .filter { it.mediaStoreId in _uiState.value.selectedIds }

            var successCount = 0
            selectedPhotos.forEach { photo ->
                moveToVaultUseCase(
                    mediaStoreId = photo.mediaStoreId,
                    mimeType = photo.mimeType,
                    displayName = photo.displayName,
                    sizeBytes = photo.sizeBytes,
                    dateTakenMillis = photo.dateTakenMillis
                ).onSuccess { successCount++ }
            }

            _uiState.value = _uiState.value.copy(
                isSelectionMode = false,
                selectedIds = emptySet(),
                message = "$successCount টা ফটো Vault-এ নেওয়া হয়েছে"
            )
        }
    }

    fun consumeMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}