package com.zayaanify.privagallery.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * MediaStore নিজে কোনো "favorite" কনসেপ্ট রাখে না, তাই আমরা শুধু mediaStoreId
 * রেফারেন্স করে এই ছোট টেবিলে favorite ট্র্যাক করছি। ফটোর আসল ডেটা এখানে ডুপ্লিকেট হয় না।
 */
@Entity(tableName = "favorite_photos")
data class FavoritePhotoEntity(
    @PrimaryKey val mediaStoreId: Long,
    val markedAt: Long = System.currentTimeMillis()
)
