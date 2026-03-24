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

/**
 * End-to-End tests for Book Deletion Flow
 *
 * Tests the complete user journey:
 * 1. Create books with images
 * 2. Delete single book
 * 3. Verify book and images are removed
 * 4. Delete multiple books sequentially
 * 5. Handle deletion errors
 * 6. Verify UI state updates after deletion
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class BookDeletionE2ETest {

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
        fileName = "test.jpg",
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
    fun `e2e delete single book removes from list`() = runTest {
        // Create a book
        viewModel.createBook(
            title = "Book to Delete",
            description = "This book will be deleted",
            tags = listOf("test"),
        )
        testDispatcher.scheduler.advanceUntilIdle()

        var state = viewModel.state.value
        val bookId = state.currentBook?.id ?: throw AssertionError("Book should exist")

        // Verify book exists in repository
        var allBooks = repository.getAllBooks().first()
        assertEquals(1, allBooks.size)

        // Delete the book
        viewModel.deleteBook(bookId)
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify deletion
        state = viewModel.state.value
        assertEquals("Book deleted successfully", state.lastMessage)
        assertNull(state.currentBook)

        // Verify removed from repository
        allBooks = repository.getAllBooks().first()
        assertTrue(allBooks.isEmpty())
    }

    @Test
    fun `e2e delete book also removes associated images`() = runTest {
        // Create a book
        viewModel.createBook(
            title = "Book with Images",
        )
        testDispatcher.scheduler.advanceUntilIdle()

        var state = viewModel.state.value
        val bookId = state.currentBook?.id ?: throw AssertionError("Book should exist")

        // Add images
        val mockUri1 = mockk<Uri>()
        val mockUri2 = mockk<Uri>()
        every { mockUri1.toString() } returns "content://images/1"
        every { mockUri2.toString() } returns "content://images/2"

        viewModel.addImagesToBook(bookId, listOf(mockUri1, mockUri2))
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify images exist
        var images = repository.getBookImages(bookId).first()
        assertEquals(2, images.size)

        // Delete the book
        viewModel.deleteBook(bookId)
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify images are also deleted
        images = repository.getBookImages(bookId).first()
        assertTrue(images.isEmpty())
    }

    @Test
    fun `e2e delete book clears currentBook in UI state`() = runTest {
        // Create and select a book
        viewModel.createBook(title = "Current Book")
        testDispatcher.scheduler.advanceUntilIdle()

        var state = viewModel.state.value
        val bookId = state.currentBook?.id ?: throw AssertionError("Book should exist")
        assertNotNull(state.currentBook)

        // Delete the book
        viewModel.deleteBook(bookId)
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify currentBook is cleared
        state = viewModel.state.value
        assertNull(state.currentBook)
    }

    @Test
    fun `e2e delete book does not affect other books`() = runTest {
        // Create multiple books
        viewModel.createBook(title = "Book 1")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.createBook(title = "Book 2")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.createBook(title = "Book 3")
        testDispatcher.scheduler.advanceUntilIdle()

        testDispatcher.scheduler.advanceUntilIdle()

        var allBooks = repository.getAllBooks().first()
        assertEquals(3, allBooks.size)

        // Delete middle book
        val book2 = allBooks.find { it.title == "Book 2" } ?: throw AssertionError("Book 2 should exist")
        viewModel.deleteBook(book2.id)
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify other books remain
        allBooks = repository.getAllBooks().first()
        assertEquals(2, allBooks.size)
        assertTrue(allBooks.all { it.title != "Book 2" })
    }

    @Test
    fun `e2e delete multiple books sequentially`() = runTest {
        // Create 5 books
        for (i in 1..5) {
            viewModel.createBook(title = "Book $i")
            testDispatcher.scheduler.advanceUntilIdle()
        }

        testDispatcher.scheduler.advanceUntilIdle()

        var allBooks = repository.getAllBooks().first()
        assertEquals(5, allBooks.size)

        // Delete all books one by one
        allBooks.forEach { book ->
            viewModel.deleteBook(book.id)
            testDispatcher.scheduler.advanceUntilIdle()
        }

        // Verify all deleted
        allBooks = repository.getAllBooks().first()
        assertTrue(allBooks.isEmpty())
    }

    @Test
    fun `e2e delete non-existent book handles gracefully`() = runTest {
        // Attempt to delete non-existent book
        viewModel.deleteBook("non-existent-id")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        // Should have error or complete without issues
        assertFalse(state.isLoading)
    }

    @Test
    fun `e2e delete book with empty images list`() = runTest {
        // Create book without images
        viewModel.createBook(title = "Empty Book")
        testDispatcher.scheduler.advanceUntilIdle()

        var state = viewModel.state.value
        val bookId = state.currentBook?.id ?: throw AssertionError("Book should exist")

        // Verify no images
        var images = repository.getBookImages(bookId).first()
        assertTrue(images.isEmpty())

        // Delete
        viewModel.deleteBook(bookId)
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify deletion completed
        state = viewModel.state.value
        assertEquals("Book deleted successfully", state.lastMessage)
    }

    @Test
    fun `e2e delete book with many images`() = runTest {
        // Create book
        viewModel.createBook(title = "Book with Many Images")
        testDispatcher.scheduler.advanceUntilIdle()

        var state = viewModel.state.value
        val bookId = state.currentBook?.id ?: throw AssertionError("Book should exist")

        // Add 10 images
        val uris = (1..10).map { i ->
            mockk<Uri>().also { every { it.toString() } returns "content://images/$i" }
        }

        viewModel.addImagesToBook(bookId, uris)
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify images added
        var images = repository.getBookImages(bookId).first()
        assertEquals(10, images.size)

        // Delete book
        viewModel.deleteBook(bookId)
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify all images deleted
        images = repository.getBookImages(bookId).first()
        assertTrue(images.isEmpty())
    }

    @Test
    fun `e2e delete book updates filtered books list`() = runTest {
        // Create books
        viewModel.createBook(title = "Fantasy Book 1", tags = listOf("fantasy"))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.createBook(title = "Fantasy Book 2", tags = listOf("fantasy"))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.createBook(title = "Science Book", tags = listOf("science"))
        testDispatcher.scheduler.advanceUntilIdle()

        testDispatcher.scheduler.advanceUntilIdle()

        // Filter by fantasy tag
        viewModel.filterByTags(setOf("fantasy"))
        testDispatcher.scheduler.advanceUntilIdle()

        var state = viewModel.state.value
        assertEquals(2, state.filteredBooks.size)

        // Delete one fantasy book
        val fantasyBookId = state.filteredBooks[0].id
        viewModel.deleteBook(fantasyBookId)
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify filtered list updated
        state = viewModel.state.value
        assertEquals(1, state.filteredBooks.size)
        assertEquals("Fantasy Book 2", state.filteredBooks[0].title)
    }

    @Test
    fun `e2e delete book updates search results`() = runTest {
        // Create books
        viewModel.createBook(title = "The Great Adventure")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.createBook(title = "Another Adventure Story")
        testDispatcher.scheduler.advanceUntilIdle()

        testDispatcher.scheduler.advanceUntilIdle()

        // Search for adventure
        viewModel.searchBooks("Adventure")
        testDispatcher.scheduler.advanceUntilIdle()

        var state = viewModel.state.value
        assertEquals(2, state.filteredBooks.size)

        // Delete one book
        val bookId = state.filteredBooks[0].id
        viewModel.deleteBook(bookId)
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify search results updated
        state = viewModel.state.value
        assertEquals(1, state.filteredBooks.size)
    }

    @Test
    fun `e2e delete book with special characters in title`() = runTest {
        // Create book with special characters
        viewModel.createBook(
            title = "Book: Special & <Characters>",
            description = "Testing 'quotes' and \"double quotes\"",
        )
        testDispatcher.scheduler.advanceUntilIdle()

        var state = viewModel.state.value
        val bookId = state.currentBook?.id ?: throw AssertionError("Book should exist")

        // Delete
        viewModel.deleteBook(bookId)
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify deleted
        state = viewModel.state.value
        assertEquals("Book deleted successfully", state.lastMessage)

        val allBooks = repository.getAllBooks().first()
        assertTrue(allBooks.isEmpty())
    }

    @Test
    fun `e2e delete book with unicode content`() = runTest {
        // Create book with unicode
        viewModel.createBook(
            title = "中文绘本",
            description = "日本語テスト",
            tags = listOf("中文", "日本語"),
        )
        testDispatcher.scheduler.advanceUntilIdle()

        var state = viewModel.state.value
        val bookId = state.currentBook?.id ?: throw AssertionError("Book should exist")

        // Delete
        viewModel.deleteBook(bookId)
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify deleted
        state = viewModel.state.value
        assertEquals("Book deleted successfully", state.lastMessage)

        val allBooks = repository.getAllBooks().first()
        assertTrue(allBooks.isEmpty())
    }

    @Test
    fun `e2e delete then recreate with same title`() = runTest {
        // Create book
        viewModel.createBook(title = "Same Title Book")
        testDispatcher.scheduler.advanceUntilIdle()

        var state = viewModel.state.value
        val bookId = state.currentBook?.id ?: throw AssertionError("Book should exist")

        // Delete
        viewModel.deleteBook(bookId)
        testDispatcher.scheduler.advanceUntilIdle()

        // Recreate with same title
        viewModel.createBook(title = "Same Title Book")
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify new book created
        state = viewModel.state.value
        assertEquals("Book created successfully", state.lastMessage)
        assertNotNull(state.currentBook)
        assertEquals("Same Title Book", state.currentBook?.title)

        val allBooks = repository.getAllBooks().first()
        assertEquals(1, allBooks.size)
    }

    @Test
    fun `e2e UI state isLoading reflects deletion process`() = runTest {
        // Create book
        viewModel.createBook(title = "Loading Test Book")
        testDispatcher.scheduler.advanceUntilIdle()

        var state = viewModel.state.value
        val bookId = state.currentBook?.id ?: throw AssertionError("Book should exist")

        // Before deletion
        assertFalse(state.isLoading)

        // Delete
        viewModel.deleteBook(bookId)

        // After completion
        testDispatcher.scheduler.advanceUntilIdle()
        state = viewModel.state.value
        assertFalse(state.isLoading)
    }

    @Test
    fun `e2e complete delete flow verification`() = runTest {
        // Create book with images and tags
        viewModel.createBook(
            title = "Complete Flow Book",
            description = "A book for complete deletion testing",
            tags = listOf("test", "complete"),
        )
        testDispatcher.scheduler.advanceUntilIdle()

        var state = viewModel.state.value
        val bookId = state.currentBook?.id ?: throw AssertionError("Book should exist")

        // Add images
        val mockUri = mockk<Uri>()
        every { mockUri.toString() } returns "content://images/complete"
        viewModel.addImagesToBook(bookId, listOf(mockUri))
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify book exists with all data
        var book = repository.getBookById(bookId)
        assertNotNull(book)
        assertEquals("Complete Flow Book", book?.title)
        assertEquals(listOf("test", "complete"), book?.tags)

        var images = repository.getBookImages(bookId).first()
        assertEquals(1, images.size)

        // Delete
        viewModel.deleteBook(bookId)
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify complete deletion
        book = repository.getBookById(bookId)
        assertNull(book)

        images = repository.getBookImages(bookId).first()
        assertTrue(images.isEmpty())

        state = viewModel.state.value
        assertEquals("Book deleted successfully", state.lastMessage)
        assertNull(state.currentBook)
    }
}
