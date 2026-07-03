package com.zayaanify.privagallery.domain.usecase

import android.app.PendingIntent
import com.zayaanify.privagallery.domain.repository.GalleryRepository
import javax.inject.Inject

class DeletePhotosUseCase @Inject constructor(
    private val repository: GalleryRepository
) {
    suspend operator fun invoke(mediaStoreIds: List<Long>): PendingIntent? {
        return repository.requestDeletePhotos(mediaStoreIds)
    }
}
