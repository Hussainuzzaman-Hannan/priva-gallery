package com.zayaanify.privagallery.domain.repository

import kotlinx.coroutines.flow.Flow

interface AppLockRepository {
    fun isLockEnabled(): Flow<Boolean>
    fun isBiometricEnabled(): Flow<Boolean>
    fun isVaultPinSet(): Flow<Boolean>
    fun isVaultBiometricEnabled(): Flow<Boolean>
    suspend fun setPin(pin: String)
    suspend fun verifyPin(pin: String): Boolean
    suspend fun disableLock()
    suspend fun hasPinSet(): Boolean
    suspend fun setBiometricEnabled(enabled: Boolean)
    // Vault PIN
    suspend fun setVaultPin(pin: String)
    suspend fun verifyVaultPin(pin: String): Boolean
    suspend fun hasVaultPinSet(): Boolean
    suspend fun setVaultBiometricEnabled(enabled: Boolean)
    suspend fun removeVaultPin()
}