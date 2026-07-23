package com.zayaanify.privagallery.presentation.lock

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zayaanify.privagallery.data.local.crypto.BiometricHelper
import com.zayaanify.privagallery.data.local.crypto.BiometricResult
import com.zayaanify.privagallery.domain.repository.AppLockRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LockScreenMode { SETUP, UNLOCK }

data class AppLockUiState(
    val mode: LockScreenMode = LockScreenMode.UNLOCK,
    val enteredPin: String = "",
    val confirmPin: String = "",
    val isConfirmStep: Boolean = false,
    val errorMessage: String? = null,
    val isUnlocked: Boolean = false,
    val isCheckingInitialState: Boolean = true,
    val isBiometricEnabled: Boolean = false,
    val isBiometricAvailable: Boolean = false
)

@HiltViewModel
class AppLockViewModel @Inject constructor(
    private val appLockRepository: AppLockRepository,
    private val biometricHelper: BiometricHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppLockUiState())
    val uiState: StateFlow<AppLockUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val hasPin = appLockRepository.hasPinSet()
                val isBiometricAvailable = biometricHelper.isBiometricAvailable()

                _uiState.value = _uiState.value.copy(
                    mode = if (hasPin) LockScreenMode.UNLOCK else LockScreenMode.SETUP,
                    isCheckingInitialState = false,
                    isBiometricAvailable = isBiometricAvailable
                )
            } catch (e: Exception) {
                // কোনো কারণে DB/biometric চেক ব্যর্থ হলেও যেন UI চিরস্থায়ী
                // স্পিনারে আটকে না থাকে — নিরাপদ ডিফল্ট হিসেবে PIN entry দেখানো হবে।
                _uiState.value = _uiState.value.copy(
                    mode = LockScreenMode.UNLOCK,
                    isCheckingInitialState = false,
                    isBiometricAvailable = false
                )
            }
        }

        // Biometric enabled স্ট্যাটাস observe করা
        appLockRepository.isBiometricEnabled()
            .onEach { enabled ->
                _uiState.value = _uiState.value.copy(isBiometricEnabled = enabled)
            }
            .launchIn(viewModelScope)
    }

    fun onDigitPress(digit: String) {
        val current = _uiState.value
        if (current.mode == LockScreenMode.SETUP && current.isConfirmStep) {
            if (current.confirmPin.length < 4) {
                onConfirmPinChanged(current.confirmPin + digit)
            }
        } else {
            if (current.enteredPin.length < 4) {
                onPinChanged(current.enteredPin + digit)
            }
        }
    }

    fun onBackspace() {
        val current = _uiState.value
        if (current.mode == LockScreenMode.SETUP && current.isConfirmStep) {
            onConfirmPinChanged(current.confirmPin.dropLast(1))
        } else {
            onPinChanged(current.enteredPin.dropLast(1))
        }
    }

    private fun onPinChanged(pin: String) {
        _uiState.value = _uiState.value.copy(enteredPin = pin, errorMessage = null)
        if (pin.length == 4) {
            when (_uiState.value.mode) {
                LockScreenMode.SETUP -> moveToConfirmStep()
                LockScreenMode.UNLOCK -> verifyAndUnlock(pin)
            }
        }
    }

    private fun onConfirmPinChanged(pin: String) {
        _uiState.value = _uiState.value.copy(confirmPin = pin, errorMessage = null)
        if (pin.length == 4) confirmSetup()
    }

    private fun moveToConfirmStep() {
        _uiState.value = _uiState.value.copy(isConfirmStep = true)
    }

    private fun confirmSetup() {
        val current = _uiState.value
        if (current.enteredPin == current.confirmPin) {
            viewModelScope.launch {
                appLockRepository.setPin(current.enteredPin)
                _uiState.value = current.copy(isUnlocked = true)
            }
        } else {
            _uiState.value = current.copy(
                enteredPin = "",
                confirmPin = "",
                isConfirmStep = false,
                errorMessage = "PIN মিলেনি, Try Again"
            )
        }
    }

    private fun verifyAndUnlock(pin: String) {
        viewModelScope.launch {
            val isCorrect = appLockRepository.verifyPin(pin)
            if (isCorrect) {
                _uiState.value = _uiState.value.copy(isUnlocked = true)
            } else {
                _uiState.value = _uiState.value.copy(
                    enteredPin = "",
                    errorMessage = "ভুল PIN, Try Again"
                )
            }
        }
    }

    /** Biometric prompt দেখানো — activity reference দরকার। */
    fun showBiometricPrompt(activity: FragmentActivity) {
        biometricHelper.showPrompt(
            activity = activity,
            title = "PrivaGallery Unlock",
            subtitle = "unlock with fingerprint or face"
        ) { result ->
            when (result) {
                is BiometricResult.Success -> {
                    _uiState.value = _uiState.value.copy(isUnlocked = true)
                }
                is BiometricResult.Failed -> {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Biometric যাচাই ব্যর্থ হয়েছে"
                    )
                }
                is BiometricResult.Error -> {
                    // User cancel করলে error আসে — PIN দিয়ে চেষ্টার সুযোগ দেওয়া
                    _uiState.value = _uiState.value.copy(
                        errorMessage = if (result.message.contains("cancel", ignoreCase = true))
                            null else result.message
                    )
                }
            }
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appLockRepository.setBiometricEnabled(enabled)
        }
    }
}