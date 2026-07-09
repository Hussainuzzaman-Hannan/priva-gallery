package com.zayaanify.privagallery.data.local.mediastore

import android.app.PendingIntent
import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.zayaanify.privagallery.domain.model.Album
import com.zayaanify.privagallery.domain.model.Photo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaStoreDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
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

    /**
     * MediaStore-এর যেকোনো পরিবর্তন (ডিলিট/যোগ/আপডেট) হলে
     * অটোমেটিক নতুন ডেটা emit করবে।
     */
    fun observeAllPhotos(): Flow<List<Photo>> = callbackFlow {
        // প্রথমবার লোড
        launch { send(queryAllPhotos()) }

        // ContentObserver — MediaStore বদলালে আবার query করবে
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                launch { send(queryAllPhotos()) }
            }
        }

        context.contentResolver.registerContentObserver(imagesUri, true, observer)
        context.contentResolver.registerContentObserver(videosUri, true, observer)

        awaitClose {
            context.contentResolver.unregisterContentObserver(observer)
        }
    }.conflate() // দ্রুত পরিবর্তন হলে শুধু সর্বশেষটা নেওয়া

    fun observePhotosInBucket(bucketId: String): Flow<List<Photo>> = callbackFlow {
        launch { send(queryPhotosInBucket(bucketId)) }

        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                launch { send(queryPhotosInBucket(bucketId)) }
            }
        }

        context.contentResolver.registerContentObserver(imagesUri, true, observer)
        context.contentResolver.registerContentObserver(videosUri, true, observer)

        awaitClose {
            context.contentResolver.unregisterContentObserver(observer)
        }
    }.conflate()

    suspend fun queryAllPhotos(): List<Photo> = withContext(Dispatchers.IO) {
        val result = mutableListOf<Photo>()
        result += queryFromUri(imagesUri, isVideo = false)
        result += queryFromUri(videosUri, isVideo = true)
        result.sortedByDescending { it.dateTakenMillis }
    }

    suspend fun queryPhotosInBucket(bucketId: String): List<Photo> = withContext(Dispatchers.IO) {
        queryAllPhotos().filter { it.bucketId == bucketId }
    }

    suspend fun queryAlbums(): List<Album> = withContext(Dispatchers.IO) {
        queryAllPhotos()
            .groupBy { it.bucketId }
            .map { (bucketId, photos) ->
                Album(
                    bucketId = bucketId,
                    displayName = photos.first().bucketDisplayName,
                    coverPhotoUri = photos.first().contentUri,
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
                val dateTaken = cursor.getLong(dateTakenCol)
                val dateAdded = cursor.getLong(dateAddedCol) * 1000L
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

    fun createDeletePendingIntent(uris: List<android.net.Uri>): PendingIntent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            MediaStore.createDeleteRequest(context.contentResolver, uris)
        } else null
    }
}