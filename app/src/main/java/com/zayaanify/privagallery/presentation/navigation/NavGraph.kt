package com.zayaanify.privagallery.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.zayaanify.privagallery.presentation.albumdetail.AlbumDetailScreen
import com.zayaanify.privagallery.presentation.backup.BackupScreen
import com.zayaanify.privagallery.presentation.category.CategoryScreen
import com.zayaanify.privagallery.presentation.duplicate.DuplicateScreen
import com.zayaanify.privagallery.presentation.editor.PhotoEditorScreen
import com.zayaanify.privagallery.presentation.gallery.GalleryScreen
import com.zayaanify.privagallery.presentation.lock.AppLockScreen
import com.zayaanify.privagallery.presentation.ocrsearch.OcrSearchScreen
import com.zayaanify.privagallery.presentation.recyclebin.RecycleBinScreen
import com.zayaanify.privagallery.presentation.settings.SettingsScreen
import com.zayaanify.privagallery.presentation.vault.VaultLockScreen
import com.zayaanify.privagallery.presentation.vault.VaultScreen
import com.zayaanify.privagallery.presentation.viewer.PhotoViewerScreen
import com.zayaanify.privagallery.presentation.viewer.VideoPlayerScreen
import java.net.URLDecoder
import java.net.URLEncoder

object Routes {
    const val LOCK = "lock"
    const val GALLERY = "gallery"
    const val VAULT = "vault"
    const val DUPLICATE = "duplicate"
    const val CATEGORY = "category"
    const val OCR_SEARCH = "ocr_search"
    const val BACKUP = "backup"
    const val SETTINGS = "settings"
    const val RECYCLE_BIN = "recycle_bin"
    const val ALBUM_DETAIL = "album_detail/{bucketId}/{albumName}"
    const val PHOTO_VIEWER = "photo_viewer/{bucketId}/{mediaStoreId}"
    const val VIDEO_PLAYER = "video_player/{videoUri}"
    const val PHOTO_EDITOR = "photo_editor/{photoUri}"

    fun albumDetail(bucketId: String, albumName: String): String {
        val encodedName = URLEncoder.encode(albumName, "UTF-8")
        return "album_detail/$bucketId/$encodedName"
    }

    fun photoViewer(bucketId: String, mediaStoreId: Long): String {
        return "photo_viewer/$bucketId/$mediaStoreId"
    }

    fun videoPlayer(videoUri: String): String {
        val encodedUri = URLEncoder.encode(videoUri, "UTF-8")
        return "video_player/$encodedUri"
    }

    fun photoEditor(photoUri: String): String {
        val encodedUri = URLEncoder.encode(photoUri, "UTF-8")
        return "photo_editor/$encodedUri"
    }
}

@Composable
fun PrivaGalleryNavHost(
    navController: NavHostController = rememberNavController()
) {
    NavHost(navController = navController, startDestination = Routes.LOCK) {

        composable(Routes.LOCK) {
            AppLockScreen(
                onUnlocked = {
                    navController.navigate(Routes.GALLERY) {
                        popUpTo(Routes.LOCK) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.GALLERY) {
            GalleryScreen(
                onAlbumClick = { bucketId, albumName ->
                    navController.navigate(Routes.albumDetail(bucketId, albumName))
                },
                onVaultClick = { navController.navigate(Routes.VAULT) },
                onDuplicateClick = { navController.navigate(Routes.DUPLICATE) },
                onCategoryClick = { navController.navigate(Routes.CATEGORY) },
                onOcrSearchClick = { navController.navigate(Routes.OCR_SEARCH) },
                onBackupClick = { navController.navigate(Routes.BACKUP) },
                onSettingsClick = { navController.navigate(Routes.SETTINGS) },
                onRecycleBinClick = { navController.navigate(Routes.RECYCLE_BIN) }
            )
        }

        composable(Routes.VAULT) {
            VaultLockScreen(
                onUnlocked = {
                    navController.navigate("vault_content") {
                        popUpTo(Routes.VAULT) { inclusive = false }
                    }
                }
            )
        }

        composable("vault_content") {
            VaultScreen(onBackClick = { navController.popBackStack() })
        }

        composable(Routes.DUPLICATE) {
            DuplicateScreen(onBackClick = { navController.popBackStack() })
        }

        composable(Routes.CATEGORY) {
            CategoryScreen(onBackClick = { navController.popBackStack() })
        }

        composable(Routes.OCR_SEARCH) {
            OcrSearchScreen(onBackClick = { navController.popBackStack() })
        }

        composable(Routes.BACKUP) {
            BackupScreen(onBackClick = { navController.popBackStack() })
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(onBackClick = { navController.popBackStack() })
        }

        composable(Routes.RECYCLE_BIN) {
            RecycleBinScreen(onBackClick = { navController.popBackStack() })
        }

        composable(
            route = Routes.ALBUM_DETAIL,
            arguments = listOf(
                navArgument("bucketId") { type = NavType.StringType },
                navArgument("albumName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val bucketId = backStackEntry.arguments?.getString("bucketId") ?: ""
            AlbumDetailScreen(
                onBackClick = { navController.popBackStack() },
                onPhotoClick = { mediaStoreId ->
                    navController.navigate(Routes.photoViewer(bucketId, mediaStoreId))
                },
                onVideoClick = { videoUri ->
                    navController.navigate(Routes.videoPlayer(videoUri))
                }
            )
        }

        composable(
            route = Routes.PHOTO_VIEWER,
            arguments = listOf(
                navArgument("bucketId") { type = NavType.StringType },
                navArgument("mediaStoreId") { type = NavType.LongType }
            )
        ) {
            PhotoViewerScreen(
                onBackClick = { navController.popBackStack() },
                onEditClick = { photoUri ->
                    navController.navigate(Routes.photoEditor(photoUri))
                }
            )
        }

        composable(
            route = Routes.VIDEO_PLAYER,
            arguments = listOf(
                navArgument("videoUri") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val encodedUri = backStackEntry.arguments?.getString("videoUri") ?: ""
            val videoUri = URLDecoder.decode(encodedUri, "UTF-8")
            VideoPlayerScreen(
                videoUri = videoUri,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.PHOTO_EDITOR,
            arguments = listOf(
                navArgument("photoUri") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val encodedUri = backStackEntry.arguments?.getString("photoUri") ?: ""
            val photoUri = URLDecoder.decode(encodedUri, "UTF-8")
            PhotoEditorScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}