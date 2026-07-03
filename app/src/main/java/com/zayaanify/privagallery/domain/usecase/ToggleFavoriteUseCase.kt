package com.zayaanify.privagallery.domain.usecase

import com.zayaanify.privagallery.domain.repository.GalleryRepository
import javax.inject.Inject

class ToggleFavoriteUseCase @Inject constructor(
    private val repository: GalleryRepository
) {
    suspend operator fun invoke(mediaStoreId: Long) {
        repository.toggleFavorite(mediaStoreId)
    }
}
