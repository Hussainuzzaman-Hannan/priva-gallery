package com.zayaanify.privagallery.domain.usecase

import android.net.Uri
import com.zayaanify.privagallery.domain.repository.BackupRepository
import com.zayaanify.privagallery.domain.repository.BackupInfo
import javax.inject.Inject

class RestoreBackupUseCase @Inject constructor(
    private val backupRepository: BackupRepository
) {
    suspend operator fun invoke(
        sourceUri: Uri,
        password: String,
        onProgress: (Float) -> Unit = {}
    ): Result<Unit> = backupRepository.restoreBackup(sourceUri, password, onProgress)

    suspend fun readInfo(sourceUri: Uri, password: String): Result<BackupInfo> =
        backupRepository.readBackupInfo(sourceUri, password)
}