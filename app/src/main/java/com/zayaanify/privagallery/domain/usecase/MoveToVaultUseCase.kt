package com.zayaanify.privagallery.domain.usecase

import android.content.Context
import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import com.zayaanify.privagallery.domain.repository.VaultRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class MoveToVaultUseCase @Inject constructor(
    private val vaultRepository: VaultRepository,
    @ApplicationContext private val context: Context
) {
    suspend operator fun invoke(
        mediaStoreId: Long,
        mimeType: String,
        displayName: String,
        sizeBytes: Long,
        dateTakenMillis: Long
    ): Result<Unit> = withContext(Dispatchers.IO) {

        // MediaStore থেকে আসল ফাইলের পাথ বের করা
        val uri = ContentUris.withAppendedId(
            MediaStore.Files.getContentUri("external"),
            mediaStoreId
        )

        val filePath = context.contentResolver.query(
            uri,
            arrayOf(MediaStore.MediaColumns.DATA),
            null, null, null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA))
            } else null
        }

        if (filePath == null) {
            return@withContext Result.failure(Exception("ফাইলের পাথ পাওয়া যায়নি"))
        }

        vaultRepository.moveToVault(
            mediaStoreId = mediaStoreId,
            sourceFilePath = filePath,
            fileName = displayName,
            mimeType = mimeType,
            sizeBytes = sizeBytes,
            dateTakenMillis = dateTakenMillis
        )
    }
}