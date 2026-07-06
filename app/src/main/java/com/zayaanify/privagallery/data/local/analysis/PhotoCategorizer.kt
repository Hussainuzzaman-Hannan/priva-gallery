package com.zayaanify.privagallery.data.local.analysis

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.zayaanify.privagallery.domain.model.PhotoCategory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class PhotoCategorizer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val labeler = ImageLabeling.getClient(
        ImageLabelerOptions.Builder()
            .setConfidenceThreshold(0.7f)
            .build()
    )

    /**
     * একটা ফটোর URI থেকে ক্যাটেগরি বের করা।
     * ML Kit-এর on-device model ব্যবহার করা হচ্ছে —
     * কোনো ইন্টারনেট সংযোগ লাগে না।
     */
    suspend fun categorize(uri: Uri): PhotoCategory {
        return try {
            // স্ক্রিনশট চেক — displayName বা path দেখে
            val path = uri.toString().lowercase()
            if (path.contains("screenshot") || path.contains("screen_shot")) {
                return PhotoCategory.SCREENSHOT
            }

            val image = InputImage.fromFilePath(context, uri)
            val labels = processImage(image)

            // ML Kit-এর label থেকে ক্যাটেগরি ম্যাপ করা
            mapLabelsToCategory(labels)
        } catch (e: Exception) {
            PhotoCategory.OTHER
        }
    }

    private suspend fun processImage(image: InputImage): List<String> =
        suspendCancellableCoroutine { continuation ->
            labeler.process(image)
                .addOnSuccessListener { labels ->
                    continuation.resume(labels.map { it.text.lowercase() })
                }
                .addOnFailureListener {
                    continuation.resume(emptyList())
                }
        }

    private fun mapLabelsToCategory(labels: List<String>): PhotoCategory {
        return when {
            labels.any { it in DOCUMENT_LABELS } -> PhotoCategory.DOCUMENT
            labels.any { it in SELFIE_LABELS } -> PhotoCategory.SELFIE
            labels.any { it in FOOD_LABELS } -> PhotoCategory.FOOD
            labels.any { it in NATURE_LABELS } -> PhotoCategory.NATURE
            labels.any { it in PEOPLE_LABELS } -> PhotoCategory.PEOPLE
            labels.any { it in ANIMAL_LABELS } -> PhotoCategory.ANIMAL
            labels.any { it in VEHICLE_LABELS } -> PhotoCategory.VEHICLE
            else -> PhotoCategory.OTHER
        }
    }

    companion object {
        private val DOCUMENT_LABELS = setOf(
            "text", "document", "paper", "book", "letter",
            "newspaper", "magazine", "receipt", "form"
        )
        private val SELFIE_LABELS = setOf(
            "selfie", "face", "nose", "forehead", "chin",
            "cheek", "eyebrow", "portrait"
        )
        private val FOOD_LABELS = setOf(
            "food", "drink", "fruit", "vegetable", "meal",
            "cuisine", "dish", "recipe", "breakfast", "lunch",
            "dinner", "snack", "dessert", "cake", "bread"
        )
        private val NATURE_LABELS = setOf(
            "nature", "tree", "flower", "plant", "forest",
            "mountain", "sky", "cloud", "water", "river",
            "lake", "ocean", "beach", "grass", "leaf"
        )
        private val PEOPLE_LABELS = setOf(
            "people", "person", "crowd", "group", "family",
            "child", "baby", "man", "woman", "human"
        )
        private val ANIMAL_LABELS = setOf(
            "animal", "dog", "cat", "bird", "fish",
            "wildlife", "pet", "mammal", "insect"
        )
        private val VEHICLE_LABELS = setOf(
            "vehicle", "car", "truck", "bus", "motorcycle",
            "bicycle", "train", "airplane", "boat", "ship"
        )
    }
}