package com.zayaanify.privagallery.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.zayaanify.privagallery.presentation.albumdetail.AlbumDetailScreen
import com.zayaanify.privagallery.presentation.gallery.GalleryScreen
import com.zayaanify.privagallery.presentation.lock.AppLockScreen
import com.zayaanify.privagallery.presentation.vault.VaultScreen
import com.zayaanify.privagallery.presentation.viewer.PhotoViewerScreen
import java.net.URLEncoder

object Routes {
    const val LOCK = "lock"
    const val GALLERY = "gallery"
    const val VAULT = "vault"
    const val ALBUM_DETAIL = "album_detail/{bucketId}/{albumName}"
    const val PHOTO_VIEWER = "photo_viewer/{bucketId}/{mediaStoreId}"

    fun albumDetail(bucketId: String, albumName: String): String {
        val encodedName = URLEncoder.encode(albumName, "UTF-8")
        return "album_detail/$bucketId/$encodedName"
    }

    fun photoViewer(bucketId: String, mediaStoreId: Long): String {
        return "photo_viewer/$bucketId/$mediaStoreId"
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
                onVaultClick = {
                    navController.navigate(Routes.VAULT)
                }
            )
        }

        composable(Routes.VAULT) {
            VaultScreen(
                onBackClick = { navController.popBackStack() }
            )
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
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}