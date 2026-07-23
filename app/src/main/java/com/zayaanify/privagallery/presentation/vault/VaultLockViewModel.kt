package com.zayaanify.privagallery.presentation.vault

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
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class VaultLockMode { SETUP, UNLOCK }

data class VaultLockUiState(
    val mode: VaultLockMode = VaultLockMode.UNLOCK,
    val enteredPin: String = "",
    val confirmPin: String = "",
    val isConfirmStep: Boolean = false,
    val errorMessage: String? = null,
    val isUnlocked: Boolean = false,
    val isChecking: Boolean = true,
    val isBiometricAvailable: Boolean = false,
    val isBiometricEnabled: Boolean = false
)

@HiltViewModel
class VaultLockViewModel @Inject constructor(
    private val appLockRepository: AppLockRepository,
    private val biometricHelper: BiometricHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(VaultLockUiState())
    val uiState: StateFlow<VaultLockUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val hasVaultPin = appLockRepository.hasVaultPinSet()
            val isBiometricAvailable = biometricHelper.isBiometricAvailable()
            val isBiometricEnabled = appLockRepository.isVaultBiometricEnabled()
                .let { flow ->
                    var result = false
                    flow.collect {
                        result = it
                        return@collect
                    }
                    result
                }

            _uiState.value = _uiState.value.copy(
                mode = if (hasVaultPin) VaultLockMode.UNLOCK else VaultLockMode.SETUP,
                isChecking = false,
                isBiometricAvailable = isBiometricAvailable,
                isBiometricEnabled = isBiometricEnabled
            )

            // Biometric enabled থাকলে অটো prompt
            if (hasVaultPin && isBiometricEnabled && isBiometricAvailable) {
                _uiState.value = _uiState.value.copy(isBiometricEnabled = true)
            }
        }
    }

    fun onDigitPress(digit: String) {
        val current = _uiState.value
        if (current.mode == VaultLockMode.SETUP && current.isConfirmStep) {
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
        if (current.mode == VaultLockMode.SETUP && current.isConfirmStep) {
            onConfirmPinChanged(current.confirmPin.dropLast(1))
        } else {
            onPinChanged(current.enteredPin.dropLast(1))
        }
    }

    private fun onPinChanged(pin: String) {
        _uiState.value = _uiState.value.copy(enteredPin = pin, errorMessage = null)
        if (pin.length == 4) {
            when (_uiState.value.mode) {
                VaultLockMode.SETUP -> _uiState.value =
                    _uiState.value.copy(isConfirmStep = true)
                VaultLockMode.UNLOCK -> verifyAndUnlock(pin)
            }
        }
    }

    private fun onConfirmPinChanged(pin: String) {
        _uiState.value = _uiState.value.copy(confirmPin = pin, errorMessage = null)
        if (pin.length == 4) confirmSetup()
    }

    private fun confirmSetup() {
        val current = _uiState.value
        if (current.enteredPin == current.confirmPin) {
            viewModelScope.launch {
                appLockRepository.setVaultPin(current.enteredPin)
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
            val isCorrect = appLockRepository.verifyVaultPin(pin)
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

    fun showBiometricPrompt(activity: FragmentActivity) {
        biometricHelper.showPrompt(
            activity = activity,
            title = "Vault Unlock",
            subtitle = "Fingerprint বা Face দিয়ে Vault খুলুন"
        ) { result ->
            when (result) {
                is BiometricResult.Success -> {
                    _uiState.value = _uiState.value.copy(isUnlocked = true)
                }
                is BiometricResult.Failed -> {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Biometric যাচাই ব্যর্থ"
                    )
                }
                is BiometricResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = if (result.message.contains("cancel", ignoreCase = true))
                            null else result.message
                    )
                }
            }
        }
    }
}