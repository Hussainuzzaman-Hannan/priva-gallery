package com.zayaanify.privagallery.domain.usecase

import com.zayaanify.privagallery.domain.model.Album
import com.zayaanify.privagallery.domain.repository.GalleryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAlbumsUseCase @Inject constructor(
    private val repository: GalleryRepository
) {
    operator fun invoke(): Flow<List<Album>> = repository.getAlbums()
}
