package com.zayaanify.privagallery.domain.repository

import com.zayaanify.privagallery.domain.model.Album
import com.zayaanify.privagallery.domain.model.Photo
import kotlinx.coroutines.flow.Flow

interface GalleryRepository {

    /** ফোনের সব অ্যালবাম (MediaStore bucket) লিস্ট, প্রতিটার কভার ফটো সহ। */
    fun getAlbums(): Flow<List<Album>>

    /** একটা নির্দিষ্ট অ্যালবামের ভেতরের সব ফটো/ভিডিও, নতুন থেকে পুরোনো সাজানো। */
    fun getPhotosInAlbum(bucketId: String): Flow<List<Photo>>

    /** সব ফটোর মধ্যে যেগুলো favorite মার্ক করা আছে। */
    fun getFavoritePhotos(): Flow<List<Photo>>

    /** একটা ফটোর favorite স্ট্যাটাস টগল করা — শুধু আমাদের Room DB-তে রাখা হয়, MediaStore বদলায় না। */
    suspend fun toggleFavorite(mediaStoreId: Long)

    /**
     * একগুচ্ছ ফটো ডিলিট করার রিকোয়েস্ট। Android 11+ এ সরাসরি ডিলিট করা যায় না —
     * এটা একটা IntentSender রিটার্ন করে যা UI লেয়ার থেকে launch করতে হয় (system confirmation dialog)।
     */
    suspend fun requestDeletePhotos(mediaStoreIds: List<Long>): android.app.PendingIntent?
}
