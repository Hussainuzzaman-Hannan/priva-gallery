package com.zayaanify.privagallery.presentation.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zayaanify.privagallery.domain.model.Album
import com.zayaanify.privagallery.domain.usecase.GetAlbumsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

data class GalleryUiState(
    val albums: List<Album> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val getAlbumsUseCase: GetAlbumsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    init {
        loadAlbums()
    }

    fun loadAlbums() {
        getAlbumsUseCase()
            .onStart { _uiState.value = _uiState.value.copy(isLoading = true) }
            .onEach { albums ->
                _uiState.value = _uiState.value.copy(albums = albums, isLoading = false, errorMessage = null)
            }
            .catch { e ->
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.message)
            }
            .launchIn(viewModelScope)
    }
}
