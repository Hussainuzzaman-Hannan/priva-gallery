package com.zayaanify.privagallery.domain.usecase

import com.zayaanify.privagallery.domain.model.Photo
import com.zayaanify.privagallery.domain.repository.GalleryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPhotosInAlbumUseCase @Inject constructor(
    private val repository: GalleryRepository
) {
    operator fun invoke(bucketId: String): Flow<List<Photo>> =
        repository.getPhotosInAlbum(bucketId)
}
