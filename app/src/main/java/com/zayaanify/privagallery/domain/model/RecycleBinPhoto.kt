package com.zayaanify.privagallery.domain.model

data class RecycleBinPhoto(
    val id: Long,
    val originalMediaStoreId: Long,
    val filePath: String,
    val thumbnailPath: String,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val dateTakenMillis: Long,
    val deletedAt: Long
)