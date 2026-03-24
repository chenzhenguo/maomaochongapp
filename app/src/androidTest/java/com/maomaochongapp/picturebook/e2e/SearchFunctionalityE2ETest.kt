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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-End tests for Search Functionality
 *
 * Tests the complete user journey:
 * 1. Create multiple books with various metadata
 * 2. Search by title
 * 3. Search by description
 * 4. Search by tags
 * 5. Combined search and filter
 * 6. Clear search and filters
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class SearchFunctionalityE2ETest {

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

        database = Room.inMemoryDatabaseBuilder(context, BookDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        repository = BookRepositoryImpl(database.bookDao())
        imageUtils = ImageUtils()

        viewModel = PictureBookViewModel(application, repository, imageUtils)

        // Create test data
        setupTestData()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        database.close()
    }

    private fun setupTestData() = runTest {
        // Create books with varied metadata for comprehensive search testing
        viewModel.createBook(
            title = "The Adventure of Magic Forest",
            description = "A thrilling adventure in an enchanted forest with magical creatures",
            tags = listOf("adventure", "fantasy", "magic"),
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.createBook(
            title = "Science for Young Minds",
            description = "Introduction to physics, chemistry, and biology for children",
            tags = listOf("science", "education", "learning"),
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.createBook(
            title = "Bedtime Stories Collection",
            description = "Calming stories perfect for bedtime reading",
            tags = listOf("bedtime", "fiction", "collection"),
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.createBook(
            title = "Numbers and Counting",
            description = "Learn to count from 1 to 100 with fun illustrations",
            tags = listOf("math", "education", "numbers"),
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.createBook(
            title = "Animal Friends",
            description = "Stories about friendship between different animals",
            tags = listOf("animals", "friendship", "fiction"),
        )
        testDispatcher.scheduler.advanceUntilIdle()

        testDispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun `e2e search by exact title match`() = runTest {
        viewModel.searchBooks("Science for Young Minds")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(1, state.filteredBooks.size)
        assertEquals("Science for Young Minds", state.filteredBooks[0].title)
    }

    @Test
    fun `e2e search by partial title match`() = runTest {
        viewModel.searchBooks("Adventure")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(1, state.filteredBooks.size)
        assertEquals("The Adventure of Magic Forest", state.filteredBooks[0].title)
    }

    @Test
    fun `e2e search is case insensitive`() = runTest {
        // Search with different cases
        viewModel.searchBooks("SCIENCE")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(1, state.filteredBooks.size)

        viewModel.searchBooks("science for young minds")
        testDispatcher.scheduler.advanceUntilIdle()

        val state2 = viewModel.state.value
        assertEquals(1, state2.filteredBooks.size)
    }

    @Test
    fun `e2e search by description content`() = runTest {
        viewModel.searchBooks("physics")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(1, state.filteredBooks.size)
        assertEquals("Science for Young Minds", state.filteredBooks[0].title)
    }

    @Test
    fun `e2e search by description with multiple matches`() = runTest {
        viewModel.searchBooks("stories")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(2, state.filteredBooks.size)
        // Both "Bedtime Stories Collection" and "Animal Friends" have "stories" in description
    }

    @Test
    fun `e2e search with no matches returns empty list`() = runTest {
        viewModel.searchBooks("nonexistent book title xyz")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state.filteredBooks.isEmpty())
    }

    @Test
    fun `e2e search with empty query shows all books`() = runTest {
        viewModel.searchBooks("")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(5, state.filteredBooks.size)
    }

    @Test
    fun `e2e filter by single tag`() = runTest {
        viewModel.filterByTags(setOf("fantasy"))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(1, state.filteredBooks.size)
        assertEquals("The Adventure of Magic Forest", state.filteredBooks[0].title)
    }

    @Test
    fun `e2e filter by multiple tags returns union`() = runTest {
        viewModel.filterByTags(setOf("fantasy", "math"))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(2, state.filteredBooks.size)
    }

    @Test
    fun `e2e filter by tag with no matches returns empty`() = runTest {
        viewModel.filterByTags(setOf("nonexistent-tag"))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state.filteredBooks.isEmpty())
    }

    @Test
    fun `e2e filter by empty tag set shows all books`() = runTest {
        viewModel.filterByTags(emptySet())
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(5, state.filteredBooks.size)
    }

    @Test
    fun `e2e combined search and tag filter`() = runTest {
        // First search
        viewModel.searchBooks("friendship")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then filter by tag
        viewModel.filterByTags(setOf("fiction"))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(1, state.filteredBooks.size)
        assertEquals("Animal Friends", state.filteredBooks[0].title)
    }

    @Test
    fun `e2e toggle tag filter on and off`() = runTest {
        // Initial state - all books
        var state = viewModel.state.value
        assertEquals(5, state.filteredBooks.size)

        // Toggle on "education" tag
        viewModel.toggleTagFilter("education")
        testDispatcher.scheduler.advanceUntilIdle()

        state = viewModel.state.value
        assertEquals(2, state.filteredBooks.size) // Science and Numbers

        // Toggle off "education" tag
        viewModel.toggleTagFilter("education")
        testDispatcher.scheduler.advanceUntilIdle()

        state = viewModel.state.value
        assertEquals(5, state.filteredBooks.size) // Back to all
    }

    @Test
    fun `e2e clear filters resets to all books`() = runTest {
        // Apply search and filter
        viewModel.searchBooks("adventure")
        viewModel.filterByTags(setOf("fantasy"))
        testDispatcher.scheduler.advanceUntilIdle()

        var state = viewModel.state.value
        assertEquals(1, state.filteredBooks.size)

        // Clear filters
        viewModel.clearFilters()
        testDispatcher.scheduler.advanceUntilIdle()

        state = viewModel.state.value
        assertEquals(5, state.filteredBooks.size)
        assertEquals("", state.searchQuery)
        assertTrue(state.selectedTags.isEmpty())
    }

    @Test
    fun `e2e all tags are extracted from books`() = runTest {
        val state = viewModel.state.value

        val expectedTags = setOf(
            "adventure", "fantasy", "magic",
            "science", "education", "learning",
            "bedtime", "fiction", "collection",
            "math", "numbers",
            "animals", "friendship",
        )

        assertEquals(expectedTags, state.allTags)
    }

    @Test
    fun `e2e search updates after new book is created`() = runTest {
        // Initial search
        viewModel.searchBooks("education")
        testDispatcher.scheduler.advanceUntilIdle()

        var state = viewModel.state.value
        assertEquals(2, state.filteredBooks.size)

        // Create new book with matching term
        viewModel.createBook(
            title = "History Education for Kids",
            description = "Learn about history",
            tags = listOf("history", "education"),
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Search should now include new book
        state = viewModel.state.value
        assertEquals(3, state.filteredBooks.size)
    }

    @Test
    fun `e2e search updates after book is deleted`() = runTest {
        // Initial search
        viewModel.searchBooks("animals")
        testDispatcher.scheduler.advanceUntilIdle()

        var state = viewModel.state.value
        assertEquals(1, state.filteredBooks.size)
        val bookId = state.filteredBooks[0].id

        // Delete the book
        viewModel.deleteBook(bookId)
        testDispatcher.scheduler.advanceUntilIdle()

        // Search should return empty now
        state = viewModel.state.value
        assertTrue(state.filteredBooks.isEmpty())
    }

    @Test
    fun `e2e unicode search works correctly`() = runTest {
        // Create book with unicode
        viewModel.createBook(
            title = "中文绘本测试",
            description = "日本語テスト",
            tags = listOf("中文", "日本語"),
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Search by Chinese
        viewModel.searchBooks("中文")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(1, state.filteredBooks.size)

        // Search by Japanese
        viewModel.searchBooks("日本語")
        testDispatcher.scheduler.advanceUntilIdle()

        val state2 = viewModel.state.value
        assertEquals(1, state2.filteredBooks.size)
    }
}
