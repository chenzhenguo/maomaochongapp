package com.maomaochongapp.core.image

import android.content.pm.ApplicationInfo
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.util.DebugLogger
import android.content.Context
import android.util.Log
import coil.annotation.ExperimentalCoilApi
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.ImageLoader as CoilImageLoader

/**
 * Image loader configuration for picture book management
 */
object AppImageLoader {
    private const val TAG = "ImageLoader"
    private var initialized = false
    private var imageLoader: CoilImageLoader? = null

    fun initialize(context: Context) {
        if (initialized) return

        imageLoader = CoilImageLoader.Builder(context)
            .crossfade(true)
            .respectCacheHeaders(false)
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.02) // 2% of available space
                    .build()
            }
            .components {
                if (android.os.Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .apply {
                if ((context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
                    logger { message, throwable ->
                        Log.log(Log.INFO, TAG, throwable, message)
                    }
                }
            }
            .build()

        initialized = true
        Log.d(TAG, "ImageLoader initialized")
    }

    @OptIn(ExperimentalCoilApi::class)
    fun clearCache(context: Context) {
        val loader = imageLoader ?: return
        loader.memoryCache?.clear()
        loader.diskCache?.clear()
        Log.d(TAG, "Image cache cleared for ${context.packageName}")
    }
}
