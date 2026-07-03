package com.zayaanify.privagallery.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zayaanify.privagallery.data.local.db.entity.FavoritePhotoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoritePhotoDao {

    @Query("SELECT mediaStoreId FROM favorite_photos")
    fun getAllFavoriteIds(): Flow<List<Long>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_photos WHERE mediaStoreId = :mediaStoreId)")
    suspend fun isFavorite(mediaStoreId: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: FavoritePhotoEntity)

    @Query("DELETE FROM favorite_photos WHERE mediaStoreId = :mediaStoreId")
    suspend fun deleteById(mediaStoreId: Long)
}
