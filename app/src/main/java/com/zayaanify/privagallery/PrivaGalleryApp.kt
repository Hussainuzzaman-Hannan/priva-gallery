package com.zayaanify.privagallery

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import com.zayaanify.privagallery.data.local.crypto.EncryptedImageFetcher
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class PrivaGalleryApp : Application(), ImageLoaderFactory {

    @Inject
    lateinit var encryptedImageFetcherFactory: EncryptedImageFetcher.Factory

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                // এনক্রিপ্টেড vault thumbnail
                add(encryptedImageFetcherFactory)
                // ভিডিও thumbnail — URI থেকে অটো frame বের করবে
                add(VideoFrameDecoder.Factory())
            }
            .build()
    }
}