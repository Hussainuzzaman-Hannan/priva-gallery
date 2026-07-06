package com.zayaanify.privagallery.domain.model

enum class PhotoCategory(val displayName: String, val emoji: String) {
    SCREENSHOT("স্ক্রিনশট", "📱"),
    DOCUMENT("ডকুমেন্ট", "📄"),
    SELFIE("সেলফি", "🤳"),
    FOOD("খাবার", "🍔"),
    NATURE("প্রকৃতি", "🌿"),
    PEOPLE("মানুষ", "👥"),
    ANIMAL("প্রাণী", "🐾"),
    VEHICLE("যানবাহন", "🚗"),
    OTHER("অন্যান্য", "🖼️")
}