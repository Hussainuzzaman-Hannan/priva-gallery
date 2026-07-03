package com.zayaanify.privagallery.data.local.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.zayaanify.privagallery.data.local.db.entity.AppLockEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppLockDao {

    @Query("SELECT * FROM app_lock_settings WHERE id = 1")
    fun observe(): Flow<AppLockEntity?>

    @Query("SELECT * FROM app_lock_settings WHERE id = 1")
    suspend fun get(): AppLockEntity?

    @Upsert
    suspend fun upsert(entity: AppLockEntity)
}
