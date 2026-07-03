package com.zayaanify.privagallery.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.zayaanify.privagallery.data.local.db.dao.AppLockDao
import com.zayaanify.privagallery.data.local.db.dao.FavoritePhotoDao
import com.zayaanify.privagallery.data.local.db.dao.VaultPhotoDao
import com.zayaanify.privagallery.data.local.db.entity.AppLockEntity
import com.zayaanify.privagallery.data.local.db.entity.FavoritePhotoEntity
import com.zayaanify.privagallery.data.local.db.entity.VaultPhotoEntity

@Database(
    entities = [
        FavoritePhotoEntity::class,
        AppLockEntity::class,
        VaultPhotoEntity::class
    ],
    version = 2,                // ← version 1 থেকে 2 করা হয়েছে
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoritePhotoDao(): FavoritePhotoDao
    abstract fun appLockDao(): AppLockDao
    abstract fun vaultPhotoDao(): VaultPhotoDao

    companion object {
        const val DATABASE_NAME = "privagallery.db"
    }
}