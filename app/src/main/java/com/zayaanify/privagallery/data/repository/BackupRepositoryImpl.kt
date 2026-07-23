package com.zayaanify.privagallery.data.repository

import android.content.Context
import android.net.Uri
import com.zayaanify.privagallery.data.local.db.dao.VaultPhotoDao
import com.zayaanify.privagallery.data.local.db.entity.VaultPhotoEntity
import com.zayaanify.privagallery.domain.repository.BackupInfo
import com.zayaanify.privagallery.domain.repository.BackupRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.SecureRandom
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val vaultPhotoDao: VaultPhotoDao
) : BackupRepository {

    companion object {
        private const val MANIFEST_FILE = "manifest.json"
        private const val PHOTOS_DIR = "photos/"
        private const val PBKDF2_ITERATIONS = 100_000
        private const val KEY_LENGTH = 256
        private const val SALT_LENGTH = 32
        private const val IV_LENGTH = 16
        private const val APP_VERSION = "1.0"
    }

    override suspend fun createBackup(
        destinationUri: Uri,
        password: String,
        onProgress: (Float) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val photos = vaultPhotoDao.getAllVaultPhotos().first()

            val salt = ByteArray(SALT_LENGTH).also { SecureRandom().nextBytes(it) }
            val iv = ByteArray(IV_LENGTH).also { SecureRandom().nextBytes(it) }
            val key = deriveKey(password, salt)

            context.contentResolver.openOutputStream(destinationUri)?.use { rawOut ->
                // Salt ও IV প্রথমে plain লেখা — decrypt করতে লাগবে
                rawOut.write(salt)
                rawOut.write(iv)

                val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                cipher.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(iv))

                CipherOutputStream(rawOut, cipher).use { cipherOut ->
                    ZipOutputStream(cipherOut).use { zip ->

                        // Manifest তৈরি
                        val photosArray = JSONArray()
                        photos.forEach { photo ->
                            photosArray.put(JSONObject().apply {
                                put("id", photo.id)
                                put("originalFileName", photo.originalFileName)
                                put("mimeType", photo.mimeType)
                                put("sizeBytes", photo.sizeBytes)
                                put("dateTakenMillis", photo.dateTakenMillis)
                                put("ivBase64", photo.ivBase64)
                                put("encryptedFileName", "${photo.id}_enc")
                                put("encryptedThumbName", "${photo.id}_thumb_enc")
                            })
                        }

                        val manifest = JSONObject().apply {
                            put("appVersion", APP_VERSION)
                            put("createdAt", System.currentTimeMillis())
                            put("photoCount", photos.size)
                            put("photos", photosArray)
                        }

                        // Manifest লেখা
                        zip.putNextEntry(ZipEntry(MANIFEST_FILE))
                        zip.write(manifest.toString().toByteArray(Charsets.UTF_8))
                        zip.closeEntry()

                        // প্রতিটা এনক্রিপ্টেড ফাইল zip-এ যোগ করা
                        photos.forEachIndexed { index, photo ->
                            val encFile = File(photo.encryptedFilePath)
                            if (encFile.exists()) {
                                zip.putNextEntry(ZipEntry("$PHOTOS_DIR${photo.id}_enc"))
                                encFile.inputStream().use { it.copyTo(zip) }
                                zip.closeEntry()
                            }

                            val thumbFile = File(photo.encryptedThumbPath)
                            if (thumbFile.exists()) {
                                zip.putNextEntry(ZipEntry("$PHOTOS_DIR${photo.id}_thumb_enc"))
                                thumbFile.inputStream().use { it.copyTo(zip) }
                                zip.closeEntry()
                            }

                            onProgress((index + 1).toFloat() / photos.size.coerceAtLeast(1))
                        }
                    }
                }
            } ?: return@withContext Result.failure(Exception("Could not open file"))

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun restoreBackup(
        sourceUri: Uri,
        password: String,
        onProgress: (Float) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val vaultDir = File(context.filesDir, "vault").also { it.mkdirs() }
            val thumbDir = File(context.filesDir, "vault_thumbs").also { it.mkdirs() }

            context.contentResolver.openInputStream(sourceUri)?.use { rawIn ->
                val salt = ByteArray(SALT_LENGTH).also { rawIn.read(it) }
                val iv = ByteArray(IV_LENGTH).also { rawIn.read(it) }
                val key = deriveKey(password, salt)

                val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))

                CipherInputStream(rawIn, cipher).use { cipherIn ->
                    ZipInputStream(cipherIn).use { zip ->

                        var manifest: JSONObject? = null
                        val photoFiles = mutableMapOf<String, ByteArray>()

                        var entry = zip.nextEntry
                        while (entry != null) {
                            val bytes = zip.readBytes()
                            when {
                                entry.name == MANIFEST_FILE -> {
                                    manifest = JSONObject(String(bytes, Charsets.UTF_8))
                                }
                                entry.name.startsWith(PHOTOS_DIR) -> {
                                    val fileName = entry.name.removePrefix(PHOTOS_DIR)
                                    photoFiles[fileName] = bytes
                                }
                            }
                            zip.closeEntry()
                            entry = zip.nextEntry
                        }

                        val m = manifest
                            ?: return@withContext Result.failure(
                                Exception("Backup ফাইল corrupt — manifest পাওয়া যায়নি")
                            )

                        val photosArray = m.getJSONArray("photos")
                        val total = photosArray.length()

                        for (i in 0 until total) {
                            val photoJson = photosArray.getJSONObject(i)
                            val id = photoJson.getLong("id")

                            val encFileName = photoJson.getString("encryptedFileName")
                            val thumbFileName = photoJson.getString("encryptedThumbName")

                            val encFile = File(vaultDir, "${id}_enc")
                            val thumbFile = File(thumbDir, "${id}_thumb_enc")

                            photoFiles[encFileName]?.let { encFile.writeBytes(it) }
                            photoFiles[thumbFileName]?.let { thumbFile.writeBytes(it) }

                            vaultPhotoDao.insert(
                                VaultPhotoEntity(
                                    encryptedFilePath = encFile.absolutePath,
                                    encryptedThumbPath = thumbFile.absolutePath,
                                    ivBase64 = photoJson.getString("ivBase64"),
                                    originalFileName = photoJson.getString("originalFileName"),
                                    mimeType = photoJson.getString("mimeType"),
                                    sizeBytes = photoJson.getLong("sizeBytes"),
                                    dateTakenMillis = photoJson.getLong("dateTakenMillis")
                                )
                            )

                            onProgress((i + 1).toFloat() / total.coerceAtLeast(1))
                        }
                    }
                }
            } ?: return@withContext Result.failure(Exception("Could not open file"))

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("ভুল পাসওয়ার্ড অথবা corrupt ফাইল: ${e.message}"))
        }
    }

    override suspend fun readBackupInfo(
        sourceUri: Uri,
        password: String
    ): Result<BackupInfo> = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(sourceUri)?.use { rawIn ->
                val salt = ByteArray(SALT_LENGTH).also { rawIn.read(it) }
                val iv = ByteArray(IV_LENGTH).also { rawIn.read(it) }
                val key = deriveKey(password, salt)

                val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))

                CipherInputStream(rawIn, cipher).use { cipherIn ->
                    ZipInputStream(cipherIn).use { zip ->
                        var entry = zip.nextEntry
                        while (entry != null) {
                            if (entry.name == MANIFEST_FILE) {
                                val manifest = JSONObject(
                                    String(zip.readBytes(), Charsets.UTF_8)
                                )
                                return@withContext Result.success(
                                    BackupInfo(
                                        photoCount = manifest.getInt("photoCount"),
                                        createdAt = manifest.getLong("createdAt"),
                                        appVersion = manifest.getString("appVersion"),
                                        totalSizeBytes = 0L
                                    )
                                )
                            }
                            zip.closeEntry()
                            entry = zip.nextEntry
                        }
                    }
                }
            }
            Result.failure(Exception("Manifest পাওয়া যায়নি"))
        } catch (e: Exception) {
            Result.failure(Exception("ভুল পাসওয়ার্ড: ${e.message}"))
        }
    }

    private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(
            password.toCharArray(),
            salt,
            PBKDF2_ITERATIONS,
            KEY_LENGTH
        )
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }
}