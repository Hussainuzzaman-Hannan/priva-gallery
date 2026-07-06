package com.zayaanify.privagallery.presentation.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zayaanify.privagallery.domain.model.Photo
import com.zayaanify.privagallery.domain.model.PhotoCategory
import com.zayaanify.privagallery.domain.usecase.CategoryScanUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoryUiState(
    val categories: Map<PhotoCategory, List<Photo>> = emptyMap(),
    val selectedCategory: PhotoCategory? = null,
    val isScanning: Boolean = false,
    val scanProgress: Float = 0f,
    val hasScanned: Boolean = false
)

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val categoryScanUseCase: CategoryScanUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoryUiState())
    val uiState: StateFlow<CategoryUiState> = _uiState.asStateFlow()

    fun startScan() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isScanning = true,
                scanProgress = 0f,
                categories = emptyMap(),
                selectedCategory = null,
                hasScanned = false
            )

            val result = categoryScanUseCase { progress ->
                _uiState.value = _uiState.value.copy(scanProgress = progress)
            }

            _uiState.value = _uiState.value.copy(
                isScanning = false,
                categories = result,
                hasScanned = true
            )
        }
    }

    fun selectCategory(category: PhotoCategory?) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }
}