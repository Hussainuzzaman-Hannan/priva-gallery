package com.zayaanify.privagallery.domain.model

/**
 * একটা অ্যালবাম (MediaStore bucket) এর ডোমেইন মডেল।
 * coverPhotoUri আর photoCount UI-তে অ্যালবাম গ্রিডে দেখানোর জন্য।
 */
data class Album(
    val bucketId: String,
    val displayName: String,
    val coverPhotoUri: String,
    val photoCount: Int
)
