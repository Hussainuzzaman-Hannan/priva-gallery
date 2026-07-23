package com.zayaanify.privagallery.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Recycle Bin-এ রাখা ফটোর মেটাডেটা।
 * আসল ফাইল app-private storage-এ কপি করে রাখা হয়।
 * 30 দিন পর অটো-Delete হবে।
 */
@Entity(tableName = "recycle_bin")
data class RecycleBinEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val originalMediaStoreId: Long,
    val filePath: String,           // app-private storage-এ কপি করা ফাইলের পাথ
    val thumbnailPath: String,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val dateTakenMillis: Long,
    val deletedAt: Long = System.currentTimeMillis()
)