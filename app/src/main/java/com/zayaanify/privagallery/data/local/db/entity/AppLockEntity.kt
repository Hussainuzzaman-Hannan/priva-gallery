package com.zayaanify.privagallery.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * একটাই row থাকবে এই টেবিলে (id = 1)। pinHash-এ কখনো plain PIN রাখা হয় না —
 * SHA-256 + salt দিয়ে হ্যাশ করে রাখা হয় (দেখুন PinHasher.kt)।
 */
@Entity(tableName = "app_lock_settings")
data class AppLockEntity(
    @PrimaryKey val id: Int = 1,
    val pinHash: String?,
    val salt: String?,
    val isLockEnabled: Boolean = false
)
