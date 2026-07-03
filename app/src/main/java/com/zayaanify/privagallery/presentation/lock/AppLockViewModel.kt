package com.zayaanify.privagallery.presentation.lock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zayaanify.privagallery.domain.repository.AppLockRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val isCheckingInitialState: Boolean = true
)

@HiltViewModel
class AppLockViewModel @Inject constructor(
    private val appLockRepository: AppLockRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppLockUiState())
    val uiState: StateFlow<AppLockUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val hasPin = appLockRepository.hasPinSet()
            _uiState.value = _uiState.value.copy(
                mode = if (hasPin) LockScreenMode.UNLOCK else LockScreenMode.SETUP,
                isCheckingInitialState = false
            )
        }
    }

    fun onDigitPress(digit: String) {
        val current = _uiState.value
        if (current.mode == LockScreenMode.SETUP && current.isConfirmStep) {
            if (current.confirmPin.length < 6) {
                onConfirmPinChanged(current.confirmPin + digit)
            }
        } else {
            if (current.enteredPin.length < 6) {
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
        if (pin.length == 4) { // 4-ডিজিট PIN হলে অটো-সাবমিট, চাইলে 6 করা যায়
            when (_uiState.value.mode) {
                LockScreenMode.SETUP -> moveToConfirmStep()
                LockScreenMode.UNLOCK -> verifyAndUnlock(pin)
            }
        }
    }

    private fun onConfirmPinChanged(pin: String) {
        _uiState.value = _uiState.value.copy(confirmPin = pin, errorMessage = null)
        if (pin.length == 4) {
            confirmSetup()
        }
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
            // মিল না হলে পুরো ফ্লো রিসেট
            _uiState.value = current.copy(
                enteredPin = "",
                confirmPin = "",
                isConfirmStep = false,
                errorMessage = "PIN মিলেনি, আবার চেষ্টা করুন"
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
                    errorMessage = "ভুল PIN, আবার চেষ্টা করুন"
                )
            }
        }
    }
}
