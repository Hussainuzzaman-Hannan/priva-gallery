package com.zayaanify.privagallery.presentation.recyclebin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zayaanify.privagallery.domain.model.RecycleBinPhoto
import com.zayaanify.privagallery.domain.repository.RecycleBinRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecycleBinUiState(
    val photos: List<RecycleBinPhoto> = emptyList(),
    val selectedIds: Set<Long> = emptySet(),
    val isSelectionMode: Boolean = false,
    val isLoading: Boolean = true,
    val isProcessing: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class RecycleBinViewModel @Inject constructor(
    private val recycleBinRepository: RecycleBinRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecycleBinUiState())
    val uiState: StateFlow<RecycleBinUiState> = _uiState.asStateFlow()

    init {
        recycleBinRepository.observeAll()
            .onEach { photos ->
                _uiState.value = _uiState.value.copy(
                    photos = photos,
                    isLoading = false
                )
            }
            .launchIn(viewModelScope)

        // মেয়াদ শেষ Item পরিষ্কার করা
        viewModelScope.launch {
            recycleBinRepository.deleteExpiredItems()
        }
    }

    fun onPhotoLongPress(id: Long) {
        val newSelected = _uiState.value.selectedIds + id
        _uiState.value = _uiState.value.copy(
            selectedIds = newSelected,
            isSelectionMode = true
        )
    }

    fun onPhotoTap(id: Long) {
        val current = _uiState.value
        if (!current.isSelectionMode) return
        val newSelected = if (id in current.selectedIds) {
            current.selectedIds - id
        } else {
            current.selectedIds + id
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

    fun restoreSelected() {
        viewModelScope.launch {
            val ids = _uiState.value.selectedIds.toList()
            _uiState.value = _uiState.value.copy(isProcessing = true)
            var count = 0
            ids.forEach { id ->
                recycleBinRepository.restore(id).onSuccess { count++ }
            }
            _uiState.value = _uiState.value.copy(
                isProcessing = false,
                isSelectionMode = false,
                selectedIds = emptySet(),
                message = "$count টা ফটো restore করা হয়েছে"
            )
        }
    }

    fun deleteSelectedPermanently() {
        viewModelScope.launch {
            val ids = _uiState.value.selectedIds.toList()
            _uiState.value = _uiState.value.copy(isProcessing = true)
            var count = 0
            ids.forEach { id ->
                recycleBinRepository.deletePermanently(id).onSuccess { count++ }
            }
            _uiState.value = _uiState.value.copy(
                isProcessing = false,
                isSelectionMode = false,
                selectedIds = emptySet(),
                message = "$count টা ফটো স্থায়ীভাবে মুছে ফেলা হয়েছে"
            )
        }
    }

    fun emptyRecycleBin() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true)
            recycleBinRepository.emptyRecycleBin()
            _uiState.value = _uiState.value.copy(
                isProcessing = false,
                message = "Recycle Bin খালি করা হয়েছে"
            )
        }
    }

    fun consumeMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}