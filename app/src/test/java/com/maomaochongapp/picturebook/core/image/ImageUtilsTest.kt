package com.maomaochongapp.picturebook.core.image

import android.content.ContentResolver
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.webkit.MimeTypeMap
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.InputStream

/**
 * Unit tests for ImageUtils
 *
 * Tests cover:
 * - getImageInfo with valid URIs
 * - Edge cases: null values, empty strings, invalid URIs
 * - isValidImage validation
 * - isSupportedImageFormat for various MIME types
 * - isValidImageSize boundary conditions
 * - Error paths: exceptions from ContentResolver
 */
class ImageUtilsTest {

    private lateinit var context: Context
    private lateinit var contentResolver: ContentResolver
    private lateinit var imageUtils: ImageUtils

    @Before
    fun setup() {
        context = mockk()
        contentResolver = mockk()
        imageUtils = ImageUtils()

        every { context.contentResolver } returns contentResolver
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // region getImageInfo Tests

    @Test
    fun `getImageInfo returns correct image info for valid URI`() {
        // Given
        val uri = mockk<Uri>()
        every { uri.lastPathSegment } returns "test.jpg"

        val inputStream = mockk<InputStream>()
        every { contentResolver.openInputStream(uri) } returns inputStream
        every { inputStream.close() } returns Unit

        // Mock BitmapFactory to return dimensions
        mockkStatic(BitmapFactory::class)
        every { BitmapFactory.decodeStream(any(), any(), withArg { it.outWidth = 1920; it.outHeight = 1080 }) } returns null

        // When
        val result = imageUtils.getImageInfo(context, uri)

        // Then
        assertEquals("test.jpg", result.fileName)
        assertEquals(1920, result.width)
        assertEquals(1080, result.height)
    }

    @Test
    fun `getImageInfo handles null lastPathSegment`() {
        // Given
        val uri = mockk<Uri>()
        every { uri.lastPathSegment } returns null
        every { contentResolver.openInputStream(uri) } returns null

        // When
        val result = imageUtils.getImageInfo(context, uri)

        // Then
        assertNull(result.fileName)
    }

    @Test
    fun `getImageInfo returns default values on exception`() {
        // Given
        val uri = mockk<Uri>()
        every { uri.lastPathSegment } throws RuntimeException("URI error")

        // When
        val result = imageUtils.getImageInfo(context, uri)

        // Then
        assertNull(result.fileName)
        assertNull(result.mimeType)
        assertEquals(0L, result.fileSize)
        assertEquals(0, result.width)
        assertEquals(0, result.height)
    }

    @Test
    fun `getImageInfo handles null content resolver type`() {
        // Given
        val uri = mockk<Uri>()
        every { uri.lastPathSegment } returns "test.png"
        every { contentResolver.getType(uri) } returns null
        every { contentResolver.openInputStream(uri) } returns null

        // When
        val result = imageUtils.getImageInfo(context, uri)

        // Then
        // mimeType should be derived from extension
        assertEquals("test.png", result.fileName)
    }

    @Test
    fun `getImageInfo returns zero dimensions when stream is null`() {
        // Given
        val uri = mockk<Uri>()
        every { uri.lastPathSegment } returns "test.jpg"
        every { contentResolver.openInputStream(uri) } returns null

        // When
        val result = imageUtils.getImageInfo(context, uri)

        // Then
        assertEquals(0, result.width)
        assertEquals(0, result.height)
    }

    @Test
    fun `getImageInfo handles IOException when reading dimensions`() {
        // Given
        val uri = mockk<Uri>()
        every { uri.lastPathSegment } returns "test.jpg"

        val inputStream = mockk<InputStream>()
        every { contentResolver.openInputStream(uri) } returns inputStream
        every { inputStream.close() } throws java.io.IOException("IO error")

        // When
        val result = imageUtils.getImageInfo(context, uri)

        // Then
        assertEquals(0, result.width)
        assertEquals(0, result.height)
    }

    // endregion

    // region isValidImage Tests

    @Test
    fun `isValidImage returns true for valid image MIME type`() {
        // Given
        val uri = mockk<Uri>()
        every { contentResolver.getType(uri) } returns "image/jpeg"

        // When
        val result = imageUtils.isValidImage(context, uri)

        // Then
        assertTrue(result)
    }

    @Test
    fun `isValidImage returns false for non-image MIME type`() {
        // Given
        val uri = mockk<Uri>()
        every { contentResolver.getType(uri) } returns "application/pdf"

        // When
        val result = imageUtils.isValidImage(context, uri)

        // Then
        assertFalse(result)
    }

    @Test
    fun `isValidImage returns false when MIME type is null`() {
        // Given
        val uri = mockk<Uri>()
        every { contentResolver.getType(uri) } returns null
        every { uri.lastPathSegment } returns "document.pdf"

        // When
        val result = imageUtils.isValidImage(context, uri)

        // Then
        assertFalse(result)
    }

    @Test
    fun `isValidImage returns true for image file extension when MIME null`() {
        // Given
        val uri = mockk<Uri>()
        every { contentResolver.getType(uri) } returns null
        every { uri.lastPathSegment } returns "photo.jpg"

        mockkObject(MimeTypeMap)
        every { MimeTypeMap.getSingleton() } returns mockk {
            every { getMimeTypeFromExtension("jpg") } returns "image/jpeg"
        }

        // When
        val result = imageUtils.isValidImage(context, uri)

        // Then
        assertTrue(result)
    }

    @Test
    fun `isValidImage returns false on exception`() {
        // Given
        val uri = mockk<Uri>()
        every { contentResolver.getType(uri) } throws RuntimeException("Error")

        // When
        val result = imageUtils.isValidImage(context, uri)

        // Then
        assertFalse(result)
    }

    // endregion

    // region isSupportedImageFormat Tests

    @Test
    fun `isSupportedImageFormat returns true for JPEG`() {
        assertTrue(imageUtils.isSupportedImageFormat("image/jpeg"))
    }

    @Test
    fun `isSupportedImageFormat returns true for JPG`() {
        assertTrue(imageUtils.isSupportedImageFormat("image/jpg"))
    }

    @Test
    fun `isSupportedImageFormat returns true for PNG`() {
        assertTrue(imageUtils.isSupportedImageFormat("image/png"))
    }

    @Test
    fun `isSupportedImageFormat returns true for GIF`() {
        assertTrue(imageUtils.isSupportedImageFormat("image/gif"))
    }

    @Test
    fun `isSupportedImageFormat returns true for WebP`() {
        assertTrue(imageUtils.isSupportedImageFormat("image/webp"))
    }

    @Test
    fun `isSupportedImageFormat returns true for HEIC`() {
        assertTrue(imageUtils.isSupportedImageFormat("image/heic"))
    }

    @Test
    fun `isSupportedImageFormat returns true for HEIF`() {
        assertTrue(imageUtils.isSupportedImageFormat("image/heif"))
    }

    @Test
    fun `isSupportedImageFormat returns false for unsupported format`() {
        assertFalse(imageUtils.isSupportedImageFormat("image/bmp"))
    }

    @Test
    fun `isSupportedImageFormat returns false for non-image MIME type`() {
        assertFalse(imageUtils.isSupportedImageFormat("application/pdf"))
    }

    @Test
    fun `isSupportedImageFormat returns false for null MIME type`() {
        assertFalse(imageUtils.isSupportedImageFormat(null))
    }

    @Test
    fun `isSupportedImageFormat returns false for empty string`() {
        assertFalse(imageUtils.isSupportedImageFormat(""))
    }

    @Test
    fun `isSupportedImageFormat is case sensitive`() {
        // The implementation checks exact matches
        assertFalse(imageUtils.isSupportedImageFormat("IMAGE/JPEG"))
    }

    // endregion

    // region isValidImageSize Tests

    @Test
    fun `isValidImageSize returns true for valid size within limit`() {
        // When
        val result = imageUtils.isValidImageSize(1024L, 10240L)

        // Then
        assertTrue(result)
    }

    @Test
    fun `isValidImageSize returns true for size equal to limit`() {
        // When
        val result = imageUtils.isValidImageSize(10240L, 10240L)

        // Then
        assertTrue(result)
    }

    @Test
    fun `isValidImageSize returns false for size exceeding limit`() {
        // When
        val result = imageUtils.isValidImageSize(10241L, 10240L)

        // Then
        assertFalse(result)
    }

    @Test
    fun `isValidImageSize returns false for zero file size`() {
        // When
        val result = imageUtils.isValidImageSize(0L, 10240L)

        // Then
        assertFalse(result)
    }

    @Test
    fun `isValidImageSize returns false for negative file size`() {
        // When
        val result = imageUtils.isValidImageSize(-1L, 10240L)

        // Then
        assertFalse(result)
    }

    @Test
    fun `isValidImageSize returns false for very large file`() {
        // When
        val result = imageUtils.isValidImageSize(100_000_000L, 10_000_000L)

        // Then
        assertFalse(result)
    }

    @Test
    fun `isValidImageSize handles max long value`() {
        // When
        val result = imageUtils.isValidImageSize(Long.MAX_VALUE, Long.MAX_VALUE)

        // Then
        assertTrue(result)
    }

    // endregion

    // region Edge Cases and Special Scenarios

    @Test
    fun `getImageInfo handles URI with content scheme`() {
        // Given
        val uri = mockk<Uri>()
        every { uri.lastPathSegment } returns "content_image.jpg"
        every { contentResolver.getType(uri) } returns "image/jpeg"
        every { contentResolver.openInputStream(uri) } returns null

        // When
        val result = imageUtils.getImageInfo(context, uri)

        // Then
        assertEquals("content_image.jpg", result.fileName)
        assertEquals("image/jpeg", result.mimeType)
    }

    @Test
    fun `getImageInfo handles URI with file scheme`() {
        // Given
        val uri = mockk<Uri>()
        every { uri.lastPathSegment } returns "/path/to/file.png"
        every { contentResolver.getType(uri) } returns "image/png"
        every { contentResolver.openInputStream(uri) } returns null

        // When
        val result = imageUtils.getImageInfo(context, uri)

        // Then
        assertEquals("/path/to/file.png", result.fileName)
    }

    @Test
    fun `getImageInfo handles file without extension`() {
        // Given
        val uri = mockk<Uri>()
        every { uri.lastPathSegment } returns "noextension"
        every { contentResolver.getType(uri) } returns null
        every { contentResolver.openInputStream(uri) } returns null

        // When
        val result = imageUtils.getImageInfo(context, uri)

        // Then
        assertEquals("noextension", result.fileName)
        assertNull(result.mimeType)
    }

    @Test
    fun `isSupportedImageFormat handles malformed MIME type`() {
        // When
        val result = imageUtils.isSupportedImageFormat("not/a/valid/mime/type")

        // Then
        assertFalse(result)
    }

    @Test
    fun `isValidImageSize with zero max size only accepts nothing`() {
        // When
        val result1 = imageUtils.isValidImageSize(0L, 0L)
        val result2 = imageUtils.isValidImageSize(1L, 0L)

        // Then
        assertFalse(result1) // 0 is not > 0
        assertFalse(result2) // 1 > 0
    }

    // endregion
}
