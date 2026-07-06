package com.zayaanify.privagallery.domain.usecase

import android.net.Uri
import com.zayaanify.privagallery.data.local.analysis.OcrScanner
import com.zayaanify.privagallery.data.local.db.dao.PhotoTextIndexDao
import com.zayaanify.privagallery.data.local.db.entity.PhotoTextIndexEntity
import com.zayaanify.privagallery.domain.model.Photo
import com.zayaanify.privagallery.domain.repository.GalleryRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class OcrSearchUseCase @Inject constructor(
    private val galleryRepository: GalleryRepository,
    private val ocrScanner: OcrScanner,
    private val photoTextIndexDao: PhotoTextIndexDao
) {
    /**
     * সব স্ক্রিনশট স্ক্যান করে Room-এ index করা।
     * একবার index হলে পরবর্তীতে আর OCR করতে হয় না।
     */
    suspend fun buildIndex(onProgress: (Float) -> Unit = {}) {
        val allPhotos = galleryRepository.getAlbums().first()
            .flatMap { album ->
                galleryRepository.getPhotosInAlbum(album.bucketId).first()
            }
            .filter { !it.isVideo }
            .filter {
                // শুধু স্ক্রিনশট ফোল্ডারের ফটো index করা
                it.bucketDisplayName.lowercase().contains("screenshot") ||
                        it.displayName.lowercase().contains("screenshot")
            }
            .distinctBy { it.mediaStoreId }

        if (allPhotos.isEmpty()) return

        allPhotos.forEachIndexed { index, photo ->
            // আগে index হয়ে থাকলে skip করা
            if (!photoTextIndexDao.isIndexed(photo.mediaStoreId)) {
                val uri = Uri.parse(photo.contentUri)
                val text = ocrScanner.extractText(uri) ?: ""
                if (text.isNotBlank()) {
                    photoTextIndexDao.insert(
                        PhotoTextIndexEntity(
                            mediaStoreId = photo.mediaStoreId,
                            extractedText = text
                        )
                    )
                }
            }
            onProgress((index + 1).toFloat() / allPhotos.size)
        }
    }

    /**
     * Index করা টেক্সটে query সার্চ করা।
     * Room-এর LIKE query ব্যবহার করা হচ্ছে।
     */
    suspend fun search(query: String): List<Photo> {
        if (query.isBlank()) return emptyList()

        val matchedIds = photoTextIndexDao.search(query).toSet()
        if (matchedIds.isEmpty()) return emptyList()

        return galleryRepository.getAlbums().first()
            .flatMap { album ->
                galleryRepository.getPhotosInAlbum(album.bucketId).first()
            }
            .filter { it.mediaStoreId in matchedIds }
            .distinctBy { it.mediaStoreId }
    }

    suspend fun getIndexedCount(): Int = photoTextIndexDao.getIndexedCount()

    suspend fun clearIndex() = photoTextIndexDao.clearAll()
}