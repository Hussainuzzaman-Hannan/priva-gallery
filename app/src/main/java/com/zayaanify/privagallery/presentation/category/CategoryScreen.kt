package com.zayaanify.privagallery.presentation.category

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Category
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.zayaanify.privagallery.domain.model.Photo
import com.zayaanify.privagallery.domain.model.PhotoCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(
    onBackClick: () -> Unit,
    viewModel: CategoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (uiState.selectedCategory != null)
                            "${uiState.selectedCategory!!.emoji} ${uiState.selectedCategory!!.displayName}"
                        else "ক্যাটেগরি"
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.selectedCategory != null) {
                            viewModel.selectCategory(null)
                        } else {
                            onBackClick()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "ফিরে যান")
                    }
                }
            )
        }
    ) { padding ->
        when {
            uiState.isScanning -> {
                ScanningView(
                    progress = uiState.scanProgress,
                    modifier = Modifier.padding(padding)
                )
            }

            !uiState.hasScanned -> {
                StartScanView(
                    onScanClick = { viewModel.startScan() },
                    modifier = Modifier.padding(padding)
                )
            }

            uiState.selectedCategory != null -> {
                // নির্দিষ্ট ক্যাটেগরির ফটো গ্রিড
                val photos = uiState.categories[uiState.selectedCategory] ?: emptyList()
                CategoryPhotoGrid(
                    photos = photos,
                    contentPadding = padding
                )
            }

            else -> {
                // ক্যাটেগরি লিস্ট
                CategoryList(
                    categories = uiState.categories,
                    onCategoryClick = { viewModel.selectCategory(it) },
                    onRescanClick = { viewModel.startScan() },
                    contentPadding = padding
                )
            }
        }
    }
}

@Composable
private fun StartScanView(onScanClick: () -> Unit, modifier: Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Category,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("ফটো ক্যাটেগরি", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "ML Kit দিয়ে ফটো বিশ্লেষণ করে\nক্যাটেগরিতে ভাগ করা হবে",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onScanClick) {
                Icon(Icons.Default.Category, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("স্ক্যান শুরু করুন")
            }
        }
    }
}

@Composable
private fun ScanningView(progress: Float, modifier: Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            CircularProgressIndicator(modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(24.dp))
            Text("ফটো বিশ্লেষণ হচ্ছে...", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "${(progress * 100).toInt()}% সম্পন্ন",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CategoryList(
    categories: Map<PhotoCategory, List<Photo>>,
    onCategoryClick: (PhotoCategory) -> Unit,
    onRescanClick: () -> Unit,
    contentPadding: PaddingValues
) {
    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories.entries.toList()) { (category, photos) ->
            CategoryCard(
                category = category,
                photos = photos,
                onClick = { onCategoryClick(category) }
            )
        }
        item {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onRescanClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("আবার স্ক্যান করুন")
            }
        }
    }
}

@Composable
private fun CategoryCard(
    category: PhotoCategory,
    photos: List<Photo>,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // কভার ফটো
            AsyncImage(
                model = photos.first().contentUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${category.emoji} ${category.displayName}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "${photos.size} টা ফটো",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CategoryPhotoGrid(
    photos: List<Photo>,
    contentPadding: PaddingValues
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
            AsyncImage(
                model = photo.contentUri,
                contentDescription = photo.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.aspectRatio(1f)
            )
        }
    }
}