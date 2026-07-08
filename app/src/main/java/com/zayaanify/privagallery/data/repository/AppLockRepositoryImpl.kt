package com.zayaanify.privagallery.data.repository

import com.zayaanify.privagallery.data.local.crypto.PinHasher
import com.zayaanify.privagallery.data.local.db.dao.AppLockDao
import com.zayaanify.privagallery.data.local.db.entity.AppLockEntity
import com.zayaanify.privagallery.domain.repository.AppLockRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLockRepositoryImpl @Inject constructor(
    private val appLockDao: AppLockDao,
    private val pinHasher: PinHasher
) : AppLockRepository {

    override fun isLockEnabled(): Flow<Boolean> {
        return appLockDao.observe().map { it?.isLockEnabled == true }
    }

    override fun isBiometricEnabled(): Flow<Boolean> {
        return appLockDao.observe().map { it?.biometricEnabled == true }
    }

    override suspend fun setPin(pin: String) {
        val salt = pinHasher.generateSalt()
        val hash = pinHasher.hash(pin, salt)
        val current = appLockDao.get()
        appLockDao.upsert(
            AppLockEntity(
                id = 1,
                pinHash = hash,
                salt = salt,
                isLockEnabled = true,
                biometricEnabled = current?.biometricEnabled ?: false
            )
        )
    }

    override suspend fun verifyPin(pin: String): Boolean {
        val entity = appLockDao.get() ?: return false
        val salt = entity.salt ?: return false
        val expectedHash = entity.pinHash ?: return false
        return pinHasher.verify(pin, salt, expectedHash)
    }

    override suspend fun disableLock() {
        val current = appLockDao.get()
        if (current != null) {
            appLockDao.upsert(
                current.copy(isLockEnabled = false, biometricEnabled = false)
            )
        }
    }

    override suspend fun hasPinSet(): Boolean {
        return appLockDao.get()?.pinHash != null
    }

    override suspend fun setBiometricEnabled(enabled: Boolean) {
        val current = appLockDao.get() ?: return
        appLockDao.upsert(current.copy(biometricEnabled = enabled))
    }
}