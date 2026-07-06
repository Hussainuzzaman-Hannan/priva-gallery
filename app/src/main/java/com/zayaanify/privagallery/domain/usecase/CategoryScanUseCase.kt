package com.zayaanify.privagallery.domain.usecase

import android.net.Uri
import com.zayaanify.privagallery.data.local.analysis.PhotoCategorizer
import com.zayaanify.privagallery.domain.model.Photo
import com.zayaanify.privagallery.domain.model.PhotoCategory
import com.zayaanify.privagallery.domain.repository.GalleryRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

data class CategorizedPhoto(
    val photo: Photo,
    val category: PhotoCategory
)

class CategoryScanUseCase @Inject constructor(
    private val galleryRepository: GalleryRepository,
    private val photoCategorizer: PhotoCategorizer
) {
    suspend operator fun invoke(
        onProgress: (Float) -> Unit = {}
    ): Map<PhotoCategory, List<Photo>> {

        val allPhotos = galleryRepository.getAlbums().first()
            .flatMap { album ->
                galleryRepository.getPhotosInAlbum(album.bucketId).first()
            }
            .filter { !it.isVideo }
            .distinctBy { it.mediaStoreId }

        if (allPhotos.isEmpty()) return emptyMap()

        val result = mutableMapOf<PhotoCategory, MutableList<Photo>>()
        PhotoCategory.entries.forEach { result[it] = mutableListOf() }

        allPhotos.forEachIndexed { index, photo ->
            val uri = Uri.parse(photo.contentUri)
            val category = photoCategorizer.categorize(uri)
            result[category]?.add(photo)
            onProgress((index + 1).toFloat() / allPhotos.size)
        }

        // খালি ক্যাটেগরি বাদ দেওয়া
        return result.filter { it.value.isNotEmpty() }
    }
}