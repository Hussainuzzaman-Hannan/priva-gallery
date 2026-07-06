package com.zayaanify.privagallery.presentation.gallery

import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.ManageSearch
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.zayaanify.privagallery.domain.model.Album

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    onAlbumClick: (bucketId: String, albumName: String) -> Unit,
    onVaultClick: () -> Unit,
    onDuplicateClick: () -> Unit,
    onCategoryClick: () -> Unit,
    onOcrSearchClick: () -> Unit,
    onBackupClick: () -> Unit,
    viewModel: GalleryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("PrivaGallery") },
                actions = {
                    IconButton(onClick = onOcrSearchClick) {
                        Icon(
                            imageVector = Icons.Default.ManageSearch,
                            contentDescription = "স্ক্রিনশট সার্চ"
                        )
                    }
                    IconButton(onClick = onCategoryClick) {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = "ক্যাটেগরি"
                        )
                    }
                    IconButton(onClick = onDuplicateClick) {
                        Icon(
                            imageVector = Icons.Default.FindReplace,
                            contentDescription = "Duplicate খুঁজুন"
                        )
                    }
                    IconButton(onClick = onBackupClick) {
                        Icon(
                            imageVector = Icons.Default.Backup,
                            contentDescription = "Backup"
                        )
                    }
                    IconButton(onClick = onVaultClick) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Vault খুলুন"
                        )
                    }
                }
            )
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

            uiState.errorMessage != null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "অ্যালবাম লোড করা যায়নি: ${uiState.errorMessage}",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            }

            uiState.albums.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("কোনো ছবি পাওয়া যায়নি")
                }
            }

            else -> {
                AlbumGrid(
                    albums = uiState.albums,
                    onAlbumClick = onAlbumClick,
                    contentPadding = padding
                )
            }
        }
    }
}

@Composable
private fun AlbumGrid(
    albums: List<Album>,
    onAlbumClick: (bucketId: String, albumName: String) -> Unit,
    contentPadding: PaddingValues
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(
            start = 12.dp,
            end = 12.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = 12.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(albums, key = { it.bucketId }) { album ->
            AlbumGridItem(
                album = album,
                onClick = { onAlbumClick(album.bucketId, album.displayName) }
            )
        }
    }
}

@Composable
private fun AlbumGridItem(album: Album, onClick: () -> Unit) {
    Column(modifier = Modifier.clickable(onClick = onClick)) {
        AsyncImage(
            model = album.coverPhotoUri,
            contentDescription = album.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
        )
        Text(
            text = album.displayName,
            maxLines = 1,
            modifier = Modifier.padding(top = 6.dp, start = 2.dp)
        )
        Text(
            text = "${album.photoCount} আইটেম",
            maxLines = 1,
            modifier = Modifier.padding(start = 2.dp)
        )
    }
}