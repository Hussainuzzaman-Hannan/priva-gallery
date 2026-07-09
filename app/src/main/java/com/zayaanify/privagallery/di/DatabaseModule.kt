package com.zayaanify.privagallery.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.zayaanify.privagallery.data.local.db.AppDatabase
import com.zayaanify.privagallery.data.local.db.dao.AppLockDao
import com.zayaanify.privagallery.data.local.db.dao.FavoritePhotoDao
import com.zayaanify.privagallery.data.local.db.dao.PhotoTextIndexDao
import com.zayaanify.privagallery.data.local.db.dao.RecycleBinDao
import com.zayaanify.privagallery.data.local.db.dao.VaultPhotoDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS vault_photos (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                encryptedFilePath TEXT NOT NULL,
                encryptedThumbPath TEXT NOT NULL,
                ivBase64 TEXT NOT NULL,
                originalFileName TEXT NOT NULL,
                mimeType TEXT NOT NULL,
                sizeBytes INTEGER NOT NULL,
                dateTakenMillis INTEGER NOT NULL,
                movedToVaultAt INTEGER NOT NULL
            )
        """.trimIndent())
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS photo_text_index (
                mediaStoreId INTEGER PRIMARY KEY NOT NULL,
                extractedText TEXT NOT NULL,
                indexedAt INTEGER NOT NULL
            )
        """.trimIndent())
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE app_lock_settings ADD COLUMN biometricEnabled INTEGER NOT NULL DEFAULT 0"
        )
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS recycle_bin (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                originalMediaStoreId INTEGER NOT NULL,
                filePath TEXT NOT NULL,
                thumbnailPath TEXT NOT NULL,
                displayName TEXT NOT NULL,
                mimeType TEXT NOT NULL,
                sizeBytes INTEGER NOT NULL,
                dateTakenMillis INTEGER NOT NULL,
                deletedAt INTEGER NOT NULL
            )
        """.trimIndent())
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .build()
    }

    @Provides
    fun provideFavoritePhotoDao(database: AppDatabase): FavoritePhotoDao =
        database.favoritePhotoDao()

    @Provides
    fun provideAppLockDao(database: AppDatabase): AppLockDao =
        database.appLockDao()

    @Provides
    fun provideVaultPhotoDao(database: AppDatabase): VaultPhotoDao =
        database.vaultPhotoDao()

    @Provides
    fun providePhotoTextIndexDao(database: AppDatabase): PhotoTextIndexDao =
        database.photoTextIndexDao()

    @Provides
    fun provideRecycleBinDao(database: AppDatabase): RecycleBinDao =
        database.recycleBinDao()
}