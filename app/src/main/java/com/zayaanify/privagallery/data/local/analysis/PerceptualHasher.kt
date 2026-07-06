package com.zayaanify.privagallery.data.local.analysis

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.cos
import kotlin.math.sqrt

/**
 * Perceptual Hash (pHash) অ্যালগরিদম —
 * দুটো ছবি visually কতটা মিলে সেটা একটা 64-bit hash দিয়ে তুলনা করা হয়।
 * Hamming distance ≤ 10 হলে ছবি দুটো "similar" ধরা হয়।
 *
 * সাধারণ MD5/SHA hash-এর সাথে পার্থক্য:
 * - MD5: pixel-perfect same হলেই শুধু match — brightness/crop/resize করলে আর match করে না
 * - pHash: visually similar হলেও match করে — duplicate burst shot ধরতে পারে
 */
@Singleton
class PerceptualHasher @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context
) {
    private val HASH_SIZE = 8      // 8x8 = 64 bit hash
    private val DCT_SIZE = 32      // DCT এর জন্য 32x32 resize

    /**
     * একটা ছবির URI থেকে pHash বের করা।
     * ফলাফল একটা 64-character hex string।
     */
    suspend fun computeHash(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            // ছবি ছোট করে load করা — memory বাঁচাতে
            val options = BitmapFactory.Options().apply {
                inSampleSize = 4
            }
            val bitmap = context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, options)
            } ?: return@withContext null

            computeHashFromBitmap(bitmap).also { bitmap.recycle() }
        } catch (e: Exception) {
            null
        }
    }

    private fun computeHashFromBitmap(source: Bitmap): String {
        // ১. 32x32 grayscale-এ resize
        val resized = Bitmap.createScaledBitmap(source, DCT_SIZE, DCT_SIZE, true)
        val pixels = Array(DCT_SIZE) { y ->
            DoubleArray(DCT_SIZE) { x ->
                val pixel = resized.getPixel(x, y)
                // Grayscale conversion: luminance formula
                0.299 * Color.red(pixel) + 0.587 * Color.green(pixel) + 0.114 * Color.blue(pixel)
            }
        }
        resized.recycle()

        // ২. DCT (Discrete Cosine Transform) apply
        val dct = applyDCT(pixels)

        // ৩. শুধু top-left 8x8 নেওয়া (low-frequency component)
        val dctLowFreq = Array(HASH_SIZE) { y ->
            DoubleArray(HASH_SIZE) { x -> dct[y][x] }
        }

        // ৪. গড় বের করা (DC component বাদ দিয়ে)
        var sum = 0.0
        for (y in 0 until HASH_SIZE) {
            for (x in 0 until HASH_SIZE) {
                sum += dctLowFreq[y][x]
            }
        }
        val avg = (sum - dctLowFreq[0][0]) / (HASH_SIZE * HASH_SIZE - 1)

        // ৫. গড়ের উপরে/নিচে — 1/0 bit assign করা
        val hashBits = StringBuilder()
        for (y in 0 until HASH_SIZE) {
            for (x in 0 until HASH_SIZE) {
                hashBits.append(if (dctLowFreq[y][x] > avg) '1' else '0')
            }
        }

        // ৬. Binary string কে hex-এ convert
        return hashBits.toString()
            .chunked(4)
            .joinToString("") { nibble ->
                nibble.toInt(2).toString(16)
            }
    }

    private fun applyDCT(pixels: Array<DoubleArray>): Array<DoubleArray> {
        val n = DCT_SIZE
        val result = Array(n) { DoubleArray(n) }

        for (u in 0 until n) {
            for (v in 0 until n) {
                var sum = 0.0
                for (x in 0 until n) {
                    for (y in 0 until n) {
                        sum += pixels[x][y] *
                                cos((2 * x + 1) * u * Math.PI / (2 * n)) *
                                cos((2 * y + 1) * v * Math.PI / (2 * n))
                    }
                }
                val cu = if (u == 0) 1.0 / sqrt(2.0) else 1.0
                val cv = if (v == 0) 1.0 / sqrt(2.0) else 1.0
                result[u][v] = (2.0 / n) * cu * cv * sum
            }
        }
        return result
    }

    /**
     * দুটো hash-এর Hamming distance বের করা।
     * distance ≤ 10 → similar/duplicate
     * distance = 0  → identical (বা প্রায় identical)
     */
    fun hammingDistance(hash1: String, hash2: String): Int {
        if (hash1.length != hash2.length) return Int.MAX_VALUE
        // Hex থেকে binary-তে convert করে bit-by-bit তুলনা
        val bin1 = hash1.map { it.digitToInt(16).toString(2).padStart(4, '0') }.joinToString("")
        val bin2 = hash2.map { it.digitToInt(16).toString(2).padStart(4, '0') }.joinToString("")
        return bin1.zip(bin2).count { (a, b) -> a != b }
    }

    fun isSimilar(hash1: String, hash2: String, threshold: Int = 10): Boolean {
        return hammingDistance(hash1, hash2) <= threshold
    }
}