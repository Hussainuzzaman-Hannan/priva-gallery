package com.zayaanify.privagallery.presentation.lock

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

@Composable
fun AppLockScreen(
    onUnlocked: () -> Unit,
    viewModel: AppLockViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(uiState.isUnlocked) {
        if (uiState.isUnlocked) onUnlocked()
    }

    // Unlock মোডে biometric available থাকলে অটো prompt দেখানো।
    // এটা এখন Activity-র ON_RESUME lifecycle event-এর সাথে সিঙ্ক করে চলে —
    // আগে শুধু state পরিবর্তনের ভিত্তিতে (LaunchedEffect) কল হতো, যেটা
    // ব্যাকগ্রাউন্ড থেকে ফেরার সময় Activity পুরোপুরি resumed/window-focused
    // হওয়ার আগেই BiometricPrompt.authenticate() কল করে ফেলতে পারত —
    // ফলে সিস্টেম প্রম্পট silently দেখাতে ব্যর্থ হয়ে কোনো callback ছাড়াই
    // UI স্পিনার/আটকে থাকা অবস্থায় থেকে যেত।
    val currentUiState = rememberUpdatedState(uiState)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val state = currentUiState.value
                if (!state.isCheckingInitialState &&
                    !state.isUnlocked &&
                    state.mode == LockScreenMode.UNLOCK &&
                    state.isBiometricEnabled &&
                    state.isBiometricAvailable
                ) {
                    val activity = context as? FragmentActivity
                    activity?.let { viewModel.showBiometricPrompt(it) }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (uiState.isCheckingInitialState) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val title = when {
        uiState.mode == LockScreenMode.SETUP && !uiState.isConfirmStep -> "একটা PIN সেট করুন"
        uiState.mode == LockScreenMode.SETUP && uiState.isConfirmStep -> "PIN আবার লিখুন"
        else -> "PIN দিয়ে আনলক করুন"
    }

    val activePin = if (uiState.mode == LockScreenMode.SETUP && uiState.isConfirmStep) {
        uiState.confirmPin
    } else {
        uiState.enteredPin
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(64.dp))

        Text(text = title, style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(24.dp))

        PinDots(filledCount = activePin.length, totalCount = 4)

        if (uiState.errorMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = uiState.errorMessage ?: "",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Biometric বাটন — শুধু UNLOCK মোডে দেখাবে
        if (uiState.mode == LockScreenMode.UNLOCK &&
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
                    contentDescription = "Biometric আনলক",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        NumberPad(
            onDigit = { viewModel.onDigitPress(it) },
            onBackspace = { viewModel.onBackspace() }
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun PinDots(filledCount: Int, totalCount: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        repeat(totalCount) { index ->
            val filled = index < filledCount
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
}

@Composable
private fun NumberPad(
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9")
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                row.forEach { digit ->
                    NumberPadButton(label = digit, onClick = { onDigit(digit) })
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Spacer(modifier = Modifier.size(64.dp))
            NumberPadButton(label = "0", onClick = { onDigit("0") })
            IconButton(
                onClick = onBackspace,
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "ব্যাকস্পেস"
                )
            }
        }
    }
}

@Composable
private fun NumberPadButton(label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.size(64.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = label, style = MaterialTheme.typography.headlineMedium)
        }
    }
}