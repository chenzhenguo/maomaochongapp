package com.maomaochongapp.picturebook.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.res.Resources
import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.maomaochongapp.picturebook.core.image.ImageInfo
import com.maomaochongapp.picturebook.core.image.ImageUtils
import com.maomaochongapp.picturebook.domain.model.Book
import com.maomaochongapp.picturebook.domain.model.BookImage
import com.maomaochongapp.picturebook.domain.repository.BookRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Instant

/**
 * Unit tests for PictureBookViewModel
 *
 * Tests cover:
 * - Initial state and loading behavior
 * - Search and filter functionality
 * - Book CRUD operations
 * - Image management
 * - Error handling
 * - Edge cases: null values, empty lists, invalid input
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PictureBookViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var application: Application
    private lateinit var context: Context
    private lateinit var bookRepository: BookRepository
    private lateinit var imageUtils: ImageUtils
    private lateinit var viewModel: PictureBookViewModel

    private val testDispatcher = StandardTestDispatcher()
    private val testInstant = Instant.ofEpochMilli(1000000)

    private val testBook1 = Book(
        id = "book-1",
        title = "Test Book 1",
        description = "Description 1",
        tags = listOf("tag1", "tag2"),
        coverImageUri = "content://test/uri/1",
        pageCount = 5,
        createdAt = testInstant,
        updatedAt = testInstant,
    )

    private val testBook2 = Book(
        id = "book-2",
        title = "Test Book 2",
        description = "Description 2",
        tags = listOf("tag2", "tag3"),
        coverImageUri = "content://test/uri/2",
        pageCount = 10,
        createdAt = testInstant,
        updatedAt = testInstant,
    )

    private val testBook3 = Book(
        id = "book-3",
        title = "Different Book",
        description = "No tags here",
        tags = emptyList(),
        coverImageUri = null,
        pageCount = 0,
        createdAt = testInstant,
        updatedAt = testInstant,
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        application = mockk()
        context = mockk()
        val resources = mockk<Resources>()

        every { application.applicationContext } returns context
        every { context.resources } returns resources
        every { resources.getString(any()) } returns ""

        bookRepository = mockk()
        imageUtils = mockk()

        // Mock initial books flow
        every { bookRepository.getAllBooks() } returns flowOf(listOf(testBook1, testBook2, testBook3))

        viewModel = PictureBookViewModel(application, bookRepository, imageUtils)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    // region Initial State Tests

    @Test
    fun `initial state has correct default values`() {
        // When
        val state = viewModel.state.value

        // Then
        assertTrue(state.books.isEmpty()) // Initially empty before flow emits
        assertTrue(state.filteredBooks.isEmpty())
        assertEquals("", state.searchQuery)
        assertTrue(state.selectedTags.isEmpty())
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertNull(state.currentBook)
        assertTrue(state.bookImages.isEmpty())
        assertFalse(state.isCreating)
        assertNull(state.lastMessage)
    }

    @Test
    fun `loadAllBooks collects books from repository`() = runTest {
        // Given
        val booksFlow = MutableStateFlow(listOf(testBook1, testBook2))
        every { bookRepository.getAllBooks() } returns booksFlow

        viewModel = PictureBookViewModel(application, bookRepository, imageUtils)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertEquals(2, state.books.size)
        assertEquals(setOf("tag1", "tag2", "tag3"), state.allTags)
    }

    @Test
    fun `loadAllBooks extracts all unique tags from books`() = runTest {
        // Given
        every { bookRepository.getAllBooks() } returns flowOf(listOf(testBook1, testBook2, testBook3))

        viewModel = PictureBookViewModel(application, bookRepository, imageUtils)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertEquals(setOf("tag1", "tag2", "tag3"), state.allTags)
    }

    @Test
    fun `loadAllBooks with empty book list results in empty tags`() = runTest {
        // Given
        every { bookRepository.getAllBooks() } returns flowOf(emptyList())

        viewModel = PictureBookViewModel(application, bookRepository, imageUtils)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertTrue(state.allTags.isEmpty())
    }

    // endregion

    // region Search Tests

    @Test
    fun `searchBooks filters by title match`() = runTest {
        // Given
        every { bookRepository.getAllBooks() } returns flowOf(listOf(testBook1, testBook2, testBook3))

        viewModel = PictureBookViewModel(application, bookRepository, imageUtils)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.searchBooks("Book 1")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertEquals("Book 1", state.searchQuery)
        assertEquals(1, state.filteredBooks.size)
        assertEquals("Test Book 1", state.filteredBooks[0].title)
    }

    @Test
    fun `searchBooks filters by description match`() = runTest {
        // Given
        every { bookRepository.getAllBooks() } returns flowOf(listOf(testBook1, testBook2, testBook3))

        viewModel = PictureBookViewModel(application, bookRepository, imageUtils)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.searchBooks("Description 2")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertEquals(1, state.filteredBooks.size)
        assertEquals("Test Book 2", state.filteredBooks[0].title)
    }

    @Test
    fun `searchBooks is case insensitive`() = runTest {
        // Given
        every { bookRepository.getAllBooks() } returns flowOf(listOf(testBook1, testBook2, testBook3))

        viewModel = PictureBookViewModel(application, bookRepository, imageUtils)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.searchBooks("book")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - all 3 books match: "Test Book 1", "Test Book 2", "Different Book"
        val state = viewModel.state.value
        assertEquals(3, state.filteredBooks.size)
    }

    @Test
    fun `searchBooks with empty query shows all books`() = runTest {
        // Given
        every { bookRepository.getAllBooks() } returns flowOf(listOf(testBook1, testBook2, testBook3))

        viewModel = PictureBookViewModel(application, bookRepository, imageUtils)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.searchBooks("")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertEquals("", state.searchQuery)
        assertEquals(3, state.filteredBooks.size)
    }

    @Test
    fun `searchBooks finds no matches returns empty list`() = runTest {
        // Given
        every { bookRepository.getAllBooks() } returns flowOf(listOf(testBook1, testBook2, testBook3))

        viewModel = PictureBookViewModel(application, bookRepository, imageUtils)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.searchBooks("nonexistent")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertTrue(state.filteredBooks.isEmpty())
    }

    // endregion

    // region Tag Filter Tests

    @Test
    fun `filterByTags filters books by single tag`() = runTest {
        // Given
        every { bookRepository.getAllBooks() } returns flowOf(listOf(testBook1, testBook2, testBook3))

        viewModel = PictureBookViewModel(application, bookRepository, imageUtils)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.filterByTags(setOf("tag1"))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertEquals(setOf("tag1"), state.selectedTags)
        assertEquals(1, state.filteredBooks.size)
        assertEquals("Test Book 1", state.filteredBooks[0].title)
    }

    @Test
    fun `filterByTags filters books by multiple tags`() = runTest {
        // Given
        every { bookRepository.getAllBooks() } returns flowOf(listOf(testBook1, testBook2, testBook3))

        viewModel = PictureBookViewModel(application, bookRepository, imageUtils)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.filterByTags(setOf("tag1", "tag3"))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertEquals(2, state.filteredBooks.size)
    }

    @Test
    fun `filterByTags with empty set shows all books`() = runTest {
        // Given
        every { bookRepository.getAllBooks() } returns flowOf(listOf(testBook1, testBook2, testBook3))

        viewModel = PictureBookViewModel(application, bookRepository, imageUtils)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.filterByTags(emptySet())
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertTrue(state.selectedTags.isEmpty())
        assertEquals(3, state.filteredBooks.size)
    }

    @Test
    fun `toggleTagFilter adds tag when not present`() = runTest {
        // Given
        every { bookRepository.getAllBooks() } returns flowOf(listOf(testBook1, testBook2, testBook3))

        viewModel = PictureBookViewModel(application, bookRepository, imageUtils)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.toggleTagFilter("tag1")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertTrue(state.selectedTags.contains("tag1"))
    }

    @Test
    fun `toggleTagFilter removes tag when present`() = runTest {
        // Given
        every { bookRepository.getAllBooks() } returns flowOf(listOf(testBook1, testBook2, testBook3))

        viewModel = PictureBookViewModel(application, bookRepository, imageUtils)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.toggleTagFilter("tag1")
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.toggleTagFilter("tag1")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertFalse(state.selectedTags.contains("tag1"))
    }

    @Test
    fun `clearFilters resets search and tags`() = runTest {
        // Given
        every { bookRepository.getAllBooks() } returns flowOf(listOf(testBook1, testBook2, testBook3))

        viewModel = PictureBookViewModel(application, bookRepository, imageUtils)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.searchBooks("Book 1")
        viewModel.filterByTags(setOf("tag1"))
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.clearFilters()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertEquals("", state.searchQuery)
        assertTrue(state.selectedTags.isEmpty())
        assertEquals(3, state.filteredBooks.size)
    }

    // endregion

    // region createBook Tests

    @Test
    fun `createBook with valid data creates book successfully`() = runTest {
        // Given
        coEvery { bookRepository.upsertBook(any()) } returns "new-book-id"

        // When
        viewModel.createBook("New Book", "Description", listOf("tag1"))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertFalse(state.isCreating)
        assertEquals("Book created successfully", state.lastMessage)
        assertEquals("New Book", state.currentBook?.title)
        coVerify { bookRepository.upsertBook(any()) }
    }

    @Test
    fun `createBook with empty title sets error`() = runTest {
        // Given
        val initialState = viewModel.state.value

        // When
        viewModel.createBook("", "Description")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertEquals("Title cannot be empty", state.error)
        assertNull(state.currentBook)
        coVerify(exactly = 0) { bookRepository.upsertBook(any()) }
    }

    @Test
    fun `createBook with whitespace title sets error`() = runTest {
        // When
        viewModel.createBook("   ", "Description")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertEquals("Title cannot be empty", state.error)
        coVerify(exactly = 0) { bookRepository.upsertBook(any()) }
    }

    @Test
    fun `createBook trims title and description`() = runTest {
        // Given
        coEvery { bookRepository.upsertBook(any()) } returns "new-book-id"

        // When
        viewModel.createBook("  New Book  ", "  Desc  ")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify {
            bookRepository.upsertBook(
                withArg { book ->
                    assertEquals("New Book", book.title)
                    assertEquals("Desc", book.description)
                }
            )
        }
    }

    @Test
    fun `createBook filters empty tags`() = runTest {
        // Given
        coEvery { bookRepository.upsertBook(any()) } returns "new-book-id"

        // When
        viewModel.createBook("New Book", "", listOf("", "  ", "tag1"))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify {
            bookRepository.upsertBook(
                withArg { book ->
                    assertEquals(listOf("tag1"), book.tags)
                }
            )
        }
    }

    @Test
    fun `createBook with exception sets error`() = runTest {
        // Given
        coEvery { bookRepository.upsertBook(any()) } throws RuntimeException("Database error")

        // When
        viewModel.createBook("New Book")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertFalse(state.isCreating)
        assertEquals("Database error", state.error)
    }

    // endregion

    // region deleteBook Tests

    @Test
    fun `deleteBook removes book successfully`() = runTest {
        // Given
        coEvery { bookRepository.deleteBook("book-1") } returns Unit

        // When
        viewModel.deleteBook("book-1")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals("Book deleted successfully", state.lastMessage)
        coVerify { bookRepository.deleteBook("book-1") }
    }

    @Test
    fun `deleteBook clears currentBook if deleted`() = runTest {
        // Given
        viewModel = PictureBookViewModel(application, bookRepository, imageUtils)
        testDispatcher.scheduler.advanceUntilIdle()

        // Manually set current book for testing
        viewModel.loadBook("book-1")
        testDispatcher.scheduler.advanceUntilIdle()

        coEvery { bookRepository.deleteBook("book-1") } returns Unit

        // When
        viewModel.deleteBook("book-1")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertNull(state.currentBook)
    }

    @Test
    fun `deleteBook with exception sets error`() = runTest {
        // Given
        coEvery { bookRepository.deleteBook(any()) } throws RuntimeException("Delete failed")

        // When
        viewModel.deleteBook("book-1")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals("Delete failed", state.error)
    }

    // endregion

    // region loadBook Tests

    @Test
    fun `loadBook sets currentBook when found`() = runTest {
        // Given
        coEvery { bookRepository.getBookById("book-1") } returns testBook1

        // When
        viewModel.loadBook("book-1")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals("Test Book 1", state.currentBook?.title)
        assertNull(state.error)
    }

    @Test
    fun `loadBook sets error when not found`() = runTest {
        // Given
        coEvery { bookRepository.getBookById("non-existent") } returns null

        // When
        viewModel.loadBook("non-existent")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals("Book not found", state.error)
        assertNull(state.currentBook)
    }

    @Test
    fun `loadBook with exception sets error`() = runTest {
        // Given
        coEvery { bookRepository.getBookById(any()) } throws RuntimeException("Load failed")

        // When
        viewModel.loadBook("book-1")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals("Load failed", state.error)
    }

    // endregion

    // region loadBookImages Tests

    @Test
    fun `loadBookImages collects images from repository`() = runTest {
        // Given
        val images = listOf(
            BookImage(
                id = "img-1",
                bookId = "book-1",
                originalFileName = "image1.jpg",
                displayName = "image1.jpg",
                uri = "content://img/1",
                mimeType = "image/jpeg",
                fileSize = 1024,
                width = 800,
                height = 600,
                pageNumber = 0,
                createdAt = testInstant,
                updatedAt = testInstant,
            )
        )
        every { bookRepository.getBookImages("book-1") } returns flowOf(images)

        // When
        viewModel.loadBookImages("book-1")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals(1, state.bookImages.size)
        assertEquals("image1.jpg", state.bookImages[0].originalFileName)
    }

    @Test
    fun `loadBookImages handles empty images list`() = runTest {
        // Given
        every { bookRepository.getBookImages("book-1") } returns flowOf(emptyList())

        // When
        viewModel.loadBookImages("book-1")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertTrue(state.bookImages.isEmpty())
    }

    @Test
    fun `loadBookImages with exception sets error`() = runTest {
        // Given
        every { bookRepository.getBookImages(any()) } throws RuntimeException("Load images failed")

        // When
        viewModel.loadBookImages("book-1")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals("Load images failed", state.error)
    }

    // endregion

    // region addImagesToBook Tests

    @Test
    fun `addImagesToBook with empty list sets error`() = runTest {
        // When
        viewModel.addImagesToBook("book-1", emptyList())
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertEquals("No images selected", state.error)
    }

    @Test
    fun `addImagesToBook adds images successfully`() = runTest {
        // Given
        val uri = mockk<Uri>()
        every { uri.scheme } returns "content"
        every { uri.authority } returns "com.android.externalstorage.documents"
        every { uri.lastPathSegment } returns "test.jpg"
        every { uri.toString() } returns "content://test/image"

        val imageInfo = ImageInfo(
            fileName = "test.jpg",
            mimeType = "image/jpeg",
            fileSize = 1024L,
            width = 800,
            height = 600,
        )
        every { imageUtils.isValidImage(any(), any()) } returns true
        every { imageUtils.getImageInfo(any(), any()) } returns imageInfo
        coEvery { bookRepository.getBookById("book-1") } returns testBook1
        coEvery { bookRepository.upsertBookImages(any()) } returns Unit
        coEvery { bookRepository.upsertBook(any()) } returns "book-1"

        // When
        viewModel.addImagesToBook("book-1", listOf(uri))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertTrue(state.lastMessage?.contains("images added") == true)
        coVerify { bookRepository.upsertBookImages(any()) }
    }

    @Test
    fun `addImagesToBook updates cover image when null`() = runTest {
        // Given
        val bookWithoutCover = testBook1.copy(coverImageUri = null)
        val uri = mockk<Uri>()
        every { uri.scheme } returns "content"
        every { uri.authority } returns "com.android.externalstorage.documents"
        every { uri.lastPathSegment } returns "test.jpg"
        every { uri.toString() } returns "content://test/image"

        val imageInfo = ImageInfo(
            fileName = "test.jpg",
            mimeType = "image/jpeg",
            fileSize = 1024L,
            width = 800,
            height = 600,
        )
        every { imageUtils.isValidImage(any(), any()) } returns true
        every { imageUtils.getImageInfo(any(), any()) } returns imageInfo
        coEvery { bookRepository.getBookById("book-1") } returns bookWithoutCover
        coEvery { bookRepository.upsertBookImages(any()) } returns Unit
        coEvery { bookRepository.upsertBook(any()) } returns "book-1"

        // When
        viewModel.addImagesToBook("book-1", listOf(uri))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { bookRepository.upsertBook(withArg { assertEquals("content://test/image", it.coverImageUri) }) }
    }

    @Test
    fun `addImagesToBook with null book sets error`() = runTest {
        // Given
        val uri = mockk<Uri>()
        every { uri.scheme } returns "content"
        every { uri.authority } returns "com.android.externalstorage.documents"
        every { uri.lastPathSegment } returns "image.jpg"
        coEvery { bookRepository.getBookById("non-existent") } returns null

        // When
        viewModel.addImagesToBook("non-existent", listOf(uri))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals("Book not found", state.error)
    }

    @Test
    fun `addImagesToBook with exception sets error`() = runTest {
        // Given
        val uri = mockk<Uri>()
        every { uri.scheme } returns "content"
        every { uri.authority } returns "com.android.externalstorage.documents"
        every { uri.lastPathSegment } returns "image.jpg"
        every { imageUtils.isValidImage(any(), any()) } returns true
        every { imageUtils.getImageInfo(any(), any()) } throws RuntimeException("Image error")
        coEvery { bookRepository.getBookById("book-1") } returns testBook1

        // When
        viewModel.addImagesToBook("book-1", listOf(uri))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals("Image error", state.error)
    }

    // endregion

    // region removeImages Tests

    @Test
    fun `removeImages removes specified images`() = runTest {
        // Given - set up initial state with images
        val initialImages = listOf(
            BookImage(
                id = "img-1", bookId = "book-1", originalFileName = "1.jpg",
                displayName = "1.jpg", uri = "uri1", mimeType = "image/jpeg",
                fileSize = 100, width = 100, height = 100, pageNumber = 0,
                createdAt = testInstant, updatedAt = testInstant,
            ),
            BookImage(
                id = "img-2", bookId = "book-1", originalFileName = "2.jpg",
                displayName = "2.jpg", uri = "uri2", mimeType = "image/jpeg",
                fileSize = 100, width = 100, height = 100, pageNumber = 1,
                createdAt = testInstant, updatedAt = testInstant,
            ),
        )

        // Manually set bookImages for testing
        every { bookRepository.getBookImages("book-1") } returns flowOf(initialImages)
        viewModel.loadBookImages("book-1")
        testDispatcher.scheduler.advanceUntilIdle()

        coEvery { bookRepository.deleteBookImages(listOf("img-1")) } returns Unit

        // When
        viewModel.removeImages(listOf("img-1"))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals(1, state.bookImages.size)
        assertEquals("img-2", state.bookImages[0].id)
    }

    @Test
    fun `removeImages with exception sets error`() = runTest {
        // Given
        coEvery { bookRepository.deleteBookImages(any()) } throws RuntimeException("Delete failed")

        // When
        viewModel.removeImages(listOf("img-1"))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals("Delete failed", state.error)
    }

    // endregion

    // region clearError and clearMessage Tests

    @Test
    fun `clearError sets error to null`() {
        // Given - trigger an error first
        viewModel.createBook("") // This will set an error

        // When
        viewModel.clearError()

        // Then
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `clearMessage sets lastMessage to null`() = runTest {
        // Given
        coEvery { bookRepository.upsertBook(any()) } returns "new-id"

        viewModel.createBook("Test Book")
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.clearMessage()

        // Then
        assertNull(viewModel.state.value.lastMessage)
    }

    // endregion
}
