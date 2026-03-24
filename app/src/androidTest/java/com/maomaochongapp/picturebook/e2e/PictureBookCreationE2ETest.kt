package com.maomaochongapp.picturebook.e2e

import android.app.Application
import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maomaochongapp.picturebook.core.image.ImageUtils
import com.maomaochongapp.picturebook.data.local.BookDatabase
import com.maomaochongapp.picturebook.data.repository.BookRepositoryImpl
import com.maomaochongapp.picturebook.domain.model.Book
import com.maomaochongapp.picturebook.domain.model.BookImage
import com.maomaochongapp.picturebook.ui.viewmodel.PictureBookUiState
import com.maomaochongapp.picturebook.ui.viewmodel.PictureBookViewModel
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
 * End-to-End tests for Picture Book Creation Flow
 *
 * Tests the complete user journey:
 * 1. Initialize ViewModel with database
 * 2. Create a new picture book
 * 3. Verify book appears in the list
 * 4. Update book details
 * 5. Delete the book
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class PictureBookCreationE2ETest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var context: Context
    private lateinit var database: BookDatabase
    private lateinit var repository: BookRepositoryImpl
    private lateinit var imageUtils: ImageUtils
    private lateinit var viewModel: PictureBookViewModel
    private lateinit var application: Application

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        application = ApplicationProvider.getApplicationContext()

        // Create in-memory database for each test
        database = Room.inMemoryDatabaseBuilder(context, BookDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        repository = BookRepositoryImpl(database.bookDao())
        imageUtils = ImageUtils()

        viewModel = PictureBookViewModel(application, repository, imageUtils)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        database.close()
    }

    @Test
    fun `e2e complete book creation flow`() = runTest {
        // Step 1: Verify initial state - empty book list
        var state = viewModel.state.value
        assertTrue(state.books.isEmpty())

        // Step 2: Create a new book
        viewModel.createBook(
            title = "My First Picture Book",
            description = "A wonderful story for children",
            tags = listOf("children", "story", "fiction"),
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Step 3: Verify book was created
        state = viewModel.state.value
        assertFalse(state.isCreating)
        assertEquals("Book created successfully", state.lastMessage)
        assertNotNull(state.currentBook)
        assertEquals("My First Picture Book", state.currentBook?.title)

        // Step 4: Verify book appears in the list after repository emits
        val allBooks = repository.getAllBooks().first()
        assertEquals(1, allBooks.size)
        assertEquals("My First Picture Book", allBooks[0].title)

        // Step 5: Search for the book
        viewModel.searchBooks("First")
        testDispatcher.scheduler.advanceUntilIdle()

        state = viewModel.state.value
        assertEquals(1, state.filteredBooks.size)
        assertEquals("First", state.searchQuery)

        // Step 6: Clear search and verify all books shown
        viewModel.clearFilters()
        testDispatcher.scheduler.advanceUntilIdle()

        state = viewModel.state.value
        assertEquals("", state.searchQuery)

        // Step 7: Update the book
        val updatedBook = state.currentBook?.copy(
            title = "My Updated Picture Book",
            description = "An even better story",
        )
        updatedBook?.let { viewModel.updateBook(it) }
        testDispatcher.scheduler.advanceUntilIdle()

        state = viewModel.state.value
        assertEquals("My Updated Picture Book", state.currentBook?.title)

        // Step 8: Delete the book
        val bookId = state.currentBook?.id ?: throw AssertionError("Book ID should exist")
        viewModel.deleteBook(bookId)
        testDispatcher.scheduler.advanceUntilIdle()

        state = viewModel.state.value
        assertEquals("Book deleted successfully", state.lastMessage)
        assertNull(state.currentBook)

        // Step 9: Verify book is removed from repository
        val remainingBooks = repository.getAllBooks().first()
        assertEquals(0, remainingBooks.size)
    }

    @Test
    fun `e2e create multiple books and filter by tag`() = runTest {
        // Create book 1 with tags: fantasy, adventure
        viewModel.createBook(
            title = "Fantasy Adventure",
            description = "A magical journey",
            tags = listOf("fantasy", "adventure"),
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Create book 2 with tags: science, education
        viewModel.createBook(
            title = "Science for Kids",
            description = "Learn about science",
            tags = listOf("science", "education"),
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Create book 3 with tags: fantasy, mystery
        viewModel.createBook(
            title = "Mystery in the Castle",
            description = "A spooky tale",
            tags = listOf("fantasy", "mystery"),
        )
        testDispatcher.scheduler.advanceUntilIdle()

        testDispatcher.scheduler.advanceUntilIdle()

        // Filter by "fantasy" tag
        viewModel.filterByTags(setOf("fantasy"))
        testDispatcher.scheduler.advanceUntilIdle()

        var state = viewModel.state.value
        assertEquals(2, state.filteredBooks.size)

        // Filter by multiple tags
        viewModel.filterByTags(setOf("fantasy", "science"))
        testDispatcher.scheduler.advanceUntilIdle()

        state = viewModel.state.value
        assertEquals(3, state.filteredBooks.size)

        // Toggle off a tag
        viewModel.toggleTagFilter("science")
        testDispatcher.scheduler.advanceUntilIdle()

        state = viewModel.state.value
        assertEquals(2, state.filteredBooks.size)
    }

    @Test
    fun `e2e create book with empty title shows error`() = runTest {
        // Attempt to create book with empty title
        viewModel.createBook(
            title = "",
            description = "Should fail",
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("Title cannot be empty", state.error)
        assertNull(state.currentBook)

        // Verify no books were created
        val books = repository.getAllBooks().first()
        assertEquals(0, books.size)
    }

    @Test
    fun `e2e create book with special characters`() = runTest {
        // Create book with unicode and special characters
        viewModel.createBook(
            title = "测试绘本：Adventure & 魔法",
            description = "A bilingual story with 日本語 and español",
            tags = listOf("多言語", "multilingual"),
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("Book created successfully", state.lastMessage)
        assertEquals("测试绘本：Adventure & 魔法", state.currentBook?.title)

        // Verify retrieval
        val books = repository.getAllBooks().first()
        assertEquals(1, books.size)
        assertEquals("测试绘本：Adventure & 魔法", books[0].title)
    }

    @Test
    fun `e2e rapid book creation and deletion`() = runTest {
        // Create 5 books rapidly
        for (i in 1..5) {
            viewModel.createBook(
                title = "Book $i",
                description = "Description $i",
                tags = listOf("batch"),
            )
            testDispatcher.scheduler.advanceUntilIdle()
        }

        var state = viewModel.state.value
        assertEquals("Book created successfully", state.lastMessage)

        // Verify all books created
        val allBooks = repository.getAllBooks().first()
        assertEquals(5, allBooks.size)

        // Delete all books
        allBooks.forEach { book ->
            viewModel.deleteBook(book.id)
            testDispatcher.scheduler.advanceUntilIdle()
        }

        // Verify all deleted
        val remainingBooks = repository.getAllBooks().first()
        assertEquals(0, remainingBooks.size)
    }
}
