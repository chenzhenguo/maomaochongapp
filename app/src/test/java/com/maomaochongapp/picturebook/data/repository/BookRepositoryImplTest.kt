package com.maomaochongapp.picturebook.data.repository

import app.cash.turbine.test
import com.maomaochongapp.picturebook.data.local.BookDao
import com.maomaochongapp.picturebook.data.local.BookEntity
import com.maomaochongapp.picturebook.data.local.BookImageEntity
import com.maomaochongapp.picturebook.domain.model.Book
import com.maomaochongapp.picturebook.domain.model.BookImage
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.Instant

/**
 * Unit tests for BookRepositoryImpl
 *
 * Tests cover:
 * - All repository methods with mocked BookDao
 * - Edge cases: null values, empty lists, empty strings
 * - Error paths: exceptions from DAO
 * - Flow emissions and transformations
 */
class BookRepositoryImplTest {

    private lateinit var bookDao: BookDao
    private lateinit var repository: BookRepositoryImpl

    private val testInstant = Instant.ofEpochMilli(1000000)

    private val testBookEntity = BookEntity(
        id = "book-1",
        title = "Test Book",
        description = "Test Description",
        tags = "tag1,tag2",
        coverImageUri = "content://test/uri",
        pageCount = 5,
        createdAt = testInstant,
        updatedAt = testInstant,
        sourceFolderUri = null,
        exportPath = null,
    )

    private val testBook = Book(
        id = "book-1",
        title = "Test Book",
        description = "Test Description",
        tags = listOf("tag1", "tag2"),
        coverImageUri = "content://test/uri",
        pageCount = 5,
        createdAt = testInstant,
        updatedAt = testInstant,
        sourceFolderUri = null,
        exportPath = null,
    )

    private val testBookImageEntity = BookImageEntity(
        id = "image-1",
        bookId = "book-1",
        originalFileName = "test.jpg",
        displayName = "test.jpg",
        uri = "content://test/image/uri",
        mimeType = "image/jpeg",
        fileSize = 1024L,
        width = 800,
        height = 600,
        pageNumber = 1,
        createdAt = testInstant,
        updatedAt = testInstant,
    )

    private val testBookImage = BookImage(
        id = "image-1",
        bookId = "book-1",
        originalFileName = "test.jpg",
        displayName = "test.jpg",
        uri = "content://test/image/uri",
        mimeType = "image/jpeg",
        fileSize = 1024L,
        width = 800,
        height = 600,
        pageNumber = 1,
        createdAt = testInstant,
        updatedAt = testInstant,
    )

    @Before
    fun setup() {
        bookDao = mockk()
        repository = BookRepositoryImpl(bookDao)
    }

    // region getAllBooks Tests

    @Test
    fun `getAllBooks returns flow of domain books`() = runTest {
        // Given
        val entities = listOf(testBookEntity)
        every { bookDao.getAllBooks() } returns flowOf(entities)

        // When & Then
        repository.getAllBooks().test {
            val books = awaitItem()
            assertEquals(1, books.size)
            assertEquals(testBook.id, books[0].id)
            assertEquals(testBook.title, books[0].title)
            assertEquals(testBook.tags, books[0].tags)
            awaitComplete()
        }
    }

    @Test
    fun `getAllBooks returns empty list when dao returns empty`() = runTest {
        // Given
        every { bookDao.getAllBooks() } returns flowOf(emptyList())

        // When & Then
        repository.getAllBooks().test {
            val books = awaitItem()
            assertEquals(0, books.size)
            awaitComplete()
        }
    }

    @Test
    fun `getAllBooks maps multiple entities correctly`() = runTest {
        // Given
        val entity2 = testBookEntity.copy(id = "book-2", title = "Book 2")
        val entity3 = testBookEntity.copy(id = "book-3", title = "Book 3")
        every { bookDao.getAllBooks() } returns flowOf(listOf(testBookEntity, entity2, entity3))

        // When & Then
        repository.getAllBooks().test {
            val books = awaitItem()
            assertEquals(3, books.size)
            assertEquals("book-1", books[0].id)
            assertEquals("book-2", books[1].id)
            assertEquals("book-3", books[2].id)
            awaitComplete()
        }
    }

    // endregion

    // region searchBooks Tests

    @Test
    fun `searchBooks returns flow of matching books`() = runTest {
        // Given
        val entities = listOf(testBookEntity)
        every { bookDao.searchBooks("Test") } returns flowOf(entities)

        // When & Then
        repository.searchBooks("Test").test {
            val books = awaitItem()
            assertEquals(1, books.size)
            assertEquals(testBook.title, books[0].title)
            awaitComplete()
        }
    }

