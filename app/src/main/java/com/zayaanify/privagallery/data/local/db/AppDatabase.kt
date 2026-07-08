package com.zayaanify.privagallery.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.zayaanify.privagallery.data.local.db.dao.AppLockDao
import com.zayaanify.privagallery.data.local.db.dao.FavoritePhotoDao
import com.zayaanify.privagallery.data.local.db.dao.PhotoTextIndexDao
import com.zayaanify.privagallery.data.local.db.dao.VaultPhotoDao
import com.zayaanify.privagallery.data.local.db.entity.AppLockEntity
import com.zayaanify.privagallery.data.local.db.entity.FavoritePhotoEntity
import com.zayaanify.privagallery.data.local.db.entity.PhotoTextIndexEntity
import com.zayaanify.privagallery.data.local.db.entity.VaultPhotoEntity

@Database(
    entities = [
        FavoritePhotoEntity::class,
        AppLockEntity::class,
        VaultPhotoEntity::class,
        PhotoTextIndexEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoritePhotoDao(): FavoritePhotoDao
    abstract fun appLockDao(): AppLockDao
    abstract fun vaultPhotoDao(): VaultPhotoDao
    abstract fun photoTextIndexDao(): PhotoTextIndexDao

    companion object {
        const val DATABASE_NAME = "privagallery.db"
    }
}