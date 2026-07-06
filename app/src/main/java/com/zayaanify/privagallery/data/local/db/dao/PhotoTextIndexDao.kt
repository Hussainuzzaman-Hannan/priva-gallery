package com.zayaanify.privagallery.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zayaanify.privagallery.data.local.db.entity.PhotoTextIndexEntity

@Dao
interface PhotoTextIndexDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PhotoTextIndexEntity)

    @Query("SELECT mediaStoreId FROM photo_text_index WHERE extractedText LIKE '%' || :query || '%'")
    suspend fun search(query: String): List<Long>

    @Query("SELECT EXISTS(SELECT 1 FROM photo_text_index WHERE mediaStoreId = :mediaStoreId)")
    suspend fun isIndexed(mediaStoreId: Long): Boolean

    @Query("SELECT COUNT(*) FROM photo_text_index")
    suspend fun getIndexedCount(): Int

    @Query("DELETE FROM photo_text_index")
    suspend fun clearAll()
}