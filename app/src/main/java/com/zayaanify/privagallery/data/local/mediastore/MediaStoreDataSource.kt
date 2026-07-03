package com.zayaanify.privagallery.data.local.mediastore

import android.app.PendingIntent
import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import com.zayaanify.privagallery.domain.model.Album
import com.zayaanify.privagallery.domain.model.Photo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android-এর MediaStore থেকে সরাসরি ফটো/ভিডিও স্ক্যান করার লেয়ার।
 * এখানে কোনো ফাইল কপি বা মুভ হয় না — শুধু read-only কোয়েরি।
 *
 * Browse Mode পুরোপুরি এই ক্লাসের উপর নির্ভরশীল।
 */
@Singleton
class MediaStoreDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {

    // ছবি ও ভিডিও দুটোই দরকার — তাই দুটো collection থেকেই কোয়েরি করতে হবে
    private val imagesUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    private val videosUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI

    private val projection = arrayOf(
        MediaStore.MediaColumns._ID,
        MediaStore.MediaColumns.DISPLAY_NAME,
        MediaStore.MediaColumns.MIME_TYPE,
        MediaStore.MediaColumns.SIZE,
        MediaStore.MediaColumns.WIDTH,
        MediaStore.MediaColumns.HEIGHT,
        MediaStore.MediaColumns.DATE_ADDED,
        MediaStore.MediaColumns.DATE_TAKEN,
        MediaStore.MediaColumns.BUCKET_ID,
        MediaStore.MediaColumns.BUCKET_DISPLAY_NAME
    )

    /** ফোনের সব ছবি ও ভিডিও, নতুন থেকে পুরোনো ক্রমে — date_added দিয়ে সাজানো। */
    suspend fun queryAllPhotos(): List<Photo> = withContext(Dispatchers.IO) {
        val result = mutableListOf<Photo>()
        result += queryFromUri(imagesUri, isVideo = false)
        result += queryFromUri(videosUri, isVideo = true)
        result.sortedByDescending { it.dateTakenMillis }
    }

    /** একটা নির্দিষ্ট bucket (অ্যালবাম)-এর সব ছবি/ভিডিও। */
    suspend fun queryPhotosInBucket(bucketId: String): List<Photo> = withContext(Dispatchers.IO) {
        queryAllPhotos().filter { it.bucketId == bucketId }
    }

    /** bucketId অনুযায়ী গ্রুপ করে অ্যালবাম লিস্ট বানানো — প্রতিটার কভার ও কাউন্ট সহ। */
    suspend fun queryAlbums(): List<Album> = withContext(Dispatchers.IO) {
        queryAllPhotos()
            .groupBy { it.bucketId }
            .map { (bucketId, photos) ->
                Album(
                    bucketId = bucketId,
                    displayName = photos.first().bucketDisplayName,
                    coverPhotoUri = photos.first().contentUri, // সবচেয়ে নতুন ফটোই কভার
                    photoCount = photos.size
                )
            }
            .sortedByDescending { it.photoCount }
    }

    private fun queryFromUri(uri: android.net.Uri, isVideo: Boolean): List<Photo> {
        val photos = mutableListOf<Photo>()
        val sortOrder = "${MediaStore.MediaColumns.DATE_ADDED} DESC"

        context.contentResolver.query(uri, projection, null, null, sortOrder)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val widthCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.WIDTH)
            val heightCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.HEIGHT)
            val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            val dateTakenCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_TAKEN)
            val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_ID)
            val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val contentUri = ContentUris.withAppendedId(uri, id)

                // date_taken অনেক সময় null থাকে (বিশেষত স্ক্রিনশটে), তখন date_added ব্যবহার করা হচ্ছে
                val dateTaken = cursor.getLong(dateTakenCol)
                val dateAdded = cursor.getLong(dateAddedCol) * 1000L // seconds -> millis
                val effectiveDate = if (dateTaken > 0) dateTaken else dateAdded

                photos += Photo(
                    mediaStoreId = id,
                    contentUri = contentUri.toString(),
                    displayName = cursor.getString(nameCol) ?: "",
                    mimeType = cursor.getString(mimeCol) ?: "",
                    isVideo = isVideo,
                    sizeBytes = cursor.getLong(sizeCol),
                    width = cursor.getInt(widthCol),
                    height = cursor.getInt(heightCol),
                    dateTakenMillis = effectiveDate,
                    bucketId = cursor.getString(bucketIdCol) ?: "unknown",
                    bucketDisplayName = cursor.getString(bucketNameCol) ?: "Unknown"
                )
            }
        }
        return photos
    }

    /**
     * Android 11+ (API 30+) এ অ্যাপ নিজে অন্য অ্যাপের তোলা ফাইল সরাসরি ডিলিট করতে পারে না —
     * MediaStore.createDeleteRequest() একটা PendingIntent দেয়, যা UI থেকে launch করলে
     * সিস্টেম একটা কনফার্মেশন ডায়ালগ দেখায়। পুরনো ভার্সনে নাল রিটার্ন করি, কারণ সেখানে
     * ContentResolver.delete() সরাসরি কল করা যায় (এটা রিপোজিটরি লেয়ারে হ্যান্ডল হবে)।
     */
    fun createDeletePendingIntent(uris: List<android.net.Uri>): PendingIntent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            MediaStore.createDeleteRequest(context.contentResolver, uris)
        } else {
            null
        }
    }
}
