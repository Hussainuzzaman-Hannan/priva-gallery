package com.zayaanify.privagallery.presentation.albumdetail

import android.app.PendingIntent
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zayaanify.privagallery.domain.model.Photo
import com.zayaanify.privagallery.domain.repository.RecycleBinRepository
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
    val message: String? = null,
    // Recycle Bin-এ copy হওয়ার পর delete confirm করার জন্য
    val pendingRecycleBinDeleteIds: List<Long> = emptyList()
)

@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    private val getPhotosInAlbumUseCase: GetPhotosInAlbumUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val deletePhotosUseCase: DeletePhotosUseCase,
    private val moveToVaultUseCase: MoveToVaultUseCase,
    private val recycleBinRepository: RecycleBinRepository,
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

    /**
     * ধাপ ১: ফাইল Recycle Bin-এ copy করা
     * ধাপ ২: MediaStore থেকে delete করার জন্য PendingIntent চাওয়া
     * ধাপ ৩: User confirm করলে consumeDeletePendingIntent() call হবে
     *         এবং Recycle Bin DB entry থেকে যাবে
     */
    fun requestDeleteSelected() {
        viewModelScope.launch {
            val selectedPhotos = _uiState.value.photos
                .filter { it.mediaStoreId in _uiState.value.selectedIds }

            // ধাপ ১: Recycle Bin-এ copy করা
            val copiedIds = mutableListOf<Long>()
            selectedPhotos.forEach { photo ->
                recycleBinRepository.copyToRecycleBin(photo.mediaStoreId)
                    .onSuccess { copiedIds.add(photo.mediaStoreId) }
            }

            if (copiedIds.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    message = "Recycle Bin-এ রাখা যায়নি"
                )
                return@launch
            }

            // ধাপ ২: MediaStore delete request পাঠানো
            val pendingIntent = deletePhotosUseCase(copiedIds)
            _uiState.value = _uiState.value.copy(
                deletePendingIntent = pendingIntent,
                pendingRecycleBinDeleteIds = copiedIds
            )
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