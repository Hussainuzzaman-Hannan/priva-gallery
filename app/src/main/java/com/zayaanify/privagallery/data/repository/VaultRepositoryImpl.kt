package com.zayaanify.privagallery.data.repository

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.zayaanify.privagallery.data.local.crypto.FileEncryptor
import com.zayaanify.privagallery.data.local.crypto.SecureDelete
import com.zayaanify.privagallery.data.local.db.dao.VaultPhotoDao
import com.zayaanify.privagallery.data.local.db.entity.VaultPhotoEntity
import com.zayaanify.privagallery.domain.model.VaultPhoto
import com.zayaanify.privagallery.domain.repository.VaultRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VaultRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val vaultPhotoDao: VaultPhotoDao,
    private val fileEncryptor: FileEncryptor,
    private val secureDelete: SecureDelete
) : VaultRepository {

    // Vault ফাইলগুলো app-private directory তে রাখা হয় —
    // অন্য কোনো অ্যাপ এই ফোল্ডারে access করতে পারবে না
    private val vaultDir: File
        get() = File(context.filesDir, "vault").also { it.mkdirs() }

    private val thumbDir: File
        get() = File(context.filesDir, "vault_thumbs").also { it.mkdirs() }

    private val tempDir: File
        get() = File(context.cacheDir, "vault_temp").also { it.mkdirs() }

    override fun getAllVaultPhotos(): Flow<List<VaultPhoto>> {
        return vaultPhotoDao.getAllVaultPhotos().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun moveToVault(
        mediaStoreId: Long,
        sourceFilePath: String,
        fileName: String,
        mimeType: String,
        sizeBytes: Long,
        dateTakenMillis: Long
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val sourceFile = File(sourceFilePath)
            if (!sourceFile.exists()) {
                return@withContext Result.failure(Exception("ফাইল পাওয়া যায়নি: $sourceFilePath"))
            }

            // ১. আসল ফাইল এনক্রিপ্ট করা
            val encryptedFile = File(vaultDir, "${System.currentTimeMillis()}_enc")
            val ivBase64 = sourceFile.inputStream().use { input ->
                encryptedFile.outputStream().use { output ->
                    fileEncryptor.encrypt(input, output)
                }
            }

            // ২. Thumbnail বানিয়ে এনক্রিপ্ট করা
            val encryptedThumb = File(thumbDir, "${System.currentTimeMillis()}_thumb_enc")
            createEncryptedThumbnail(sourceFile, encryptedThumb)

            // ৩. DB-তে মেটাডেটা সেভ
            vaultPhotoDao.insert(
                VaultPhotoEntity(
                    encryptedFilePath = encryptedFile.absolutePath,
                    encryptedThumbPath = encryptedThumb.absolutePath,
                    ivBase64 = ivBase64,
                    originalFileName = fileName,
                    mimeType = mimeType,
                    sizeBytes = sizeBytes,
                    dateTakenMillis = dateTakenMillis
                )
            )

            // ৪. MediaStore থেকে আসল ফাইল ডিলিট + secure delete
            val uri = Uri.withAppendedPath(
                MediaStore.Files.getContentUri("external"),
                mediaStoreId.toString()
            )
            context.contentResolver.delete(uri, null, null)
            secureDelete.deleteSecurely(sourceFile)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun restoreFromVault(vaultPhotoId: Long): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val entity = vaultPhotoDao.getById(vaultPhotoId)
                    ?: return@withContext Result.failure(Exception("Vault photo পাওয়া যায়নি"))

                val encryptedFile = File(entity.encryptedFilePath)

                // ১. Decrypt করে MediaStore-এ সেভ
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, entity.originalFileName)
                    put(MediaStore.Images.Media.MIME_TYPE, entity.mimeType)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                }

                val resolver = context.contentResolver
                val insertUri = resolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                ) ?: return@withContext Result.failure(Exception("MediaStore-এ সেভ করা যায়নি"))

                resolver.openOutputStream(insertUri)?.use { outputStream ->
                    fileEncryptor.decrypt(encryptedFile, outputStream)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(insertUri, contentValues, null, null)
                }

                // ২. Vault-এর এনক্রিপ্টেড ফাইল secure delete
                secureDelete.deleteSecurely(encryptedFile)
                secureDelete.deleteSecurely(File(entity.encryptedThumbPath))

                // ৩. DB থেকে মুছে ফেলা
                vaultPhotoDao.deleteById(vaultPhotoId)

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun deleteFromVault(vaultPhotoId: Long): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val entity = vaultPhotoDao.getById(vaultPhotoId)
                    ?: return@withContext Result.failure(Exception("Vault photo পাওয়া যায়নি"))

                secureDelete.deleteSecurely(File(entity.encryptedFilePath))
                secureDelete.deleteSecurely(File(entity.encryptedThumbPath))
                vaultPhotoDao.deleteById(vaultPhotoId)

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun decryptToTemp(vaultPhotoId: Long): Result<File> =
        withContext(Dispatchers.IO) {
            try {
                val entity = vaultPhotoDao.getById(vaultPhotoId)
                    ?: return@withContext Result.failure(Exception("Vault photo পাওয়া যায়নি"))

                // পুরোনো temp ফাইল পরিষ্কার করা
                tempDir.listFiles()?.forEach { it.delete() }

                val tempFile = File(tempDir, entity.originalFileName)
                tempFile.outputStream().use { output ->
                    fileEncryptor.decrypt(File(entity.encryptedFilePath), output)
                }

                Result.success(tempFile)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun createEncryptedThumbnail(sourceFile: File, outputFile: File) {
        try {
            val options = BitmapFactory.Options().apply { inSampleSize = 4 }
            val bitmap = BitmapFactory.decodeFile(sourceFile.absolutePath, options) ?: return

            val thumbBytes = java.io.ByteArrayOutputStream().use { bos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, bos)
                bitmap.recycle()
                bos.toByteArray()
            }

            thumbBytes.inputStream().use { input ->
                outputFile.outputStream().use { output ->
                    fileEncryptor.encrypt(input, output)
                }
            }
        } catch (e: Exception) {
            // Thumbnail বানাতে না পারলে empty ফাইল রাখা হবে
            outputFile.createNewFile()
        }
    }

    private fun VaultPhotoEntity.toDomain() = VaultPhoto(
        id = id,
        encryptedFilePath = encryptedFilePath,
        encryptedThumbPath = encryptedThumbPath,
        ivBase64 = ivBase64,
        originalFileName = originalFileName,
        mimeType = mimeType,
        sizeBytes = sizeBytes,
        dateTakenMillis = dateTakenMillis,
        movedToVaultAt = movedToVaultAt
    )
}