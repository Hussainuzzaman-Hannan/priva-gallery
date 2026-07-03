package com.zayaanify.privagallery.presentation.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zayaanify.privagallery.domain.model.VaultPhoto
import com.zayaanify.privagallery.domain.repository.VaultRepository
import com.zayaanify.privagallery.domain.usecase.DeleteFromVaultUseCase
import com.zayaanify.privagallery.domain.usecase.RestoreFromVaultUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VaultUiState(
    val photos: List<VaultPhoto> = emptyList(),
    val selectedIds: Set<Long> = emptySet(),
    val isSelectionMode: Boolean = false,
    val isLoading: Boolean = true,
    val isProcessing: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class VaultViewModel @Inject constructor(
    private val vaultRepository: VaultRepository,
    private val restoreFromVaultUseCase: RestoreFromVaultUseCase,
    private val deleteFromVaultUseCase: DeleteFromVaultUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(VaultUiState())
    val uiState: StateFlow<VaultUiState> = _uiState.asStateFlow()

    init {
        vaultRepository.getAllVaultPhotos()
            .onEach { photos ->
                _uiState.value = _uiState.value.copy(
                    photos = photos,
                    isLoading = false
                )
            }
            .launchIn(viewModelScope)
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
            var successCount = 0
            ids.forEach { id ->
                restoreFromVaultUseCase(id).onSuccess { successCount++ }
            }
            _uiState.value = _uiState.value.copy(
                isProcessing = false,
                isSelectionMode = false,
                selectedIds = emptySet(),
                message = "$successCount টা ফটো গ্যালারিতে ফিরিয়ে দেওয়া হয়েছে"
            )
        }
    }

    fun deleteSelected() {
        viewModelScope.launch {
            val ids = _uiState.value.selectedIds.toList()
            _uiState.value = _uiState.value.copy(isProcessing = true)
            var successCount = 0
            ids.forEach { id ->
                deleteFromVaultUseCase(id).onSuccess { successCount++ }
            }
            _uiState.value = _uiState.value.copy(
                isProcessing = false,
                isSelectionMode = false,
                selectedIds = emptySet(),
                message = "$successCount টা ফটো স্থায়ীভাবে মুছে ফেলা হয়েছে"
            )
        }
    }

    fun consumeMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}