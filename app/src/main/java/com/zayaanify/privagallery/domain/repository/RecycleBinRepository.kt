package com.zayaanify.privagallery.domain.repository

import com.zayaanify.privagallery.domain.model.RecycleBinPhoto
import kotlinx.coroutines.flow.Flow

interface RecycleBinRepository {
    fun observeAll(): Flow<List<RecycleBinPhoto>>

    /** ফাইল Recycle Bin-এ copy করা — MediaStore থেকে delete আলাদাভাবে হবে। */
    suspend fun copyToRecycleBin(mediaStoreId: Long): Result<Unit>

    suspend fun restore(recycleBinId: Long): Result<Unit>
    suspend fun deletePermanently(recycleBinId: Long): Result<Unit>
    suspend fun emptyRecycleBin(): Result<Unit>
    suspend fun deleteExpiredItems()
}