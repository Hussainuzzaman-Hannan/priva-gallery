package com.zayaanify.privagallery.data.local.crypto

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class BiometricAvailability {
    AVAILABLE,          // Biometric ব্যবহার করা যাবে
    NOT_ENROLLED,       // ডিভাইসে biometric সেট করা নেই
    NOT_SUPPORTED,      // হার্ডওয়্যার নেই
    UNAVAILABLE         // অন্য কারণে unavailable
}

sealed class BiometricResult {
    object Success : BiometricResult()
    object Failed : BiometricResult()
    data class Error(val message: String) : BiometricResult()
}

@Singleton
class BiometricHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * ডিভাইসে biometric ব্যবহার করা যাবে কিনা চেক করা।
     */
    fun checkAvailability(): BiometricAvailability {
        val manager = BiometricManager.from(context)
        return when (manager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NOT_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricAvailability.NOT_SUPPORTED
            else -> BiometricAvailability.UNAVAILABLE
        }
    }

    fun isBiometricAvailable(): Boolean {
        return checkAvailability() == BiometricAvailability.AVAILABLE
    }

    /**
     * Biometric prompt দেখানো।
     * activity — FragmentActivity (MainActivity)
     * onResult — Success/Failed/Error callback
     */
    fun showPrompt(
        activity: FragmentActivity,
        title: String = "Biometric Unlock",
        subtitle: String = "unlock with fingerprint or face",
        onResult: (BiometricResult) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(
                result: BiometricPrompt.AuthenticationResult
            ) {
                onResult(BiometricResult.Success)
            }

            override fun onAuthenticationFailed() {
                onResult(BiometricResult.Failed)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                onResult(BiometricResult.Error(errString.toString()))
            }
        }

        val prompt = BiometricPrompt(activity, executor, callback)

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            .build()

        prompt.authenticate(promptInfo)
    }
}