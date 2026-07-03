package com.zayaanify.privagallery.presentation.albumdetail

import androidx.activity.result.IntentSenderRequest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.zayaanify.privagallery.domain.model.Photo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    onBackClick: () -> Unit,
    onPhotoClick: (mediaStoreId: Long) -> Unit,
    viewModel: AlbumDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) {
        viewModel.consumeDeletePendingIntent()
    }

    LaunchedEffect(uiState.deletePendingIntent) {
        uiState.deletePendingIntent?.let { pendingIntent ->
            val request = IntentSenderRequest.Builder(pendingIntent.intentSender).build()
            deleteLauncher.launch(request)
        }
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
            if (uiState.isSelectionMode) {
                SelectionTopBar(
                    selectedCount = uiState.selectedIds.size,
                    onClearSelection = { viewModel.clearSelection() },
                    onDeleteClick = { viewModel.requestDeleteSelected() },
                    onMoveToVaultClick = { viewModel.moveSelectedToVault() }
                )
            } else {
                TopAppBar(
                    title = { Text(uiState.albumName) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "ফিরে যান")
                        }
                    }
                )
            }
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.photos.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("এই অ্যালবামে কোনো ছবি নেই")
                }
            }

            else -> {
                PhotoGrid(
                    photos = uiState.photos,
                    selectedIds = uiState.selectedIds,
                    isSelectionMode = uiState.isSelectionMode,
                    contentPadding = padding,
                    onPhotoClick = { mediaStoreId ->
                        if (uiState.isSelectionMode) {
                            viewModel.onPhotoTapInSelectionMode(mediaStoreId)
                        } else {
                            onPhotoClick(mediaStoreId)
                        }
                    },
                    onPhotoLongPress = { viewModel.onPhotoLongPress(it) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionTopBar(
    selectedCount: Int,
    onClearSelection: () -> Unit,
    onDeleteClick: () -> Unit,
    onMoveToVaultClick: () -> Unit
) {
    TopAppBar(
        title = { Text("$selectedCount সিলেক্টেড") },
        navigationIcon = {
            IconButton(onClick = onClearSelection) {
                Icon(Icons.Default.Close, contentDescription = "বাতিল")
            }
        },
        actions = {
            IconButton(onClick = onMoveToVaultClick) {
                Icon(Icons.Default.Lock, contentDescription = "Vault-এ নিন")
            }
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Default.Delete, contentDescription = "ডিলিট")
            }
        }
    )
}

@Composable
private fun PhotoGrid(
    photos: List<Photo>,
    selectedIds: Set<Long>,
    isSelectionMode: Boolean,
    contentPadding: PaddingValues,
    onPhotoClick: (Long) -> Unit,
    onPhotoLongPress: (Long) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(
            start = 4.dp,
            end = 4.dp,
            top = contentPadding.calculateTopPadding() + 4.dp,
            bottom = 4.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(photos, key = { it.mediaStoreId }) { photo ->
            PhotoGridItem(
                photo = photo,
                isSelected = photo.mediaStoreId in selectedIds,
                isSelectionMode = isSelectionMode,
                onClick = { onPhotoClick(photo.mediaStoreId) },
                onLongPress = { onPhotoLongPress(photo.mediaStoreId) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoGridItem(
    photo: Photo,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
    ) {
        AsyncImage(
            model = photo.contentUri,
            contentDescription = photo.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        if (photo.isVideo) {
            Icon(
                imageVector = Icons.Default.PlayCircle,
                contentDescription = "ভিডিও",
                tint = Color.White,
                modifier = Modifier.align(Alignment.Center).size(28.dp)
            )
        }

        if (isSelectionMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else Color.Black.copy(alpha = 0.4f)
                    )
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "সিলেক্টেড",
                        tint = Color.White,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}