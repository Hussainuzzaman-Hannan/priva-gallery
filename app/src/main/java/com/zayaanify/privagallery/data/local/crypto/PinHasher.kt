package com.zayaanify.privagallery.data.local.crypto

import java.security.MessageDigest
import java.security.SecureRandom
import javax.inject.Inject

/**
 * PIN কখনো plain text-এ সেভ হয় না। প্রতিবার একটা random salt জেনারেট করে
 * salt + PIN একসাথে SHA-256 দিয়ে হ্যাশ করে রাখা হয়।
 *
 * নোট: প্রোডাকশন-গ্রেড সিকিউরিটির জন্য Argon2/PBKDF2 (iteration সহ) SHA-256-এর চেয়ে ভালো,
 * কিন্তু এই Phase-এ সিম্পল রাখা হয়েছে। Vault Mode (Phase 2)-এ key derivation আরও জোরদার করা হবে।
 */
class PinHasher @Inject constructor() {

    fun generateSalt(): String {
        val saltBytes = ByteArray(16)
        SecureRandom().nextBytes(saltBytes)
        return saltBytes.joinToString("") { "%02x".format(it) }
    }

    fun hash(pin: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val input = salt + pin
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    fun verify(pin: String, salt: String, expectedHash: String): Boolean {
        return hash(pin, salt) == expectedHash
    }
}
