package com.zayaanify.privagallery.presentation.duplicate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zayaanify.privagallery.domain.model.DuplicateGroup
import com.zayaanify.privagallery.domain.usecase.DeletePhotosUseCase
import com.zayaanify.privagallery.domain.usecase.DetectDuplicatesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DuplicateUiState(
    val groups: List<DuplicateGroup> = emptyList(),
    val isScanning: Boolean = false,
    val scanProgress: Float = 0f,
    val isDeleting: Boolean = false,
    val message: String? = null,
    val hasScanned: Boolean = false
)

@HiltViewModel
class DuplicateViewModel @Inject constructor(
    private val detectDuplicatesUseCase: DetectDuplicatesUseCase,
    private val deletePhotosUseCase: DeletePhotosUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DuplicateUiState())
    val uiState: StateFlow<DuplicateUiState> = _uiState.asStateFlow()

    fun startScan() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isScanning = true,
                scanProgress = 0f,
                groups = emptyList(),
                hasScanned = false
            )

            val groups = detectDuplicatesUseCase { progress ->
                _uiState.value = _uiState.value.copy(scanProgress = progress)
            }

            _uiState.value = _uiState.value.copy(
                isScanning = false,
                groups = groups,
                hasScanned = true,
                message = if (groups.isEmpty()) "কোনো duplicate পাওয়া যায়নি"
                else "${groups.size} টা duplicate গ্রুপ পাওয়া গেছে"
            )
        }
    }

    /** একটা গ্রুপের সব duplicate মুছে ফেলা — best ফটোটা রেখে দেওয়া হয়। */
    fun deleteGroupDuplicates(group: DuplicateGroup) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeleting = true)

            val toDelete = group.photos
                .filter { it.mediaStoreId != group.bestPhotoId }
                .map { it.mediaStoreId }

            deletePhotosUseCase(toDelete)

            // গ্রুপ লিস্ট থেকে এই গ্রুপ সরানো
            val updatedGroups = _uiState.value.groups - group
            _uiState.value = _uiState.value.copy(
                isDeleting = false,
                groups = updatedGroups,
                message = "${toDelete.size} টা duplicate মুছে ফেলা হয়েছে"
            )
        }
    }

    /** সব গ্রুপের সব duplicate একসাথে মুছে ফেলা। */
    fun deleteAllDuplicates() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeleting = true)

            val toDelete = _uiState.value.groups.flatMap { group ->
                group.photos
                    .filter { it.mediaStoreId != group.bestPhotoId }
                    .map { it.mediaStoreId }
            }

            deletePhotosUseCase(toDelete)

            val savedBytes = _uiState.value.groups.sumOf { it.wastedSpace }
            _uiState.value = _uiState.value.copy(
                isDeleting = false,
                groups = emptyList(),
                message = "${toDelete.size} টা ফটো মুছে ${formatBytes(savedBytes)} স্পেস বাঁচানো হয়েছে"
            )
        }
    }

    fun consumeMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1_000_000_000 -> "%.1f GB".format(bytes / 1_000_000_000.0)
            bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000.0)
            bytes >= 1_000 -> "%.1f KB".format(bytes / 1_000.0)
            else -> "$bytes B"
        }
    }
}