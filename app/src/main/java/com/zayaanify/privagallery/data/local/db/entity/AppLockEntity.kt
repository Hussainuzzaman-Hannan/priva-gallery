package com.zayaanify.privagallery.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_lock_settings")
data class AppLockEntity(
    @PrimaryKey val id: Int = 1,
    val pinHash: String?,
    val salt: String?,
    val isLockEnabled: Boolean = false,
    val biometricEnabled: Boolean = false   // ← নতুন field
)