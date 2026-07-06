package com.zayaanify.privagallery.domain.model

/**
 * Similar/duplicate ফটোর একটা গ্রুপ।
 * photos — visually similar ফটোগুলো
 * bestPhotoId — সবচেয়ে ভালো মানের ফটোর mediaStoreId
 * (সবচেয়ে বড় file size = সবচেয়ে কম compressed ধরা হচ্ছে)
 */
data class DuplicateGroup(
    val photos: List<Photo>,
    val bestPhotoId: Long
) {
    val duplicateCount: Int get() = photos.size - 1
    val wastedSpace: Long get() = photos
        .filter { it.mediaStoreId != bestPhotoId }
        .sumOf { it.sizeBytes }
}