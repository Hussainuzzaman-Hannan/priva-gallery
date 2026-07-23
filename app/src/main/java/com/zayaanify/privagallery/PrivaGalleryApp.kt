package com.zayaanify.privagallery

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.core.view.WindowCompat
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import com.yalantis.ucrop.UCropActivity
import com.zayaanify.privagallery.data.local.crypto.EncryptedImageFetcher
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class PrivaGalleryApp : Application(), ImageLoaderFactory {

    @Inject
    lateinit var encryptedImageFetcherFactory: EncryptedImageFetcher.Factory

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                // UCrop-এর টুলবার/বাটন যাতে স্ট্যাটাসবারের নিচে/পেছনে
                // ঢুকে না যায়, তাই এই Activity-র জন্য জোর করে
                // edge-to-edge বন্ধ করা হচ্ছে — থিমের
                // windowOptOutEdgeToEdgeEnforcement অ্যাট্রিবিউট
                // Android 16-এ আর সম্মানিত হয় না বলে এখানে কোড দিয়ে করতে হচ্ছে।
                if (activity is UCropActivity) {
                    WindowCompat.setDecorFitsSystemWindows(activity.window, true)
                }
            }

            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

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