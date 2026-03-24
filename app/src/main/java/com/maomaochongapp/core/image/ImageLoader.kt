package com.maomaochongapp.core.image

import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.ImageRequest
import coil.util.DebugLogger
import android.content.Context
import android.util.Log
import androidx.core.content.ContextCompat
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder

/**
 * Image loader configuration for picture book management
 */
object ImageLoader {
    private const val TAG = "ImageLoader"
    private var initialized = false

    fun initialize(context: Context) {
        if (initialized) return

        val imageLoader = ImageLoader.Builder(context)
            .availableMemoryPercentage(0.25)
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
                if (BuildConfig.DEBUG) {
                    logger(DebugLogger(Log.INFO))
                }
            }
            .build()

        coil.ImageLoader.setGlobal(imageLoader)
        initialized = true
        Log.d(TAG, "ImageLoader initialized")
    }

    fun clearCache(context: Context) {
        coil.ImageLoader.get(context).clearMemoryCache()
        coil.ImageLoader.get(context).clearDiskCache()
        Log.d(TAG, "Image cache cleared")
    }
}