package com.zayaanify.privagallery.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zayaanify.privagallery.data.local.db.entity.RecycleBinEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecycleBinDao {

    @Query("SELECT * FROM recycle_bin ORDER BY deletedAt DESC")
    fun observeAll(): Flow<List<RecycleBinEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: RecycleBinEntity): Long

    @Query("SELECT * FROM recycle_bin WHERE id = :id")
    suspend fun getById(id: Long): RecycleBinEntity?

    @Query("DELETE FROM recycle_bin WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM recycle_bin")
    suspend fun deleteAll()

    // 30 দিনের বেশি পুরোনো Item
    @Query("SELECT * FROM recycle_bin WHERE deletedAt < :cutoffTime")
    suspend fun getExpiredItems(cutoffTime: Long): List<RecycleBinEntity>
}