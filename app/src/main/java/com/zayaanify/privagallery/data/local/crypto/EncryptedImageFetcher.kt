package com.zayaanify.privagallery.data.local.crypto

import android.graphics.BitmapFactory
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import androidx.core.graphics.drawable.toDrawable
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject

/**
 * Coil-এর জন্য custom Fetcher —
 * এনক্রিপ্টেড thumbnail ফাইল decrypt করে Bitmap হিসেবে দেয়।
 * VaultScreen-এ AsyncImage-এ model হিসেবে EncryptedImageModel পাঠালে
 * এই Fetcher সেটা handle করবে।
 */
class EncryptedImageFetcher(
    private val model: EncryptedImageModel,
    private val fileEncryptor: FileEncryptor,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val encryptedFile = File(model.encryptedFilePath)

        val decryptedBytes = ByteArrayOutputStream().use { bos ->
            fileEncryptor.decrypt(encryptedFile, bos)
            bos.toByteArray()
        }

        val bitmap = BitmapFactory.decodeByteArray(decryptedBytes, 0, decryptedBytes.size)
            ?: throw Exception("Could not decode bitmap")

        return DrawableResult(
            drawable = bitmap.toDrawable(options.context.resources),
            isSampled = true,
            dataSource = DataSource.DISK
        )
    }

    class Factory @Inject constructor(
        private val fileEncryptor: FileEncryptor
    ) : Fetcher.Factory<EncryptedImageModel> {
        override fun create(
            data: EncryptedImageModel,
            options: Options,
            imageLoader: ImageLoader
        ): Fetcher = EncryptedImageFetcher(data, fileEncryptor, options)
    }
}

/** Coil-এ pass করার জন্য wrapper model। */
data class EncryptedImageModel(val encryptedFilePath: String)