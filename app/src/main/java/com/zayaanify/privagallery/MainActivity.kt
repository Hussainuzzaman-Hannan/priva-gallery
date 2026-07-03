package com.zayaanify.privagallery

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zayaanify.privagallery.presentation.navigation.PrivaGalleryNavHost
import com.zayaanify.privagallery.presentation.theme.PrivaGalleryTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PrivaGalleryTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppEntryPoint()
                }
            }
        }
    }
}

/**
 * প্রথমে মিডিয়া পারমিশন চেক করে, না থাকলে রিকোয়েস্ট স্ক্রিন দেখায়।
 * পারমিশন পেলেই মূল নেভিগেশন গ্রাফ (লক স্ক্রিন থেকে শুরু) দেখানো হয়।
 */
@Composable
private fun AppEntryPoint() {
    val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    var hasPermission by remember { mutableStateOf(false) }
    var permissionChecked by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasPermission = results.values.all { it }
        permissionChecked = true
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        permissionLauncher.launch(requiredPermissions)
    }

    when {
        !permissionChecked -> {
            // পারমিশন ডায়ালগ দেখানোর আগে কিছুই রেন্ডার করার দরকার নেই
        }
        hasPermission -> {
            PrivaGalleryNavHost()
        }
        else -> {
            PermissionDeniedScreen(onRetry = { permissionLauncher.launch(requiredPermissions) })
        }
    }
}

@Composable
private fun PermissionDeniedScreen(onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("ছবি দেখার জন্য মিডিয়া পারমিশন প্রয়োজন")
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(8.dp))
            Button(onClick = onRetry) {
                Text("আবার চেষ্টা করুন")
            }
        }
    }
}
