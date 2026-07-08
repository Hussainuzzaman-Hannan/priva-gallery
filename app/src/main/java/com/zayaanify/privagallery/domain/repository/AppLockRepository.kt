package com.zayaanify.privagallery.domain.repository

import kotlinx.coroutines.flow.Flow

interface AppLockRepository {
    fun isLockEnabled(): Flow<Boolean>
    fun isBiometricEnabled(): Flow<Boolean>
    suspend fun setPin(pin: String)
    suspend fun verifyPin(pin: String): Boolean
    suspend fun disableLock()
    suspend fun hasPinSet(): Boolean
    suspend fun setBiometricEnabled(enabled: Boolean)
}