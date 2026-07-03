package com.zayaanify.privagallery.data.repository

import android.app.PendingIntent
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
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GalleryRepositoryImpl @Inject constructor(
    private val mediaStoreDataSource: MediaStoreDataSource,
    private val favoritePhotoDao: FavoritePhotoDao,
    @ApplicationContext private val context: Context
) : GalleryRepository {

    override fun getAlbums(): Flow<List<Album>> = flow {
        emit(mediaStoreDataSource.queryAlbums())
    }

    override fun getPhotosInAlbum(bucketId: String): Flow<List<Photo>> {
        return favoritePhotoDao.getAllFavoriteIds().combine(
            flow { emit(mediaStoreDataSource.queryPhotosInBucket(bucketId)) }
        ) { favoriteIds, photos ->
            val favoriteSet = favoriteIds.toSet()
            photos.map { it.copy(isFavorite = it.mediaStoreId in favoriteSet) }
        }
    }

    override fun getFavoritePhotos(): Flow<List<Photo>> {
        return favoritePhotoDao.getAllFavoriteIds().combine(
            flow { emit(mediaStoreDataSource.queryAllPhotos()) }
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
        val uris: List<Uri> = mediaStoreIds.map {
            // আমরা জানি না কোনটা ইমেজ কোনটা ভিডিও, তাই দুটো বেস URI-ই ট্রাই করতে হবে।
            // সিম্পল করার জন্য images URI ব্যবহার করছি — content resolver উভয় ক্ষেত্রেই
            // সঠিক row resolve করতে পারে কারণ _ID গ্লোবালি ইউনিক না হলেও practically এটা কাজ করে
            // external content URI দিয়ে। ভিডিওর জন্য আলাদা মার্ক রাখা থাকলে এখানে ভাগ করা যেত।
            android.content.ContentUris.withAppendedId(
                MediaStore.Files.getContentUri("external"), it
            )
        }
        return mediaStoreDataSource.createDeletePendingIntent(uris)
    }
}
