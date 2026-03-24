package com.maomaochongapp.picturebook.e2e

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maomaochongapp.picturebook.core.image.ImageInfo
import com.maomaochongapp.picturebook.core.image.ImageUtils
import com.maomaochongapp.picturebook.data.local.BookDatabase
import com.maomaochongapp.picturebook.data.repository.BookRepositoryImpl
import com.maomaochongapp.picturebook.ui.viewmodel.PictureBookViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

/**
 * End-to-End tests for Image Import and Display Flow
 *
 * Tests the complete user journey:
 * 1. Create a picture book
 * 2. Import images to the book
 * 3. Verify images are associated with the book
 * 4. Display image list
 * 5. Remove images
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class ImageImportDisplayE2ETest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var context: Context
    private lateinit var database: BookDatabase
    private lateinit var repository: BookRepositoryImpl
    private lateinit var imageUtils: ImageUtils
    private lateinit var viewModel: PictureBookViewModel
    private lateinit var application: Application

    private val testDispatcher = StandardTestDispatcher()

    private val mockImageInfo = ImageInfo(
        fileName = "test_image.jpg",
        mimeType = "image/jpeg",
        fileSize = 1024L,
        width = 800,
        height = 600,
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        application = ApplicationProvider.getApplicationContext()

        database = Room.inMemoryDatabaseBuilder(context, BookDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        repository = BookRepositoryImpl(database.bookDao())

        // Mock image utils for testing
        imageUtils = mockk()
        every { imageUtils.getImageInfo(any(), any()) } returns mockImageInfo

        viewModel = PictureBookViewModel(application, repository, imageUtils)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        database.close()
    }

    @Test
    fun `e2e complete image import and display flow`() = runTest {
        // Step 1: Create a book first
        viewModel.createBook(
            title = "Image Test Book",
            description = "Book for testing images",
            tags = listOf("test"),
        )
        testDispatcher.scheduler.advanceUntilIdle()

        var state = viewModel.state.value
        val bookId = state.currentBook?.id ?: throw AssertionError("Book should exist")

        // Step 2: Load book images (should be empty initially)
        viewModel.loadBookImages(bookId)
        testDispatcher.scheduler.advanceUntilIdle()

        state = viewModel.state.value
        assertTrue(state.bookImages.isEmpty())

        // Step 3: Import images
        val mockUri1 = mockk<Uri>()
        val mockUri2 = mockk<Uri>()
        every { mockUri1.toString() } returns "content://images/1"
        every { mockUri2.toString() } returns "content://images/2"

        viewModel.addImagesToBook(bookId, listOf(mockUri1, mockUri2))
        testDispatcher.scheduler.advanceUntilIdle()

        // Step 4: Verify images were added
        state = viewModel.state.value
        assertFalse(state.isLoading)
        assertTrue(state.lastMessage?.contains("images added") == true)

        // Step 5: Reload images and verify count
        viewModel.loadBookImages(bookId)
        testDispatcher.scheduler.advanceUntilIdle()

        state = viewModel.state.value
        assertEquals(2, state.bookImages.size)

        // Verify image properties
        val firstImage = state.bookImages[0]
        assertEquals(bookId, firstImage.bookId)
        assertEquals("test_image.jpg", firstImage.originalFileName)
        assertEquals(800, firstImage.width)
        assertEquals(600, firstImage.height)
        assertEquals(0, firstImage.pageNumber)

        val secondImage = state.bookImages[1]
        assertEquals(1, secondImage.pageNumber)
    }

    @Test
    fun `e2e add images to book without cover sets cover image`() = runTest {
        // Create a book without cover
        viewModel.createBook(
            title = "No Cover Book",
            description = "Book without cover",
        )
        testDispatcher.scheduler.advanceUntilIdle()

        var state = viewModel.state.value
        val bookId = state.currentBook?.id ?: throw AssertionError("Book should exist")

        // Verify no cover initially
        assertNull(state.currentBook?.coverImageUri)

        // Add images
        val mockUri = mockk<Uri>()
        every { mockUri.toString() } returns "content://images/cover"

        viewModel.addImagesToBook(bookId, listOf(mockUri))
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify cover was set
        state = viewModel.state.value
        assertEquals("content://images/cover", state.currentBook?.coverImageUri)
    }

    @Test
    fun `e2e add images to book with existing cover does not change cover`() = runTest {
        // Create a book with cover
        viewModel.createBook(
            title = "With Cover Book",
            description = "Book with existing cover",
        )
        testDispatcher.scheduler.advanceUntilIdle()

        var state = viewModel.state.value
        val bookId = state.currentBook?.id ?: throw AssertionError("Book should exist")

        // Manually set cover image
        val bookWithCover = state.currentBook?.copy(
            coverImageUri = "content://existing/cover",
        )
        bookWithCover?.let { viewModel.updateBook(it) }
        testDispatcher.scheduler.advanceUntilIdle()

        // Add more images
        val mockUri = mockk<Uri>()
        every { mockUri.toString() } returns "content://images/new"

        viewModel.addImagesToBook(bookId, listOf(mockUri))
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify cover was NOT changed
        state = viewModel.state.value
        assertEquals("content://existing/cover", state.currentBook?.coverImageUri)
    }

    @Test
    fun `e2e remove images from book`() = runTest {
        // Create book and add images
        viewModel.createBook(
            title = "Remove Test Book",
        )
        testDispatcher.scheduler.advanceUntilIdle()

        var state = viewModel.state.value
        val bookId = state.currentBook?.id ?: throw AssertionError("Book should exist")

        val mockUri1 = mockk<Uri>()
        val mockUri2 = mockk<Uri>()
        val mockUri3 = mockk<Uri>()
        every { mockUri1.toString() } returns "content://images/1"
        every { mockUri2.toString() } returns "content://images/2"
        every { mockUri3.toString() } returns "content://images/3"

        viewModel.addImagesToBook(bookId, listOf(mockUri1, mockUri2, mockUri3))
        testDispatcher.scheduler.advanceUntilIdle()

        // Load images
        viewModel.loadBookImages(bookId)
        testDispatcher.scheduler.advanceUntilIdle()

        state = viewModel.state.value
        assertEquals(3, state.bookImages.size)

        // Remove middle image
        val imageIdToRemove = state.bookImages[1].id
        viewModel.removeImages(listOf(imageIdToRemove))
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify removal
        state = viewModel.state.value
        assertEquals(2, state.bookImages.size)
        assertFalse(state.bookImages.any { it.id == imageIdToRemove })
    }

    @Test
    fun `e2e remove multiple images at once`() = runTest {
        // Create book and add images
        viewModel.createBook(title = "Multi Remove Book")
        testDispatcher.scheduler.advanceUntilIdle()

        var state = viewModel.state.value
        val bookId = state.currentBook?.id ?: throw AssertionError("Book should exist")

        // Add 5 images
        val uris = (1..5).map { i ->
            mockk<Uri>().also { every { it.toString() } returns "content://images/$i" }
        }

        viewModel.addImagesToBook(bookId, uris)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.loadBookImages(bookId)
        testDispatcher.scheduler.advanceUntilIdle()

        state = viewModel.state.value
        assertEquals(5, state.bookImages.size)

        // Remove first 3 images
        val idsToRemove = state.bookImages.take(3).map { it.id }
        viewModel.removeImages(idsToRemove)
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify
        state = viewModel.state.value
        assertEquals(2, state.bookImages.size)
        assertTrue(state.bookImages.all { it.id !in idsToRemove })
    }

    @Test
    fun `e2e add images with empty list shows error`() = runTest {
        // Create book
        viewModel.createBook(title = "Empty Images Book")
        testDispatcher.scheduler.advanceUntilIdle()

        var state = viewModel.state.value
        val bookId = state.currentBook?.id ?: throw AssertionError("Book should exist")

        // Try to add empty image list
        viewModel.addImagesToBook(bookId, emptyList())
        testDispatcher.scheduler.advanceUntilIdle()

        state = viewModel.state.value
        assertEquals("No images selected", state.error)
    }

    @Test
    fun `e2e image import with null book shows error`() = runTest {
        val mockUri = mockk<Uri>()

        // Try to add images to non-existent book
        viewModel.addImagesToBook("non-existent-book", listOf(mockUri))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("Book not found", state.error)
    }

    @Test
    fun `e2e image page numbers are sequential`() = runTest {
        // Create book
        viewModel.createBook(title = "Sequential Pages Book")
        testDispatcher.scheduler.advanceUntilIdle()

        var state = viewModel.state.value
        val bookId = state.currentBook?.id ?: throw AssertionError("Book should exist")

        // Add images in batches
        val batch1 = (1..2).map { i ->
            mockk<Uri>().also { every { it.toString() } returns "content://batch1/$i" }
        }
        viewModel.addImagesToBook(bookId, batch1)
        testDispatcher.scheduler.advanceUntilIdle()

        val batch2 = (1..3).map { i ->
            mockk<Uri>().also { every { it.toString() } returns "content://batch2/$i" }
        }
        viewModel.addImagesToBook(bookId, batch2)
        testDispatcher.scheduler.advanceUntilIdle()

        // Load and verify page numbers
        viewModel.loadBookImages(bookId)
        testDispatcher.scheduler.advanceUntilIdle()

        state = viewModel.state.value
        assertEquals(5, state.bookImages.size)

        // Verify page numbers are sequential
        state.bookImages.forEachIndexed { index, image ->
            assertEquals(index, image.pageNumber)
        }
    }

    @Test
    fun `e2e image metadata is preserved correctly`() = runTest {
        // Create custom image info
        val customImageInfo = ImageInfo(
            fileName = "custom_name.png",
            mimeType = "image/png",
            fileSize = 2048L,
            width = 1920,
            height = 1080,
        )
        every { imageUtils.getImageInfo(any(), any()) } returns customImageInfo

        // Create book and add image
        viewModel.createBook(title = "Metadata Test Book")
        testDispatcher.scheduler.advanceUntilIdle()

        var state = viewModel.state.value
        val bookId = state.currentBook?.id ?: throw AssertionError("Book should exist")

        val mockUri = mockk<Uri>()
        every { mockUri.toString() } returns "content://custom/image"

        viewModel.addImagesToBook(bookId, listOf(mockUri))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.loadBookImages(bookId)
        testDispatcher.scheduler.advanceUntilIdle()

        state = viewModel.state.value
        assertEquals(1, state.bookImages.size)

        val image = state.bookImages[0]
        assertEquals("custom_name.png", image.originalFileName)
        assertEquals("custom_name.png", image.displayName)
        assertEquals("image/png", image.mimeType)
        assertEquals(2048L, image.fileSize)
        assertEquals(1920, image.width)
        assertEquals(1080, image.height)
    }
}
