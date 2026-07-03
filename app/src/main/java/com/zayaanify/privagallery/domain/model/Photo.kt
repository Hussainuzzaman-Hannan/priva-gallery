package com.zayaanify.privagallery.domain.model

/**
 * একটা ফটো/ভিডিওর ডোমেইন মডেল।
 * mediaStoreId, contentUri — MediaStore থেকে আসা; isFavorite — আমাদের নিজের Room DB থেকে আসা।
 */
data class Photo(
    val mediaStoreId: Long,
    val contentUri: String,      // "content://media/external/images/media/123"
    val displayName: String,
    val mimeType: String,
    val isVideo: Boolean,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val dateTakenMillis: Long,   // EXIF date_taken; না পেলে date_added
    val bucketId: String,        // MediaStore bucket id (album)
    val bucketDisplayName: String,
    val isFavorite: Boolean = false
)
