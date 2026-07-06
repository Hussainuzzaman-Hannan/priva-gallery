package com.zayaanify.privagallery.domain.repository

import android.net.Uri

interface BackupRepository {

    /**
     * Vault-এর সব এনক্রিপ্টেড ফটো + DB মেটাডেটা একটা
     * password-protected ZIP ফাইলে export করা।
     * destinationUri — SAF দিয়ে ইউজার যেখানে সেভ করতে চান।
     */
    suspend fun createBackup(
        destinationUri: Uri,
        password: String,
        onProgress: (Float) -> Unit
    ): Result<Unit>

    /**
     * Backup ফাইল থেকে Vault restore করা।
     * sourceUri — SAF দিয়ে ইউজার যে ফাইল সিলেক্ট করেছেন।
     */
    suspend fun restoreBackup(
        sourceUri: Uri,
        password: String,
        onProgress: (Float) -> Unit
    ): Result<Unit>

    /** Backup ফাইলের মেটাডেটা পড়া — কতটা ফটো, কবে বানানো ইত্যাদি। */
    suspend fun readBackupInfo(sourceUri: Uri, password: String): Result<BackupInfo>
}

data class BackupInfo(
    val photoCount: Int,
    val createdAt: Long,
    val appVersion: String,
    val totalSizeBytes: Long
)