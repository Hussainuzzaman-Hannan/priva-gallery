package com.zayaanify.privagallery.data.local.crypto

import java.io.File
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/**
 * সাধারণ delete শুধু file system entry সরায় —
 * আসল data disk-এ থেকে যায় এবং recovery tool দিয়ে ফিরিয়ে আনা সম্ভব।
 * এখানে আগে random data দিয়ে overwrite করা হয়, তারপর delete করা হয়।
 */
@Singleton
class SecureDelete @Inject constructor() {

    fun deleteSecurely(file: File): Boolean {
        if (!file.exists()) return true

        return try {
            val length = file.length()
            val random = SecureRandom()
            val buffer = ByteArray(minOf(length, 8192L).toInt())

            // ৩ পাস random data দিয়ে overwrite
            repeat(3) {
                file.outputStream().use { fos ->
                    var remaining = length
                    while (remaining > 0) {
                        random.nextBytes(buffer)
                        val toWrite = minOf(remaining, buffer.size.toLong()).toInt()
                        fos.write(buffer, 0, toWrite)
                        remaining -= toWrite
                    }
                    fos.flush()
                }
            }

            file.delete()
        } catch (e: Exception) {
            false
        }
    }
}