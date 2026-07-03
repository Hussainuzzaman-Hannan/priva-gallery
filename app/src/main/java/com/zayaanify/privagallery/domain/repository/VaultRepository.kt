package com.zayaanify.privagallery.domain.repository

import com.zayaanify.privagallery.domain.model.VaultPhoto
import kotlinx.coroutines.flow.Flow
import java.io.File

interface VaultRepository {

    /** Vault-এর সব ফটো রিয়েল-টাইমে observe করা। */
    fun getAllVaultPhotos(): Flow<List<VaultPhoto>>

    /**
     * একটা ফটো Vault-এ নেওয়া:
     * ১. ফাইল এনক্রিপ্ট করে filesDir/vault/ এ সেভ
     * ২. Thumbnail এনক্রিপ্ট করে সেভ
     * ৩. DB-তে মেটাডেটা রাখা
     * ৪. MediaStore থেকে আসল ফাইল secure delete
     */
    suspend fun moveToVault(
        mediaStoreId: Long,
        sourceFilePath: String,
        fileName: String,
        mimeType: String,
        sizeBytes: Long,
        dateTakenMillis: Long
    ): Result<Unit>

    /**
     * Vault থেকে ফটো বের করা (restore):
     * ১. এনক্রিপ্টেড ফাইল decrypt করা
     * ২. MediaStore-এ সেভ করা
     * ৩. Vault-এর এনক্রিপ্টেড ফাইল secure delete
     * ৪. DB থেকে entry মুছে ফেলা
     */
    suspend fun restoreFromVault(vaultPhotoId: Long): Result<Unit>

    /**
     * Vault থেকে ফটো সম্পূর্ণ ডিলিট করা (restore না করে):
     * এনক্রিপ্টেড ফাইল secure delete + DB entry মুছে ফেলা।
     * একবার করলে ফটো আর ফিরে পাওয়া যাবে না।
     */
    suspend fun deleteFromVault(vaultPhotoId: Long): Result<Unit>

    /** Vault-এর একটা ফটো decrypt করে temp ফাইলে দেওয়া — ফুলস্ক্রিন ভিউয়ারের জন্য। */
    suspend fun decryptToTemp(vaultPhotoId: Long): Result<File>
}