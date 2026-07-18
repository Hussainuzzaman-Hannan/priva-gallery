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

    override fun isLockEnabled(): Flow<Boolean> =
        appLockDao.observe().map { it?.isLockEnabled == true }

    override fun isBiometricEnabled(): Flow<Boolean> =
        appLockDao.observe().map { it?.biometricEnabled == true }

    override fun isVaultPinSet(): Flow<Boolean> =
        appLockDao.observe().map { it?.vaultPinHash != null }

    override fun isVaultBiometricEnabled(): Flow<Boolean> =
        appLockDao.observe().map { it?.vaultBiometricEnabled == true }

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
                biometricEnabled = current?.biometricEnabled ?: false,
                vaultPinHash = current?.vaultPinHash,
                vaultSalt = current?.vaultSalt,
                vaultBiometricEnabled = current?.vaultBiometricEnabled ?: false
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

    override suspend fun hasPinSet(): Boolean =
        appLockDao.get()?.pinHash != null

    override suspend fun setBiometricEnabled(enabled: Boolean) {
        val current = appLockDao.get() ?: return
        appLockDao.upsert(current.copy(biometricEnabled = enabled))
    }

    override suspend fun setVaultPin(pin: String) {
        val salt = pinHasher.generateSalt()
        val hash = pinHasher.hash(pin, salt)
        val current = appLockDao.get() ?: AppLockEntity(
            id = 1, pinHash = null, salt = null
        )
        appLockDao.upsert(
            current.copy(vaultPinHash = hash, vaultSalt = salt)
        )
    }

    override suspend fun verifyVaultPin(pin: String): Boolean {
        val entity = appLockDao.get() ?: return false
        val salt = entity.vaultSalt ?: return false
        val expectedHash = entity.vaultPinHash ?: return false
        return pinHasher.verify(pin, salt, expectedHash)
    }

    override suspend fun hasVaultPinSet(): Boolean =
        appLockDao.get()?.vaultPinHash != null

    override suspend fun setVaultBiometricEnabled(enabled: Boolean) {
        val current = appLockDao.get() ?: return
        appLockDao.upsert(current.copy(vaultBiometricEnabled = enabled))
    }

    override suspend fun removeVaultPin() {
        val current = appLockDao.get() ?: return
        appLockDao.upsert(
            current.copy(
                vaultPinHash = null,
                vaultSalt = null,
                vaultBiometricEnabled = false
            )
        )
    }
}