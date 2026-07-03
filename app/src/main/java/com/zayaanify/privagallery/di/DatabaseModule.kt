package com.zayaanify.privagallery.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.zayaanify.privagallery.data.local.db.AppDatabase
import com.zayaanify.privagallery.data.local.db.dao.AppLockDao
import com.zayaanify.privagallery.data.local.db.dao.FavoritePhotoDao
import com.zayaanify.privagallery.data.local.db.dao.VaultPhotoDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// Version 1 → 2: vault_photos টেবিল যোগ হয়েছে
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
            .addMigrations(MIGRATION_1_2)
            .build()
    }

    @Provides
    fun provideFavoritePhotoDao(database: AppDatabase): FavoritePhotoDao {
        return database.favoritePhotoDao()
    }

    @Provides
    fun provideAppLockDao(database: AppDatabase): AppLockDao {
        return database.appLockDao()
    }

    @Provides
    fun provideVaultPhotoDao(database: AppDatabase): VaultPhotoDao {
        return database.vaultPhotoDao()
    }
}