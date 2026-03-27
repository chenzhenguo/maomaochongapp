package com.maomaochongapp.picturebook.core.image

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.webkit.MimeTypeMap
import java.io.IOException

/**
 * Data class holding image information
 */
data class ImageInfo(
    val fileName: String?,
    val mimeType: String?,
    val fileSize: Long,
    val width: Int,
    val height: Int,
)

/**
 * Utility class for image operations
 */
class ImageUtils {

    /**
     * Get image information from URI
     *
     * @param context Application context
     * @param uri Image URI
     * @return ImageInfo with metadata, or default values if unable to read
     */
    fun getImageInfo(context: Context, uri: Uri): ImageInfo {
        var fileName: String? = null
        var mimeType: String? = null
        var fileSize: Long = 0L
        var width: Int = 0
        var height: Int = 0

        try {
            // Get file name from URI
            fileName = uri.lastPathSegment ?: getFileNameFromUri(context, uri)

            // Get MIME type from URI or file extension
            mimeType = context.contentResolver.getType(uri)
            if (mimeType == null) {
                mimeType = getMimeTypeFromFileName(fileName)
            }

            // Get file size
            fileSize = getFileSize(context, uri)

            // Get image dimensions
            val dimensions = getImageDimensions(context, uri)
            width = dimensions.first
            height = dimensions.second
        } catch (e: Exception) {
            // Return default values on error
            // In a real app, you might want to log this error
        }

        return ImageInfo(
            fileName = fileName,
            mimeType = mimeType,
            fileSize = fileSize,
            width = width,
            height = height,
        )
    }

    /**
     * Validate if the URI points to a valid image file
     *
     * @param context Application context
     * @param uri Image URI
     * @return true if valid image, false otherwise
     */
    fun isValidImage(context: Context, uri: Uri): Boolean {
        return try {
            val mimeType = context.contentResolver.getType(uri)
                ?: getMimeTypeFromFileName(uri.lastPathSegment)

            mimeType != null && mimeType.startsWith("image/")
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if the image format is supported
     *
     * @param mimeType MIME type of the image
     * @return true if supported, false otherwise
     */
    fun isSupportedImageFormat(mimeType: String?): Boolean {
        return when (mimeType) {
            "image/jpeg", "image/jpg" -> true
            "image/png" -> true
            "image/gif" -> true
            "image/webp" -> true
            "image/heic", "image/heif" -> true
            else -> false
        }
    }

    /**
     * Validate image file size against a maximum limit
     *
     * @param fileSize File size in bytes
     * @param maxSizeBytes Maximum allowed size in bytes
     * @return true if within limit, false otherwise
     */
    fun isValidImageSize(fileSize: Long, maxSizeBytes: Long): Boolean {
        return fileSize > 0 && fileSize <= maxSizeBytes
    }

    /**
     * Get file name from content URI
     */
    private fun getFileNameFromUri(context: Context, uri: Uri): String? {
        return try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && it.moveToFirst()) {
                    it.getString(nameIndex)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get MIME type from file extension
     */
    private fun getMimeTypeFromFileName(fileName: String?): String? {
        if (fileName == null) return null
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return if (extension.isNotEmpty()) {
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        } else {
            null
        }
    }

    /**
     * Get file size from URI
     */
    private fun getFileSize(context: Context, uri: Uri): Long {
        return try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                val sizeIndex = it.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (sizeIndex >= 0 && it.moveToFirst()) {
                    it.getLong(sizeIndex)
                } else {
                    0L
                }
            } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Get image dimensions from URI
     */
    private fun getImageDimensions(context: Context, uri: Uri): Pair<Int, Int> {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(inputStream, null, options)
                Pair(options.outWidth, options.outHeight)
            } ?: Pair(0, 0)
        } catch (e: IOException) {
            Pair(0, 0)
        }
    }
}
