package com.zayaanify.privagallery.di

import com.zayaanify.privagallery.data.repository.AppLockRepositoryImpl
import com.zayaanify.privagallery.data.repository.BackupRepositoryImpl
import com.zayaanify.privagallery.data.repository.GalleryRepositoryImpl
import com.zayaanify.privagallery.data.repository.RecycleBinRepositoryImpl
import com.zayaanify.privagallery.data.repository.VaultRepositoryImpl
import com.zayaanify.privagallery.domain.repository.AppLockRepository
import com.zayaanify.privagallery.domain.repository.BackupRepository
import com.zayaanify.privagallery.domain.repository.GalleryRepository
import com.zayaanify.privagallery.domain.repository.RecycleBinRepository
import com.zayaanify.privagallery.domain.repository.VaultRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindGalleryRepository(impl: GalleryRepositoryImpl): GalleryRepository

    @Binds @Singleton
    abstract fun bindAppLockRepository(impl: AppLockRepositoryImpl): AppLockRepository

    @Binds @Singleton
    abstract fun bindVaultRepository(impl: VaultRepositoryImpl): VaultRepository

    @Binds @Singleton
    abstract fun bindBackupRepository(impl: BackupRepositoryImpl): BackupRepository

    @Binds @Singleton
    abstract fun bindRecycleBinRepository(impl: RecycleBinRepositoryImpl): RecycleBinRepository
}