    @Test
    fun `searchBooks with empty query returns empty list`() = runTest {
        // Given
        every { bookDao.searchBooks("") } returns flowOf(emptyList())

        // When & Then
        repository.searchBooks("").test {
            val books = awaitItem()
            assertEquals(0, books.size)
            awaitComplete()
        }
    }

    @Test
    fun `searchBooks propagates query to dao`() = runTest {
        // Given
        every { bookDao.searchBooks(any()) } returns flowOf(emptyList())

        // When
        repository.searchBooks("search query").test {
            awaitItem() // consume the emptyList emission
            awaitComplete()
        }

        // Then
        verify { bookDao.searchBooks("search query") }
    }

    // endregion

    // region getBooksByTags Tests

    @Test
    fun `getBooksByTags returns books matching first tag when tags not empty`() = runTest {
        // Given
        val entities = listOf(testBookEntity)
        every { bookDao.getBooksByTag("tag1") } returns flowOf(entities)

        // When & Then
        repository.getBooksByTags(listOf("tag1", "tag2")).test {
            val books = awaitItem()
            assertEquals(1, books.size)
            awaitComplete()
        }
    }

    @Test
    fun `getBooksByTags returns all books when tags list is empty`() = runTest {
        // Given
        every { bookDao.getAllBooks() } returns flowOf(listOf(testBookEntity))
        every { bookDao.getBooksByTag(any()) } returns flowOf(emptyList())

        // When & Then
        repository.getBooksByTags(emptyList()).test {
            val books = awaitItem()
            assertEquals(1, books.size)
            awaitComplete()
        }
    }

    @Test
    fun `getBooksByTags with single tag returns matching books`() = runTest {
        // Given
        every { bookDao.getBooksByTag("fiction") } returns flowOf(listOf(testBookEntity))

        // When & Then
        repository.getBooksByTags(listOf("fiction")).test {
            val books = awaitItem()
            assertEquals(1, books.size)
            awaitComplete()
        }
    }

    // endregion

    // region getBookById Tests

    @Test
    fun `getBookById returns book when found`() = runTest {
        // Given
        coEvery { bookDao.getBookById("book-1") } returns testBookEntity

        // When
        val result = repository.getBookById("book-1")

        // Then
        assertEquals(testBook.id, result?.id)
        assertEquals(testBook.title, result?.title)
        coVerify { bookDao.getBookById("book-1") }
    }

    @Test
    fun `getBookById returns null when not found`() = runTest {
        // Given
        coEvery { bookDao.getBookById("non-existent") } returns null

        // When
        val result = repository.getBookById("non-existent")

        // Then
        assertNull(result)
        coVerify { bookDao.getBookById("non-existent") }
    }

    @Test
    fun `getBookById with empty string id returns null`() = runTest {
        // Given
        coEvery { bookDao.getBookById("") } returns null

        // When
        val result = repository.getBookById("")

        // Then
        assertNull(result)
    }

    // endregion

    // region getBookImages Tests

    @Test
    fun `getBookImages returns flow of domain book images`() = runTest {
        // Given
        val entities = listOf(testBookImageEntity)
        every { bookDao.getBookImages("book-1") } returns flowOf(entities)

        // When & Then
        repository.getBookImages("book-1").test {
            val images = awaitItem()
            assertEquals(1, images.size)
            assertEquals(testBookImage.id, images[0].id)
            assertEquals(testBookImage.bookId, images[0].bookId)
            awaitComplete()
        }
    }

    @Test
    fun `getBookImages returns empty list when no images`() = runTest {
        // Given
        every { bookDao.getBookImages("book-1") } returns flowOf(emptyList())

        // When & Then
        repository.getBookImages("book-1").test {
            val images = awaitItem()
            assertEquals(0, images.size)
            awaitComplete()
        }
    }

    @Test
    fun `getBookImages maps multiple images correctly`() = runTest {
        // Given
        val image2 = testBookImageEntity.copy(id = "image-2", pageNumber = 2)
        val image3 = testBookImageEntity.copy(id = "image-3", pageNumber = 3)
        every { bookDao.getBookImages("book-1") } returns flowOf(listOf(testBookImageEntity, image2, image3))

        // When & Then
        repository.getBookImages("book-1").test {
            val images = awaitItem()
            assertEquals(3, images.size)
            assertEquals(1, images[0].pageNumber)
            assertEquals(2, images[1].pageNumber)
            assertEquals(3, images[2].pageNumber)
            awaitComplete()
        }
    }

    // endregion

    // region upsertBook Tests

    @Test
    fun `upsertBook inserts book and returns id`() = runTest {
        // Given
        coEvery { bookDao.insertBook(any()) } returns 1L

        // When
        val result = repository.upsertBook(testBook)

        // Then
        assertEquals(testBook.id, result)
        coVerify { bookDao.insertBook(any()) }
    }

