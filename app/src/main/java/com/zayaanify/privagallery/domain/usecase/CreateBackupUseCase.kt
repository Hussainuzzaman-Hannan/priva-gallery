package com.zayaanify.privagallery.domain.usecase

import android.net.Uri
import com.zayaanify.privagallery.domain.repository.BackupRepository
import javax.inject.Inject

class CreateBackupUseCase @Inject constructor(
    private val backupRepository: BackupRepository
) {
    suspend operator fun invoke(
        destinationUri: Uri,
        password: String,
        onProgress: (Float) -> Unit = {}
    ): Result<Unit> = backupRepository.createBackup(destinationUri, password, onProgress)
}