package com.zayaanify.privagallery.data.local.analysis

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class OcrScanner @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * একটা ফটো URI থেকে সব টেক্সট বের করা।
     * ML Kit on-device OCR — ইন্টারনেট লাগে না।
     */
    suspend fun extractText(uri: Uri): String? {
        return try {
            val image = InputImage.fromFilePath(context, uri)
            processImage(image)
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun processImage(image: InputImage): String =
        suspendCancellableCoroutine { continuation ->
            recognizer.process(image)
                .addOnSuccessListener { result ->
                    continuation.resume(result.text)
                }
                .addOnFailureListener {
                    continuation.resume("")
                }
        }
}