package com.zayaanify.privagallery.presentation.ocrsearch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zayaanify.privagallery.domain.model.Photo
import com.zayaanify.privagallery.domain.usecase.OcrSearchUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OcrSearchUiState(
    val query: String = "",
    val results: List<Photo> = emptyList(),
    val isIndexing: Boolean = false,
    val isSearching: Boolean = false,
    val indexProgress: Float = 0f,
    val indexedCount: Int = 0,
    val hasIndexed: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class OcrSearchViewModel @Inject constructor(
    private val ocrSearchUseCase: OcrSearchUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(OcrSearchUiState())
    val uiState: StateFlow<OcrSearchUiState> = _uiState.asStateFlow()

    private val queryFlow = MutableStateFlow("")

    init {
        checkIndexStatus()
        observeQuery()
    }

    private fun checkIndexStatus() {
        viewModelScope.launch {
            val count = ocrSearchUseCase.getIndexedCount()
            _uiState.value = _uiState.value.copy(
                indexedCount = count,
                hasIndexed = count > 0
            )
        }
    }

    @OptIn(FlowPreview::class)
    private fun observeQuery() {
        queryFlow
            .debounce(400L)
            .distinctUntilChanged()
            .filter { it.length >= 2 }
            .onEach { query ->
                _uiState.value = _uiState.value.copy(isSearching = true)
                val results = ocrSearchUseCase.search(query)
                _uiState.value = _uiState.value.copy(
                    results = results,
                    isSearching = false
                )
            }
            .launchIn(viewModelScope)
    }

    fun onQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        queryFlow.value = query
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(results = emptyList())
        }
    }

    fun buildIndex() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isIndexing = true,
                indexProgress = 0f
            )
            ocrSearchUseCase.buildIndex { progress ->
                _uiState.value = _uiState.value.copy(indexProgress = progress)
            }
            val count = ocrSearchUseCase.getIndexedCount()
            _uiState.value = _uiState.value.copy(
                isIndexing = false,
                hasIndexed = true,
                indexedCount = count,
                message = "$count টা স্ক্রিনশট index করা হয়েছে"
            )
        }
    }

    fun clearIndex() {
        viewModelScope.launch {
            ocrSearchUseCase.clearIndex()
            _uiState.value = _uiState.value.copy(
                hasIndexed = false,
                indexedCount = 0,
                results = emptyList(),
                query = "",
                message = "Index মুছে ফেলা হয়েছে"
            )
        }
    }

    fun consumeMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}