package com.maomaochongapp.core.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.net.toUri
import java.io.InputStream

/**
 * Utility functions for image processing and validation
 */
object ImageUtils {
    private const val MAX_BITMAP_SIZE = 2048 // Maximum dimension for bitmap loading

    /**
     * Load a downscaled bitmap from URI to avoid OOM errors
     */
    fun loadDownscaledBitmap(context: Context, uri: Uri, maxWidth: Int = MAX_BITMAP_SIZE, maxHeight: Int = MAX_BITMAP_SIZE): Bitmap? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            inputStream?.use { BitmapFactory.decodeStream(it, null, options) }

            val scaleFactor = calculateScaleFactor(options.outWidth, options.outHeight, maxWidth, maxHeight)

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = scaleFactor
                inPreferredConfig = Bitmap.Config.RGB_565
            }

            val finalInputStream = context.contentResolver.openInputStream(uri)
            finalInputStream?.use { BitmapFactory.decodeStream(it, null, decodeOptions) }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Calculate the scale factor for downsampling
     */
    private fun calculateScaleFactor(width: Int, height: Int, maxWidth: Int, maxHeight: Int): Int {
        var scaleFactor = 1
        if (height > maxHeight || width > maxWidth) {
            val widthScale = Math.ceil((width.toFloat() / maxWidth.toFloat()).toDouble()).toInt()
            val heightScale = Math.ceil((height.toFloat() / maxHeight.toFloat()).toDouble()).toInt()
            scaleFactor = Math.max(widthScale, heightScale)
        }
        return scaleFactor
    }

    /**
     * Validate if a URI points to a valid image file
     */
    fun isValidImageUri(context: Context, uri: Uri): Boolean {
        return try {
            val mimeType = context.contentResolver.getType(uri)
            mimeType?.startsWith("image/") == true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get file extension from URI
     */
    fun getExtensionFromUri(uri: Uri): String {
        val path = uri.path ?: ""
        val lastDot = path.lastIndexOf('.')
        return if (lastDot != -1 && lastDot < path.length - 1) {
            path.substring(lastDot + 1).lowercase()
        } else {
            ""
        }
    }
}