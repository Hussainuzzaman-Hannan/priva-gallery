package com.zayaanify.privagallery.domain.model

/**
 * Vault-এ রাখা একটা এনক্রিপ্টেড ফটোর ডোমেইন মডেল।
 * encryptedFilePath — আসল এনক্রিপ্টেড ফাইলের পাথ (filesDir/vault/ এ)
 * UI-তে thumbnail দেখানোর জন্য encryptedThumbPath আলাদা রাখা হয়েছে।
 */
data class VaultPhoto(
    val id: Long,
    val encryptedFilePath: String,
    val encryptedThumbPath: String,
    val ivBase64: String,
    val originalFileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val dateTakenMillis: Long,
    val movedToVaultAt: Long
)