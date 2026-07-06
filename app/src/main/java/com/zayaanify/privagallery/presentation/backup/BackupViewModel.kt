package com.zayaanify.privagallery.presentation.backup

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zayaanify.privagallery.domain.repository.BackupInfo
import com.zayaanify.privagallery.domain.usecase.CreateBackupUseCase
import com.zayaanify.privagallery.domain.usecase.RestoreBackupUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class BackupScreenMode { HOME, CREATE, RESTORE }

data class BackupUiState(
    val mode: BackupScreenMode = BackupScreenMode.HOME,
    val password: String = "",
    val confirmPassword: String = "",
    val isPasswordVisible: Boolean = false,
    val isProcessing: Boolean = false,
    val progress: Float = 0f,
    val backupInfo: BackupInfo? = null,
    val selectedFileUri: Uri? = null,
    val message: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val createBackupUseCase: CreateBackupUseCase,
    private val restoreBackupUseCase: RestoreBackupUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    fun setMode(mode: BackupScreenMode) {
        _uiState.value = BackupUiState(mode = mode)
    }

    fun onPasswordChange(password: String) {
        _uiState.value = _uiState.value.copy(password = password)
    }

    fun onConfirmPasswordChange(password: String) {
        _uiState.value = _uiState.value.copy(confirmPassword = password)
    }

    fun togglePasswordVisibility() {
        _uiState.value = _uiState.value.copy(
            isPasswordVisible = !_uiState.value.isPasswordVisible
        )
    }

    fun onBackupFileSelected(uri: Uri) {
        _uiState.value = _uiState.value.copy(selectedFileUri = uri)
    }

    /** Backup ফাইলের info পড়া — password সঠিক কিনা যাচাই করার জন্য। */
    fun readBackupInfo(uri: Uri) {
        viewModelScope.launch {
            val password = _uiState.value.password
            if (password.isBlank()) return@launch

            _uiState.value = _uiState.value.copy(isProcessing = true)
            restoreBackupUseCase.readInfo(uri, password)
                .onSuccess { info ->
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        backupInfo = info,
                        selectedFileUri = uri
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        message = e.message ?: "ফাইল পড়া যায়নি"
                    )
                }
        }
    }

    fun createBackup(destinationUri: Uri) {
        val current = _uiState.value
        if (current.password.isBlank()) {
            _uiState.value = current.copy(message = "পাসওয়ার্ড দিন")
            return
        }
        if (current.password != current.confirmPassword) {
            _uiState.value = current.copy(message = "পাসওয়ার্ড মিলেনি")
            return
        }
        if (current.password.length < 6) {
            _uiState.value = current.copy(message = "পাসওয়ার্ড কমপক্ষে ৬ অক্ষরের হতে হবে")
            return
        }

        viewModelScope.launch {
            _uiState.value = current.copy(isProcessing = true, progress = 0f)
            createBackupUseCase(
                destinationUri = destinationUri,
                password = current.password,
                onProgress = { progress ->
                    _uiState.value = _uiState.value.copy(progress = progress)
                }
            )
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        isSuccess = true,
                        message = "Backup সফলভাবে তৈরি হয়েছে"
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        message = "Backup ব্যর্থ: ${e.message}"
                    )
                }
        }
    }

    fun restoreBackup() {
        val current = _uiState.value
        val uri = current.selectedFileUri ?: run {
            _uiState.value = current.copy(message = "ফাইল সিলেক্ট করুন")
            return
        }
        if (current.password.isBlank()) {
            _uiState.value = current.copy(message = "পাসওয়ার্ড দিন")
            return
        }

        viewModelScope.launch {
            _uiState.value = current.copy(isProcessing = true, progress = 0f)
            restoreBackupUseCase(
                sourceUri = uri,
                password = current.password,
                onProgress = { progress ->
                    _uiState.value = _uiState.value.copy(progress = progress)
                }
            )
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        isSuccess = true,
                        message = "Restore সফলভাবে সম্পন্ন হয়েছে"
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        message = "Restore ব্যর্থ: ${e.message}"
                    )
                }
        }
    }

    fun consumeMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}