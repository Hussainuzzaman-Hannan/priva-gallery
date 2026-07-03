package com.zayaanify.privagallery.domain.usecase

import com.zayaanify.privagallery.domain.repository.VaultRepository
import javax.inject.Inject

class DeleteFromVaultUseCase @Inject constructor(
    private val vaultRepository: VaultRepository
) {
    suspend operator fun invoke(vaultPhotoId: Long): Result<Unit> {
        return vaultRepository.deleteFromVault(vaultPhotoId)
    }
}