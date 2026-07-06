package com.zayaanify.privagallery.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * একটা ফটোর OCR স্ক্যান করা টেক্সট ইনডেক্স।
 * MediaStore থেকে আসা ফটোর টেক্সট এখানে cache করা হয়
 * যাতে প্রতিবার সার্চে OCR না করতে হয়।
 */
@Entity(tableName = "photo_text_index")
data class PhotoTextIndexEntity(
    @PrimaryKey val mediaStoreId: Long,
    val extractedText: String,
    val indexedAt: Long = System.currentTimeMillis()
)