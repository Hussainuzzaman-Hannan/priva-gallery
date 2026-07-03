package com.zayaanify.privagallery.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Vault-এ রাখা প্রতিটা এনক্রিপ্টেড ফটোর মেটাডেটা।
 * আসল ফাইল filesDir/vault/ এ এনক্রিপ্টেড অবস্থায় থাকে।
 * encryptedFilePath ও encryptedThumbPath — সেই ফাইলের পাথ।
 * ivBase64 — AES-GCM decrypt করতে লাগবে।
 */
@Entity(tableName = "vault_photos")
data class VaultPhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val encryptedFilePath: String,
    val encryptedThumbPath: String,
    val ivBase64: String,
    val originalFileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val dateTakenMillis: Long,
    val movedToVaultAt: Long = System.currentTimeMillis()
)