package com.zayaanify.privagallery.data.repository

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.provider.MediaStore
import com.zayaanify.privagallery.data.local.db.dao.RecycleBinDao
import com.zayaanify.privagallery.data.local.db.entity.RecycleBinEntity
import com.zayaanify.privagallery.domain.model.RecycleBinPhoto
import com.zayaanify.privagallery.domain.repository.RecycleBinRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecycleBinRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val recycleBinDao: RecycleBinDao
) : RecycleBinRepository {

    private val trashDir: File
        get() = File(context.filesDir, "trash").also { it.mkdirs() }

    private val trashThumbDir: File
        get() = File(context.filesDir, "trash_thumbs").also { it.mkdirs() }

    override fun observeAll(): Flow<List<RecycleBinPhoto>> =
        recycleBinDao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }

    /**
     * ফাইল Recycle Bin-এ copy করা।
     * MediaStore থেকে delete আলাদাভাবে হবে — AlbumDetailViewModel-এ
     * deletePhotosUseCase দিয়ে user confirmation এর পর।
     */
    override suspend fun copyToRecycleBin(mediaStoreId: Long): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val imageUri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, mediaStoreId
                )
                val videoUri = ContentUris.withAppendedId(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI, mediaStoreId
                )

                val isImage = context.contentResolver.query(
                    imageUri,
                    arrayOf(MediaStore.MediaColumns._ID),
                    null, null, null
                )?.use { it.count > 0 } ?: false

                val uri = if (isImage) imageUri else videoUri

                val projection = arrayOf(
                    MediaStore.MediaColumns.DISPLAY_NAME,
                    MediaStore.MediaColumns.MIME_TYPE,
                    MediaStore.MediaColumns.SIZE,
                    MediaStore.MediaColumns.DATE_TAKEN
                )

                val info = context.contentResolver.query(
                    uri, projection, null, null, null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        mapOf(
                            "name" to (cursor.getString(0) ?: "unknown"),
                            "mime" to (cursor.getString(1) ?: "image/jpeg"),
                            "size" to cursor.getLong(2).toString(),
                            "date" to cursor.getLong(3).toString()
                        )
                    } else null
                } ?: return@withContext Result.failure(Exception("ফাইল পাওয়া যায়নি"))

                // app-private storage-এ copy করা
                val destFile = File(trashDir, "${mediaStoreId}_${info["name"]}")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                // Thumbnail বানানো
                val thumbFile = File(trashThumbDir, "${mediaStoreId}_thumb.jpg")
                createThumbnail(destFile.absolutePath, thumbFile)

                // DB-তে রাখা
                recycleBinDao.insert(
                    RecycleBinEntity(
                        originalMediaStoreId = mediaStoreId,
                        filePath = destFile.absolutePath,
                        thumbnailPath = thumbFile.absolutePath,
                        displayName = info["name"] ?: "",
                        mimeType = info["mime"] ?: "",
                        sizeBytes = info["size"]?.toLong() ?: 0L,
                        dateTakenMillis = info["date"]?.toLong() ?: 0L
                    )
                )

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun restore(recycleBinId: Long): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val entity = recycleBinDao.getById(recycleBinId)
                    ?: return@withContext Result.failure(Exception("Item পাওয়া যায়নি"))

                // file থেকে MediaStore-এ insert করা
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, entity.displayName)
                    put(MediaStore.Images.Media.MIME_TYPE, entity.mimeType)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                }

                val insertUri = context.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                ) ?: return@withContext Result.failure(Exception("Restore করা যায়নি"))

                context.contentResolver.openOutputStream(insertUri)?.use { output ->
                    File(entity.filePath).inputStream().use { input ->
                        input.copyTo(output)
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    context.contentResolver.update(insertUri, contentValues, null, null)
                }

                // Trash থেকে মুছে ফেলা
                File(entity.filePath).delete()
                File(entity.thumbnailPath).delete()
                recycleBinDao.deleteById(recycleBinId)

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun deletePermanently(recycleBinId: Long): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val entity = recycleBinDao.getById(recycleBinId)
                    ?: return@withContext Result.failure(Exception("Item পাওয়া যায়নি"))

                File(entity.filePath).delete()
                File(entity.thumbnailPath).delete()
                recycleBinDao.deleteById(recycleBinId)

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun emptyRecycleBin(): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                trashDir.listFiles()?.forEach { it.delete() }
                trashThumbDir.listFiles()?.forEach { it.delete() }
                recycleBinDao.deleteAll()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun deleteExpiredItems() = withContext(Dispatchers.IO) {
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        val expired = recycleBinDao.getExpiredItems(thirtyDaysAgo)
        expired.forEach { entity ->
            File(entity.filePath).delete()
            File(entity.thumbnailPath).delete()
            recycleBinDao.deleteById(entity.id)
        }
    }

    private fun createThumbnail(sourcePath: String, destFile: File) {
        try {
            val options = BitmapFactory.Options().apply { inSampleSize = 4 }
            val bitmap = BitmapFactory.decodeFile(sourcePath, options) ?: return
            destFile.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, out)
            }
            bitmap.recycle()
        } catch (e: Exception) {
            destFile.createNewFile()
        }
    }

    private fun RecycleBinEntity.toDomain() = RecycleBinPhoto(
        id = id,
        originalMediaStoreId = originalMediaStoreId,
        filePath = filePath,
        thumbnailPath = thumbnailPath,
        displayName = displayName,
        mimeType = mimeType,
        sizeBytes = sizeBytes,
        dateTakenMillis = dateTakenMillis,
        deletedAt = deletedAt
    )
}