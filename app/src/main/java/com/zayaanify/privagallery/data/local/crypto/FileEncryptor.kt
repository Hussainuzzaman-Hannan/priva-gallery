package com.zayaanify.privagallery.data.local.crypto

import android.util.Base64
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import javax.crypto.Cipher
import javax.crypto.CipherOutputStream
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileEncryptor @Inject constructor(
    private val keystoreManager: KeystoreManager
) {
    private val TRANSFORMATION = "AES/GCM/NoPadding"
    private val GCM_TAG_LENGTH = 128

    /**
     * inputStream থেকে পড়ে outputStream-এ এনক্রিপ্ট করে লেখা।
     * IV (Initialization Vector) রিটার্ন করা হয় — পরে decrypt-এর সময় লাগবে।
     */
    fun encrypt(inputStream: InputStream, outputStream: OutputStream): String {
        val key = keystoreManager.getOrCreateKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)

        val iv = cipher.iv
        // IV প্রথমে ফাইলে লিখে রাখছি যাতে decrypt-এর সময় পাওয়া যায়
        outputStream.write(iv)

        CipherOutputStream(outputStream, cipher).use { cos ->
            inputStream.copyTo(cos)
        }

        return Base64.encodeToString(iv, Base64.DEFAULT)
    }

    /**
     * এনক্রিপ্টেড ফাইল থেকে পড়ে decrypt করে outputStream-এ লেখা।
     */
    fun decrypt(encryptedFile: File, outputStream: OutputStream) {
        encryptedFile.inputStream().use { fis ->
            // প্রথম 12 byte হলো IV
            val iv = ByteArray(12)
            fis.read(iv)

            val key = keystoreManager.getOrCreateKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))

            val buffer = ByteArray(8192)
            val cipherText = fis.readBytes()
            val decrypted = cipher.doFinal(cipherText)
            outputStream.write(decrypted)
        }
    }
}