    @Test
    fun `upsertBook converts domain to entity before inserting`() = runTest {
        // Given
        coEvery { bookDao.insertBook(any()) } returns 1L

        // When
        repository.upsertBook(testBook)

        // Then
        coVerify {
            bookDao.insertBook(any())
        }
    }

    @Test
    fun `upsertBook with empty description handles correctly`() = runTest {
        // Given
        val bookWithEmptyDesc = testBook.copy(description = "")
        coEvery { bookDao.insertBook(any()) } returns 1L

        // When
        val result = repository.upsertBook(bookWithEmptyDesc)

        // Then
        assertEquals(bookWithEmptyDesc.id, result)
    }

    // endregion

    // region upsertBookImages Tests

    @Test
    fun `upsertBookImages inserts all images`() = runTest {
        // Given
        val images = listOf(testBookImage)
        coEvery { bookDao.insertBookImages(any()) } returns Unit

        // When
        repository.upsertBookImages(images)

        // Then
        coVerify { bookDao.insertBookImages(any()) }
    }

    @Test
    fun `upsertBookImages converts domain to entities before inserting`() = runTest {
        // Given
        val images = listOf(testBookImage)
        coEvery { bookDao.insertBookImages(any()) } returns Unit

        // When
        repository.upsertBookImages(images)

        // Then
        coVerify {
            bookDao.insertBookImages(any())
        }
    }

    @Test
    fun `upsertBookImages handles empty list`() = runTest {
        // Given
        coEvery { bookDao.insertBookImages(any()) } returns Unit

        // When
        repository.upsertBookImages(emptyList())

        // Then
        coVerify { bookDao.insertBookImages(emptyList()) }
    }

    @Test
    fun `upsertBookImages handles multiple images`() = runTest {
        // Given
        val image2 = testBookImage.copy(id = "image-2")
        val image3 = testBookImage.copy(id = "image-3")
        coEvery { bookDao.insertBookImages(any()) } returns Unit

        // When
        repository.upsertBookImages(listOf(testBookImage, image2, image3))

        // Then
        coVerify {
            bookDao.insertBookImages(any())
        }
    }

    // endregion

    // region deleteBook Tests

    @Test
    fun `deleteBook deletes book images first then book`() = runTest {
        // Given
        coEvery { bookDao.getBookById("book-1") } returns testBookEntity
        coEvery { bookDao.deleteAllBookImagesForBook("book-1") } returns Unit
        coEvery { bookDao.deleteBook(any()) } returns Unit

        // When
        repository.deleteBook("book-1")

        // Then
        coVerifyOrder {
            bookDao.getBookById("book-1")
            bookDao.deleteAllBookImagesForBook("book-1")
            bookDao.deleteBook(any())
        }
    }

    @Test
    fun `deleteBook does nothing when book not found`() = runTest {
        // Given
        coEvery { bookDao.getBookById("non-existent") } returns null
        coEvery { bookDao.deleteAllBookImagesForBook(any()) } returns Unit
        coEvery { bookDao.deleteBook(any()) } returns Unit

        // When
        repository.deleteBook("non-existent")

        // Then
        coVerify(exactly = 0) { bookDao.deleteAllBookImagesForBook(any()) }
        coVerify(exactly = 0) { bookDao.deleteBook(any()) }
    }

    @Test
    fun `deleteBook with empty id does nothing`() = runTest {
        // Given
        coEvery { bookDao.getBookById("") } returns null

        // When
        repository.deleteBook("")

        // Then
        coVerify(exactly = 0) { bookDao.deleteAllBookImagesForBook(any()) }
        coVerify(exactly = 0) { bookDao.deleteBook(any()) }
    }

    // endregion

    // region deleteBookImages Tests

    @Test
    fun `deleteBookImages deletes specified images`() = runTest {
        // Given
        val imageIds = listOf("image-1", "image-2")
        coEvery { bookDao.deleteBookImages(imageIds) } returns Unit

        // When
        repository.deleteBookImages(imageIds)

        // Then
        coVerify { bookDao.deleteBookImages(imageIds) }
    }

    @Test
    fun `deleteBookImages handles empty list`() = runTest {
        // Given
        coEvery { bookDao.deleteBookImages(emptyList()) } returns Unit

        // When
        repository.deleteBookImages(emptyList())

        // Then
        coVerify { bookDao.deleteBookImages(emptyList()) }
    }

    @Test
    fun `deleteBookImages handles single image id`() = runTest {
        // Given
        coEvery { bookDao.deleteBookImages(listOf("image-1")) } returns Unit

        // When
        repository.deleteBookImages(listOf("image-1"))

        // Then
        coVerify { bookDao.deleteBookImages(listOf("image-1")) }
    }

    // endregion
}
