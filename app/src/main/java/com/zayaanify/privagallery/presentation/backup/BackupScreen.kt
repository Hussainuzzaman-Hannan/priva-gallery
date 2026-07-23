package com.zayaanify.privagallery.presentation.backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    onBackClick: () -> Unit,
    viewModel: BackupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Backup ফাইল সেভ করার জন্য SAF launcher
    val createFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        uri?.let { viewModel.createBackup(it) }
    }

    // Backup ফাইল সিলেক্ট করার জন্য SAF launcher
    val openFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.onBackupFileSelected(it) }
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (uiState.mode) {
                            BackupScreenMode.HOME -> "Backup & Restore"
                            BackupScreenMode.CREATE -> "Backup তৈরি করুন"
                            BackupScreenMode.RESTORE -> "Backup Restore করুন"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.mode == BackupScreenMode.HOME) {
                            onBackClick()
                        } else {
                            viewModel.setMode(BackupScreenMode.HOME)
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "ফিরে যান")
                    }
                }
            )
        }
    ) { padding ->
        when (uiState.mode) {
            BackupScreenMode.HOME -> {
                HomeView(
                    onCreateClick = { viewModel.setMode(BackupScreenMode.CREATE) },
                    onRestoreClick = { viewModel.setMode(BackupScreenMode.RESTORE) },
                    modifier = Modifier.padding(padding)
                )
            }

            BackupScreenMode.CREATE -> {
                if (uiState.isSuccess) {
                    SuccessView(
                        message = "Backup সফলভাবে তৈরি হয়েছে",
                        onDoneClick = { viewModel.setMode(BackupScreenMode.HOME) },
                        modifier = Modifier.padding(padding)
                    )
                } else {
                    CreateBackupView(
                        password = uiState.password,
                        confirmPassword = uiState.confirmPassword,
                        isPasswordVisible = uiState.isPasswordVisible,
                        isProcessing = uiState.isProcessing,
                        progress = uiState.progress,
                        onPasswordChange = { viewModel.onPasswordChange(it) },
                        onConfirmPasswordChange = { viewModel.onConfirmPasswordChange(it) },
                        onToggleVisibility = { viewModel.togglePasswordVisibility() },
                        onCreateClick = {
                            val fileName = "PrivaGallery_Backup_${
                                SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                                    .format(Date())
                            }.pgbak"
                            createFileLauncher.launch(fileName)
                        },
                        modifier = Modifier.padding(padding)
                    )
                }
            }

            BackupScreenMode.RESTORE -> {
                if (uiState.isSuccess) {
                    SuccessView(
                        message = "Restore সফলভাবে সম্পন্ন হয়েছে",
                        onDoneClick = { viewModel.setMode(BackupScreenMode.HOME) },
                        modifier = Modifier.padding(padding)
                    )
                } else {
                    RestoreBackupView(
                        password = uiState.password,
                        isPasswordVisible = uiState.isPasswordVisible,
                        isProcessing = uiState.isProcessing,
                        progress = uiState.progress,
                        selectedFileUri = uiState.selectedFileUri,
                        backupInfo = uiState.backupInfo,
                        onPasswordChange = { viewModel.onPasswordChange(it) },
                        onToggleVisibility = { viewModel.togglePasswordVisibility() },
                        onSelectFileClick = {
                            openFileLauncher.launch(arrayOf("application/octet-stream", "*/*"))
                        },
                        onReadInfoClick = {
                            uiState.selectedFileUri?.let { viewModel.readBackupInfo(it) }
                        },
                        onRestoreClick = { viewModel.restoreBackup() },
                        modifier = Modifier.padding(padding)
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeView(
    onCreateClick: () -> Unit,
    onRestoreClick: () -> Unit,
    modifier: Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Backup,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Vault Backup",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Vault-এর সব এনক্রিপ্টেড ফটো\nপাসওয়ার্ড দিয়ে সেভ বা restore করুন",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = onCreateClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Backup, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("New Backup তৈরি করুন")
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onRestoreClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Restore, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Backup থেকে Restore করুন")
        }
    }
}

@Composable
private fun CreateBackupView(
    password: String,
    confirmPassword: String,
    isPasswordVisible: Boolean,
    isProcessing: Boolean,
    progress: Float,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onToggleVisibility: () -> Unit,
    onCreateClick: () -> Unit,
    modifier: Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text(
            "Backup পাসওয়ার্ড",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "এই পাসওয়ার্ড দিয়ে backup ফাইল encrypt হবে।\nপাসওয়ার্ড ভুলে গেলে backup আর খোলা যাবে না।",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("পাসওয়ার্ড (কমপক্ষে ৬ অক্ষর)") },
            visualTransformation = if (isPasswordVisible)
                VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = onToggleVisibility) {
                    Icon(
                        if (isPasswordVisible) Icons.Default.VisibilityOff
                        else Icons.Default.Visibility,
                        contentDescription = null
                    )
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = onConfirmPasswordChange,
            label = { Text("পাসওয়ার্ড নিশ্চিত করুন") },
            visualTransformation = if (isPasswordVisible)
                VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (isProcessing) {
            ProcessingView(progress = progress, message = "Backup তৈরি হচ্ছে...")
        } else {
            Button(
                onClick = onCreateClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = password.isNotBlank() && confirmPassword.isNotBlank()
            ) {
                Icon(Icons.Default.Backup, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Backup তৈরি করুন")
            }
        }
    }
}

@Composable
private fun RestoreBackupView(
    password: String,
    isPasswordVisible: Boolean,
    isProcessing: Boolean,
    progress: Float,
    selectedFileUri: Uri?,
    backupInfo: com.zayaanify.privagallery.domain.repository.BackupInfo?,
    onPasswordChange: (String) -> Unit,
    onToggleVisibility: () -> Unit,
    onSelectFileClick: () -> Unit,
    onReadInfoClick: () -> Unit,
    onRestoreClick: () -> Unit,
    modifier: Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        // ফাইল সিলেক্ট
        Text(
            "Backup ফাইল",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onSelectFileClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.FolderOpen, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                if (selectedFileUri != null) "ফাইল সিলেক্ট হয়েছে ✓"
                else "Backup ফাইল সিলেক্ট করুন (.pgbak)"
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // পাসওয়ার্ড
        Text(
            "পাসওয়ার্ড",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Backup পাসওয়ার্ড") },
            visualTransformation = if (isPasswordVisible)
                VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = onToggleVisibility) {
                    Icon(
                        if (isPasswordVisible) Icons.Default.VisibilityOff
                        else Icons.Default.Visibility,
                        contentDescription = null
                    )
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Backup info দেখানো
        if (selectedFileUri != null && password.isNotBlank()) {
            OutlinedButton(
                onClick = onReadInfoClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Backup Info যাচাই করুন")
            }
        }

        // Backup info card
        if (backupInfo != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Backup Info",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "ফটো: ${backupInfo.photoCount} টা",
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "তৈরির Date: ${
                            SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                                .format(Date(backupInfo.createdAt))
                        }",
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "App version: ${backupInfo.appVersion}",
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (isProcessing) {
            ProcessingView(progress = progress, message = "Restore হচ্ছে...")
        } else {
            Button(
                onClick = onRestoreClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedFileUri != null && password.isNotBlank()
            ) {
                Icon(Icons.Default.Restore, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Restore শুরু করুন")
            }
        }
    }
}

@Composable
private fun ProcessingView(progress: Float, message: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(message, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "${(progress * 100).toInt()}% সম্পন্ন",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SuccessView(
    message: String,
    onDoneClick: () -> Unit,
    modifier: Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(message, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onDoneClick) {
                Text("সম্পন্ন")
            }
        }
    }
}