package com.zayaanify.privagallery.domain.usecase

import android.net.Uri
import com.zayaanify.privagallery.data.local.analysis.PerceptualHasher
import com.zayaanify.privagallery.domain.model.DuplicateGroup
import com.zayaanify.privagallery.domain.model.Photo
import com.zayaanify.privagallery.domain.repository.GalleryRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class DetectDuplicatesUseCase @Inject constructor(
    private val galleryRepository: GalleryRepository,
    private val perceptualHasher: PerceptualHasher
) {
    /**
     * সব ফটো স্ক্যান করে duplicate গ্রুপ খুঁজে বের করা।
     * onProgress — UI-তে progress দেখানোর জন্য callback (0.0 to 1.0)
     */
    suspend operator fun invoke(
        onProgress: (Float) -> Unit = {}
    ): List<DuplicateGroup> {

        val allPhotos = galleryRepository.getAlbums().first()
            .flatMap { album ->
                galleryRepository.getPhotosInAlbum(album.bucketId).first()
            }
            .filter { !it.isVideo } // ভিডিও বাদ দেওয়া হচ্ছে
            .distinctBy { it.mediaStoreId }

        if (allPhotos.isEmpty()) return emptyList()

        // প্রতিটা ফটোর hash বের করা
        val hashMap = mutableMapOf<Long, String>() // mediaStoreId → hash
        allPhotos.forEachIndexed { index, photo ->
            val uri = Uri.parse(photo.contentUri)
            val hash = perceptualHasher.computeHash(uri)
            if (hash != null) {
                hashMap[photo.mediaStoreId] = hash
            }
            onProgress((index + 1).toFloat() / allPhotos.size)
        }

        // Similar hash গুলো গ্রুপ করা
        val groups = mutableListOf<MutableList<Photo>>()
        val visited = mutableSetOf<Long>()

        for (photo in allPhotos) {
            if (photo.mediaStoreId in visited) continue
            val hash1 = hashMap[photo.mediaStoreId] ?: continue

            val similarPhotos = mutableListOf(photo)
            visited.add(photo.mediaStoreId)

            for (other in allPhotos) {
                if (other.mediaStoreId in visited) continue
                val hash2 = hashMap[other.mediaStoreId] ?: continue
                if (perceptualHasher.isSimilar(hash1, hash2)) {
                    similarPhotos.add(other)
                    visited.add(other.mediaStoreId)
                }
            }

            // একটাই থাকলে duplicate না
            if (similarPhotos.size > 1) {
                groups.add(similarPhotos)
            }
        }

        // প্রতিটা গ্রুপে সবচেয়ে বড় ফাইলটাকে "best" ধরা হচ্ছে
        return groups.map { photos ->
            val bestPhoto = photos.maxByOrNull { it.sizeBytes }!!
            DuplicateGroup(photos = photos, bestPhotoId = bestPhoto.mediaStoreId)
        }.sortedByDescending { it.wastedSpace }
    }
}