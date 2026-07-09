package com.zayaanify.privagallery.data.repository

import android.app.PendingIntent
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.zayaanify.privagallery.data.local.db.dao.FavoritePhotoDao
import com.zayaanify.privagallery.data.local.db.entity.FavoritePhotoEntity
import com.zayaanify.privagallery.data.local.mediastore.MediaStoreDataSource
import com.zayaanify.privagallery.domain.model.Album
import com.zayaanify.privagallery.domain.model.Photo
import com.zayaanify.privagallery.domain.repository.GalleryRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GalleryRepositoryImpl @Inject constructor(
    private val mediaStoreDataSource: MediaStoreDataSource,
    private val favoritePhotoDao: FavoritePhotoDao,
    @ApplicationContext private val context: Context
) : GalleryRepository {

    override fun getAlbums(): Flow<List<Album>> =
        mediaStoreDataSource.observeAllPhotos().map { photos ->
            photos
                .groupBy { it.bucketId }
                .map { (bucketId, bucketPhotos) ->
                    Album(
                        bucketId = bucketId,
                        displayName = bucketPhotos.first().bucketDisplayName,
                        coverPhotoUri = bucketPhotos.first().contentUri,
                        photoCount = bucketPhotos.size
                    )
                }
                .sortedByDescending { it.photoCount }
        }

    override fun getPhotosInAlbum(bucketId: String): Flow<List<Photo>> {
        return favoritePhotoDao.getAllFavoriteIds().combine(
            mediaStoreDataSource.observePhotosInBucket(bucketId)
        ) { favoriteIds, photos ->
            val favoriteSet = favoriteIds.toSet()
            photos.map { it.copy(isFavorite = it.mediaStoreId in favoriteSet) }
        }
    }

    override fun getFavoritePhotos(): Flow<List<Photo>> {
        return favoritePhotoDao.getAllFavoriteIds().combine(
            mediaStoreDataSource.observeAllPhotos()
        ) { favoriteIds, allPhotos ->
            val favoriteSet = favoriteIds.toSet()
            allPhotos.filter { it.mediaStoreId in favoriteSet }
                .map { it.copy(isFavorite = true) }
        }
    }

    override suspend fun toggleFavorite(mediaStoreId: Long) {
        withContext(Dispatchers.IO) {
            if (favoritePhotoDao.isFavorite(mediaStoreId)) {
                favoritePhotoDao.deleteById(mediaStoreId)
            } else {
                favoritePhotoDao.insert(FavoritePhotoEntity(mediaStoreId = mediaStoreId))
            }
        }
    }

    override suspend fun requestDeletePhotos(mediaStoreIds: List<Long>): PendingIntent? {
        val uris = mutableListOf<Uri>()

        mediaStoreIds.forEach { id ->
            val imageUri = ContentUris.withAppendedId(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id
            )
            val videoUri = ContentUris.withAppendedId(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id
            )

            val isImage = context.contentResolver.query(
                imageUri,
                arrayOf(MediaStore.MediaColumns._ID),
                null, null, null
            )?.use { it.count > 0 } ?: false

            if (isImage) {
                uris.add(imageUri)
            } else {
                val isVideo = context.contentResolver.query(
                    videoUri,
                    arrayOf(MediaStore.MediaColumns._ID),
                    null, null, null
                )?.use { it.count > 0 } ?: false

                if (isVideo) uris.add(videoUri)
            }
        }

        if (uris.isEmpty()) return null
        return mediaStoreDataSource.createDeletePendingIntent(uris)
    }
}