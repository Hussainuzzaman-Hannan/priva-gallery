package com.zayaanify.privagallery.presentation.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun VaultLockScreen(
    onUnlocked: () -> Unit,
    viewModel: VaultLockViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uiState.isUnlocked) {
        if (uiState.isUnlocked) onUnlocked()
    }

    // Biometric enabled থাকলে অটো prompt
    LaunchedEffect(uiState.isChecking, uiState.isBiometricEnabled) {
        if (!uiState.isChecking &&
            uiState.mode == VaultLockMode.UNLOCK &&
            uiState.isBiometricEnabled &&
            uiState.isBiometricAvailable
        ) {
            val activity = context as? FragmentActivity
            activity?.let { viewModel.showBiometricPrompt(it) }
        }
    }

    if (uiState.isChecking) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val title = when {
        uiState.mode == VaultLockMode.SETUP && !uiState.isConfirmStep ->
            "Vault PIN সেট করুন"
        uiState.mode == VaultLockMode.SETUP && uiState.isConfirmStep ->
            "PIN আবার লিখুন"
        else -> "Vault Unlock করুন"
    }

    val activePin = if (uiState.mode == VaultLockMode.SETUP && uiState.isConfirmStep) {
        uiState.confirmPin
    } else {
        uiState.enteredPin
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = title, style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(24.dp))

        // PIN dots
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            repeat(4) { index ->
                val filled = index < activePin.length
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(
                            color = if (filled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape
                        )
                )
            }
        }

        if (uiState.errorMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = uiState.errorMessage ?: "",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Biometric বাটন
        if (uiState.mode == VaultLockMode.UNLOCK &&
            uiState.isBiometricEnabled &&
            uiState.isBiometricAvailable
        ) {
            IconButton(
                onClick = {
                    val activity = context as? FragmentActivity
                    activity?.let { viewModel.showBiometricPrompt(it) }
                },
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = "Biometric Unlock",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Number Pad
        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9")
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    row.forEach { digit ->
                        Surface(
                            onClick = { viewModel.onDigitPress(digit) },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = digit,
                                    style = MaterialTheme.typography.headlineMedium
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Spacer(modifier = Modifier.size(64.dp))
                Surface(
                    onClick = { viewModel.onDigitPress("0") },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = "0", style = MaterialTheme.typography.headlineMedium)
                    }
                }
                IconButton(
                    onClick = { viewModel.onBackspace() },
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = "ব্যাকস্পেস"
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}