package com.zayaanify.privagallery.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zayaanify.privagallery.data.local.db.entity.VaultPhotoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultPhotoDao {

    @Query("SELECT * FROM vault_photos ORDER BY movedToVaultAt DESC")
    fun getAllVaultPhotos(): Flow<List<VaultPhotoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: VaultPhotoEntity): Long

    @Query("DELETE FROM vault_photos WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM vault_photos WHERE id = :id")
    suspend fun getById(id: Long): VaultPhotoEntity?
}