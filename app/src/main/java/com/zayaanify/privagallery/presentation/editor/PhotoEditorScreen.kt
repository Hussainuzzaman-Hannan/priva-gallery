package com.zayaanify.privagallery.presentation.editor

import android.app.Activity
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yalantis.ucrop.UCrop
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoEditorScreen(
    onBackClick: () -> Unit,
    viewModel: PhotoEditorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // UCrop launcher
    val cropLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { intent ->
                val croppedUri = UCrop.getOutput(intent)
                croppedUri?.let { viewModel.onCropResult(it) }
            }
        }
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onBackClick()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = { Text("Edit", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "ফিরে যান",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.resetEdits() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "রিসেট",
                            tint = Color.White
                        )
                    }
                    IconButton(
                        onClick = { viewModel.savePhoto() },
                        enabled = !uiState.isSaving
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White
                            )
                        } else {
                            Icon(
                                Icons.Default.Save,
                                contentDescription = "সেভ করুন",
                                tint = Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.8f)
                )
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.9f))
            ) {
                // Tab content
                when (uiState.activeTab) {
                    EditorTab.ADJUST -> AdjustPanel(
                        brightness = uiState.brightness,
                        contrast = uiState.contrast,
                        saturation = uiState.saturation,
                        onBrightnessChange = { viewModel.setBrightness(it) },
                        onContrastChange = { viewModel.setContrast(it) },
                        onSaturationChange = { viewModel.setSaturation(it) }
                    )
                    EditorTab.FILTER -> FilterPanel(
                        selectedFilter = uiState.selectedFilter,
                        previewBitmap = uiState.editedBitmap,
                        onFilterSelect = { viewModel.setFilter(it) }
                    )
                    EditorTab.CROP -> CropPanel(
                        onRotateLeft = { viewModel.rotateLeft() },
                        onRotateRight = { viewModel.rotateRight() },
                        onCropClick = {
                            val sourceUri = Uri.parse(uiState.sourceUri)
                            val destFile = File(
                                context.cacheDir,
                                "cropped_${System.currentTimeMillis()}.jpg"
                            )
                            val destUri = Uri.fromFile(destFile)

                            val cropIntent = UCrop.of(sourceUri, destUri)
                                .withOptions(
                                    UCrop.Options().apply {
                                        // Free crop — কোনো fixed ratio নেই
                                        setFreeStyleCropEnabled(true)
                                        // Toolbar রঙ
                                        setToolbarColor(android.graphics.Color.BLACK)
                                        setStatusBarColor(android.graphics.Color.BLACK)
                                        setToolbarWidgetColor(android.graphics.Color.WHITE)
                                        setActiveControlsWidgetColor(
                                            android.graphics.Color.parseColor("#6200EE")
                                        )
                                        // Grid লাইন দেখানো
                                        setCropGridColumnCount(3)
                                        setCropGridRowCount(3)
                                        setShowCropGrid(true)
                                        setShowCropFrame(true)
                                        // Hide aspect ratio options — নিজে টেনে করবে
                                        setHideBottomControls(false)
                                        setLogoColor(android.graphics.Color.BLACK)
                                    }
                                )
                                .withMaxResultSize(4096, 4096)
                                .getIntent(context)

                            cropLauncher.launch(cropIntent)
                        }
                    )
                }

                // Bottom navigation tabs
                NavigationBar(
                    containerColor = Color.Black,
                    contentColor = Color.White
                ) {
                    NavigationBarItem(
                        selected = uiState.activeTab == EditorTab.ADJUST,
                        onClick = { viewModel.setActiveTab(EditorTab.ADJUST) },
                        icon = {
                            Text("🎨", style = MaterialTheme.typography.titleMedium)
                        },
                        label = { Text("Adjust", color = Color.White) }
                    )
                    NavigationBarItem(
                        selected = uiState.activeTab == EditorTab.FILTER,
                        onClick = { viewModel.setActiveTab(EditorTab.FILTER) },
                        icon = {
                            Text("✨", style = MaterialTheme.typography.titleMedium)
                        },
                        label = { Text("Filter", color = Color.White) }
                    )
                    NavigationBarItem(
                        selected = uiState.activeTab == EditorTab.CROP,
                        onClick = { viewModel.setActiveTab(EditorTab.CROP) },
                        icon = {
                            Icon(
                                Icons.Default.Crop,
                                contentDescription = null,
                                tint = if (uiState.activeTab == EditorTab.CROP)
                                    MaterialTheme.colorScheme.primary
                                else Color.White
                            )
                        },
                        label = { Text("Crop", color = Color.White) }
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            when {
                uiState.isLoading -> CircularProgressIndicator(color = Color.White)
                uiState.editedBitmap != null -> {
                    Image(
                        bitmap = uiState.editedBitmap!!.asImageBitmap(),
                        contentDescription = "Edit প্রিভিউ",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun AdjustPanel(
    brightness: Float,
    contrast: Float,
    saturation: Float,
    onBrightnessChange: (Float) -> Unit,
    onContrastChange: (Float) -> Unit,
    onSaturationChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        SliderRow(
            label = "Brightness",
            value = brightness,
            valueRange = -1f..1f,
            onValueChange = onBrightnessChange,
            displayValue = "${(brightness * 100).toInt()}"
        )
        SliderRow(
            label = "Contrast",
            value = contrast,
            valueRange = 0f..2f,
            onValueChange = onContrastChange,
            displayValue = "${((contrast - 1f) * 100).toInt()}"
        )
        SliderRow(
            label = "Saturation",
            value = saturation,
            valueRange = 0f..2f,
            onValueChange = onSaturationChange,
            displayValue = "${((saturation - 1f) * 100).toInt()}"
        )
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    displayValue: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color.White,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(80.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = displayValue,
            color = Color.White,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(36.dp),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun FilterPanel(
    selectedFilter: PhotoFilter,
    previewBitmap: Bitmap?,
    onFilterSelect: (PhotoFilter) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(PhotoFilter.entries) { filter ->
            FilterItem(
                filter = filter,
                isSelected = filter == selectedFilter,
                bitmap = previewBitmap,
                onClick = { onFilterSelect(filter) }
            )
        }
    }
}

@Composable
private fun FilterItem(
    filter: PhotoFilter,
    isSelected: Boolean,
    bitmap: Bitmap?,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(
                    width = if (isSelected) 2.dp else 0.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                    else Color.Transparent,
                    shape = RoundedCornerShape(8.dp)
                )
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = filter.displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.DarkGray)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = filter.displayName,
            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun CropPanel(
    onRotateLeft: () -> Unit,
    onRotateRight: () -> Unit,
    onCropClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // বাম দিকে ঘোরানো
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(
                onClick = onRotateLeft,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    Icons.Default.RotateLeft,
                    contentDescription = "বাম দিকে ঘোরান",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
            Text(
                "বাম",
                color = Color.White,
                style = MaterialTheme.typography.labelSmall
            )
        }

        // ক্রপ বাটন
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(
                onClick = onCropClick,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    Icons.Default.Crop,
                    contentDescription = "ক্রপ করুন",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }
            Text(
                "ক্রপ",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }

        // ডান দিকে ঘোরানো
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(
                onClick = onRotateRight,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    Icons.Default.RotateRight,
                    contentDescription = "ডান দিকে ঘোরান",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
            Text(
                "ডান",
                color = Color.White,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}