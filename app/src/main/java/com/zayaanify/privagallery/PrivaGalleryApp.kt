package com.zayaanify.privagallery

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.zayaanify.privagallery.data.local.crypto.EncryptedImageFetcher
import com.zayaanify.privagallery.data.local.crypto.EncryptedImageModel
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class PrivaGalleryApp : Application(), ImageLoaderFactory {

    @Inject
    lateinit var encryptedImageFetcherFactory: EncryptedImageFetcher.Factory

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(encryptedImageFetcherFactory)
            }
            .build()
    }
}