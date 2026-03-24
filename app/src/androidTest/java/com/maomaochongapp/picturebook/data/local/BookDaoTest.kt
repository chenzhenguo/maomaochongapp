package com.maomaochongapp.picturebook.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.maomaochongapp.picturebook.data.local.BookDatabase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

/**
 * Integration tests for BookDao
 *
 * Tests cover:
 * - All DAO operations with in-memory Room database
 * - CRUD operations for books
 * - CRUD operations for book images
 * - Search functionality
 * - Tag-based filtering
 * - Edge cases and data validation
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
class BookDaoTest {

    private lateinit var bookDao: BookDao
    private lateinit var database: BookDatabase
    private lateinit var context: Context

    private val testInstant = Instant.ofEpochMilli(1000000)

    private val testBook = BookEntity(
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

    private val testBookImage = BookImageEntity(
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
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, BookDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        bookDao = database.bookDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // region insertBook Tests

    @Test
    fun `insertBook inserts book successfully and returns row id`() = runTest {
        // When
        val rowId = bookDao.insertBook(testBook)

        // Then
        assertTrue(rowId > 0)
        val insertedBook = bookDao.getBookById("book-1")
        assertNotNull(insertedBook)
        assertEquals("book-1", insertedBook?.id)
        assertEquals("Test Book", insertedBook?.title)
    }

    @Test
    fun `insertBook with REPLACE strategy updates existing book`() = runTest {
        // Given
        bookDao.insertBook(testBook)

        // When
        val updatedBook = testBook.copy(title = "Updated Title", updatedAt = Instant.now())
        bookDao.insertBook(updatedBook)

        // Then
        val retrieved = bookDao.getBookById("book-1")
        assertEquals("Updated Title", retrieved?.title)
    }

    @Test
    fun `insertBook handles empty description`() = runTest {
        // Given
        val bookWithEmptyDesc = testBook.copy(id = "book-2", description = "")

        // When
        bookDao.insertBook(bookWithEmptyDesc)

        // Then
        val retrieved = bookDao.getBookById("book-2")
        assertEquals("", retrieved?.description)
    }

    @Test
    fun `insertBook handles empty tags`() = runTest {
        // Given
        val bookWithNoTags = testBook.copy(id = "book-2", tags = "")

        // When
        bookDao.insertBook(bookWithNoTags)

        // Then
        val retrieved = bookDao.getBookById("book-2")
        assertEquals("", retrieved?.tags)
    }

    @Test
    fun `insertBook handles null URIs`() = runTest {
        // Given
        val bookWithNullUris = testBook.copy(
            id = "book-2",
            coverImageUri = null,
            sourceFolderUri = null,
            exportPath = null,
        )

        // When
        bookDao.insertBook(bookWithNullUris)

        // Then
        val retrieved = bookDao.getBookById("book-2")
        assertNull(retrieved?.coverImageUri)
        assertNull(retrieved?.sourceFolderUri)
        assertNull(retrieved?.exportPath)
    }

    @Test
    fun `insertBook handles unicode characters`() = runTest {
        // Given
        val bookWithUnicode = testBook.copy(
            id = "book-2",
            title = "测试绘本",
            description = "日本語の説明",
        )

        // When
        bookDao.insertBook(bookWithUnicode)

        // Then
        val retrieved = bookDao.getBookById("book-2")
        assertEquals("测试绘本", retrieved?.title)
        assertEquals("日本語の説明", retrieved?.description)
    }

    @Test
    fun `insertBook handles very long strings`() = runTest {
        // Given
        val longString = "A".repeat(10000)
        val bookWithLongDesc = testBook.copy(id = "book-2", description = longString)

        // When
        bookDao.insertBook(bookWithLongDesc)

        // Then
        val retrieved = bookDao.getBookById("book-2")
        assertEquals(10000, retrieved?.description?.length)
    }

    @Test
    fun `insertBook handles special characters in title`() = runTest {
        // Given
        val bookWithSpecialChars = testBook.copy(
            id = "book-2",
            title = "Book: \"Title\" & <Tags> 'quotes'",
        )

        // When
        bookDao.insertBook(bookWithSpecialChars)

        // Then
        val retrieved = bookDao.getBookById("book-2")
        assertEquals("Book: \"Title\" & <Tags> 'quotes'", retrieved?.title)
    }

    // endregion

    // region getAllBooks Tests

    @Test
    fun `getAllBooks returns empty flow when no books`() = runTest {
        // When & Then
        val books = bookDao.getAllBooks().first()
        assertTrue(books.isEmpty())
    }

    @Test
    fun `getAllBooks returns all inserted books`() = runTest {
        // Given
        val book2 = testBook.copy(id = "book-2", title = "Book 2")
        val book3 = testBook.copy(id = "book-3", title = "Book 3")
        bookDao.insertBook(testBook)
        bookDao.insertBook(book2)
        bookDao.insertBook(book3)

        // When
        val books = bookDao.getAllBooks().first()

        // Then
        assertEquals(3, books.size)
        assertEquals("book-1", books[0].id)
        assertEquals("book-2", books[1].id)
        assertEquals("book-3", books[2].id)
    }

    @Test
    fun `getAllBooks orders by updated_at DESC`() = runTest {
        // Given
        val oldBook = testBook.copy(
            id = "book-2",
            updatedAt = Instant.ofEpochMilli(500000),
        )
        val newBook = testBook.copy(
            id = "book-3",
            updatedAt = Instant.ofEpochMilli(2000000),
        )
        bookDao.insertBook(testBook)
        bookDao.insertBook(oldBook)
        bookDao.insertBook(newBook)

        // When
        val books = bookDao.getAllBooks().first()

        // Then
        assertEquals(3, books.size)
        assertEquals("book-3", books[0].id) // Newest first
        assertEquals("book-1", books[1].id)
        assertEquals("book-2", books[2].id) // Oldest last
    }

    // endregion

    // region searchBooks Tests

    @Test
    fun `searchBooks finds books by title match`() = runTest {
        // Given
        bookDao.insertBook(testBook)

        // When
        val books = bookDao.searchBooks("Test").first()

        // Then
        assertEquals(1, books.size)
        assertEquals("Test Book", books[0].title)
    }

    @Test
    fun `searchBooks finds books by description match`() = runTest {
        // Given
        bookDao.insertBook(testBook)

        // When
        val books = bookDao.searchBooks("Description").first()

        // Then
        assertEquals(1, books.size)
    }

    @Test
    fun `searchBooks finds books by tags match`() = runTest {
        // Given
        bookDao.insertBook(testBook)

        // When
        val books = bookDao.searchBooks("tag1").first()

        // Then
        assertEquals(1, books.size)
    }

    @Test
    fun `searchBooks is case insensitive`() = runTest {
        // Given
        bookDao.insertBook(testBook)

        // When
        val books = bookDao.searchBooks("test").first()

        // Then
        assertEquals(1, books.size)
    }

    @Test
    fun `searchBooks with no matches returns empty list`() = runTest {
        // Given
        bookDao.insertBook(testBook)

        // When
        val books = bookDao.searchBooks("nonexistent").first()

        // Then
        assertTrue(books.isEmpty())
    }

    @Test
    fun `searchBooks with empty query returns all books`() = runTest {
        // Given
        bookDao.insertBook(testBook)
        val book2 = testBook.copy(id = "book-2")
        bookDao.insertBook(book2)

        // When
        val books = bookDao.searchBooks("").first()

        // Then
        assertEquals(2, books.size)
    }

    @Test
    fun `searchBooks with partial match works`() = runTest {
        // Given
        bookDao.insertBook(testBook)

        // When
        val books = bookDao.searchBooks("Test B").first()

        // Then
        assertEquals(1, books.size)
    }

    // endregion

    // region getBooksByTag Tests

    @Test
    fun `getBooksByTag returns books matching tag`() = runTest {
        // Given
        bookDao.insertBook(testBook)

        // When
        val books = bookDao.getBooksByTag("tag1").first()

        // Then
        assertEquals(1, books.size)
        assertEquals("book-1", books[0].id)
    }

    @Test
    fun `getBooksByTag matches tag within comma-separated list`() = runTest {
        // Given
        bookDao.insertBook(testBook)

        // When
        val books = bookDao.getBooksByTag("tag2").first()

        // Then
        assertEquals(1, books.size)
    }

    @Test
    fun `getBooksByTag with no matches returns empty list`() = runTest {
        // Given
        bookDao.insertBook(testBook)

        // When
        val books = bookDao.getBooksByTag("nonexistent").first()

        // Then
        assertTrue(books.isEmpty())
    }

    @Test
    fun `getBooksByTag is case sensitive`() = runTest {
        // Given
        bookDao.insertBook(testBook)

        // When
        val books1 = bookDao.getBooksByTag("TAG1").first()
        val books2 = bookDao.getBooksByTag("tag1").first()

        // Then
        assertTrue(books1.isEmpty()) // Case sensitive - no match
        assertEquals(1, books2.size) // Exact match
    }

    // endregion

    // region getBookById Tests

    @Test
    fun `getBookById returns book when exists`() = runTest {
        // Given
        bookDao.insertBook(testBook)

        // When
        val book = bookDao.getBookById("book-1")

        // Then
        assertNotNull(book)
        assertEquals("book-1", book?.id)
        assertEquals("Test Book", book?.title)
    }

    @Test
    fun `getBookById returns null when not exists`() = runTest {
        // When
        val book = bookDao.getBookById("non-existent")

        // Then
        assertNull(book)
    }

    @Test
    fun `getBookById with empty string returns null`() = runTest {
        // When
        val book = bookDao.getBookById("")

        // Then
        assertNull(book)
    }

    // endregion

    // region insertBookImages Tests

    @Test
    fun `insertBookImages inserts multiple images successfully`() = runTest {
        // Given
        bookDao.insertBook(testBook)
        val image2 = testBookImage.copy(id = "image-2", pageNumber = 2)
        val image3 = testBookImage.copy(id = "image-3", pageNumber = 3)

        // When
        bookDao.insertBookImages(listOf(testBookImage, image2, image3))

        // Then
        val images = bookDao.getBookImages("book-1").first()
        assertEquals(3, images.size)
    }

    @Test
    fun `insertBookImages with REPLACE strategy updates existing image`() = runTest {
        // Given
        bookDao.insertBook(testBook)
        bookDao.insertBookImages(listOf(testBookImage))

        // When
        val updatedImage = testBookImage.copy(displayName = "updated.jpg", updatedAt = Instant.now())
        bookDao.insertBookImages(listOf(updatedImage))

        // Then
        val images = bookDao.getBookImages("book-1").first()
        assertEquals(1, images.size)
        assertEquals("updated.jpg", images[0].displayName)
    }

    @Test
    fun `insertBookImages handles empty list`() = runTest {
        // When
        bookDao.insertBookImages(emptyList())

        // Then
        val images = bookDao.getBookImages("book-1").first()
        assertTrue(images.isEmpty())
    }

    // endregion

    // region getBookImages Tests

    @Test
    fun `getBookImages returns empty flow when no images`() = runTest {
        // Given
        bookDao.insertBook(testBook)

        // When & Then
        val images = bookDao.getBookImages("book-1").first()
        assertTrue(images.isEmpty())
    }

    @Test
    fun `getBookImages returns images ordered by page_number ASC`() = runTest {
        // Given
        bookDao.insertBook(testBook)
        val image1 = testBookImage.copy(id = "img-1", pageNumber = 3)
        val image2 = testBookImage.copy(id = "img-2", pageNumber = 1)
        val image3 = testBookImage.copy(id = "img-3", pageNumber = 2)
        bookDao.insertBookImages(listOf(image1, image2, image3))

        // When
        val images = bookDao.getBookImages("book-1").first()

        // Then
        assertEquals(3, images.size)
        assertEquals(1, images[0].pageNumber)
        assertEquals(2, images[1].pageNumber)
        assertEquals(3, images[2].pageNumber)
    }

    @Test
    fun `getBookImages returns only images for specified book`() = runTest {
        // Given
        val book2 = testBook.copy(id = "book-2")
        bookDao.insertBook(testBook)
        bookDao.insertBook(book2)

        val book1Image = testBookImage.copy(id = "img-1", bookId = "book-1")
        val book2Image = testBookImage.copy(id = "img-2", bookId = "book-2")
        bookDao.insertBookImages(listOf(book1Image, book2Image))

        // When
        val book1Images = bookDao.getBookImages("book-1").first()
        val book2Images = bookDao.getBookImages("book-2").first()

        // Then
        assertEquals(1, book1Images.size)
        assertEquals("img-1", book1Images[0].id)
        assertEquals(1, book2Images.size)
        assertEquals("img-2", book2Images[0].id)
    }

    // endregion

    // region updateBook Tests

    @Test
    fun `updateBook updates existing book`() = runTest {
        // Given
        bookDao.insertBook(testBook)
        val updatedBook = testBook.copy(title = "Updated Title", updatedAt = Instant.now())

        // When
        bookDao.updateBook(updatedBook)

        // Then
        val retrieved = bookDao.getBookById("book-1")
        assertEquals("Updated Title", retrieved?.title)
    }

    @Test
    fun `updateBook updates multiple fields`() = runTest {
        // Given
        bookDao.insertBook(testBook)
        val updatedBook = testBook.copy(
            title = "New Title",
            description = "New Description",
            tags = "newTag1,newTag2",
            pageCount = 10,
            updatedAt = Instant.now(),
        )

        // When
        bookDao.updateBook(updatedBook)

        // Then
        val retrieved = bookDao.getBookById("book-1")
        assertEquals("New Title", retrieved?.title)
        assertEquals("New Description", retrieved?.description)
        assertEquals("newTag1,newTag2", retrieved?.tags)
        assertEquals(10, retrieved?.pageCount)
    }

    // endregion

    // region deleteBook Tests

    @Test
    fun `deleteBook removes book from database`() = runTest {
        // Given
        bookDao.insertBook(testBook)

        // When
        bookDao.deleteBook(testBook)

        // Then
        val retrieved = bookDao.getBookById("book-1")
        assertNull(retrieved)
    }

    @Test
    fun `deleteBook does not affect other books`() = runTest {
        // Given
        val book2 = testBook.copy(id = "book-2")
        bookDao.insertBook(testBook)
        bookDao.insertBook(book2)

        // When
        bookDao.deleteBook(testBook)

        // Then
        val remaining = bookDao.getAllBooks().first()
        assertEquals(1, remaining.size)
        assertEquals("book-2", remaining[0].id)
    }

    // endregion

    // region deleteBookImages Tests

    @Test
    fun `deleteBookImages removes specified images by id`() = runTest {
        // Given
        bookDao.insertBook(testBook)
        val image1 = testBookImage.copy(id = "img-1")
        val image2 = testBookImage.copy(id = "img-2")
        val image3 = testBookImage.copy(id = "img-3")
        bookDao.insertBookImages(listOf(image1, image2, image3))

        // When
        bookDao.deleteBookImages(listOf("img-1", "img-2"))

        // Then
        val remaining = bookDao.getBookImages("book-1").first()
        assertEquals(1, remaining.size)
        assertEquals("img-3", remaining[0].id)
    }

    @Test
    fun `deleteBookImages with empty list does nothing`() = runTest {
        // Given
        bookDao.insertBook(testBook)
        bookDao.insertBookImages(listOf(testBookImage))

        // When
        bookDao.deleteBookImages(emptyList())

        // Then
        val images = bookDao.getBookImages("book-1").first()
        assertEquals(1, images.size)
    }

    @Test
    fun `deleteBookImages with non-existent ids does nothing`() = runTest {
        // Given
        bookDao.insertBook(testBook)
        bookDao.insertBookImages(listOf(testBookImage))

        // When
        bookDao.deleteBookImages(listOf("non-existent"))

        // Then
        val images = bookDao.getBookImages("book-1").first()
        assertEquals(1, images.size)
    }

    // endregion

    // region deleteAllBookImagesForBook Tests

    @Test
    fun `deleteAllBookImagesForBook removes all images for book`() = runTest {
        // Given
        bookDao.insertBook(testBook)
        val image1 = testBookImage.copy(id = "img-1", pageNumber = 1)
        val image2 = testBookImage.copy(id = "img-2", pageNumber = 2)
        bookDao.insertBookImages(listOf(image1, image2))

        // When
        bookDao.deleteAllBookImagesForBook("book-1")

        // Then
        val images = bookDao.getBookImages("book-1").first()
        assertTrue(images.isEmpty())
    }

    @Test
    fun `deleteAllBookImagesForBook does not affect other books images`() = runTest {
        // Given
        val book2 = testBook.copy(id = "book-2")
        bookDao.insertBook(testBook)
        bookDao.insertBook(book2)

        val book1Image = testBookImage.copy(id = "img-1", bookId = "book-1")
        val book2Image = testBookImage.copy(id = "img-2", bookId = "book-2")
        bookDao.insertBookImages(listOf(book1Image, book2Image))

        // When
        bookDao.deleteAllBookImagesForBook("book-1")

        // Then
        val book1Images = bookDao.getBookImages("book-1").first()
        val book2Images = bookDao.getBookImages("book-2").first()

        assertTrue(book1Images.isEmpty())
        assertEquals(1, book2Images.size)
        assertEquals("img-2", book2Images[0].id)
    }

    @Test
    fun `deleteAllBookImagesForBook with no images does nothing`() = runTest {
        // Given
        bookDao.insertBook(testBook)

        // When
        bookDao.deleteAllBookImagesForBook("book-1")

        // Then
        val images = bookDao.getBookImages("book-1").first()
        assertTrue(images.isEmpty())
    }

    // endregion

    // region insertBooks (batch) Tests

    @Test
    fun `insertBooks inserts multiple books at once`() = runTest {
        // Given
        val book2 = testBook.copy(id = "book-2", title = "Book 2")
        val book3 = testBook.copy(id = "book-3", title = "Book 3")
        val books = listOf(testBook, book2, book3)

        // When
        bookDao.insertBooks(books)

        // Then
        val allBooks = bookDao.getAllBooks().first()
        assertEquals(3, allBooks.size)
    }

    @Test
    fun `insertBooks with empty list does nothing`() = runTest {
        // When
        bookDao.insertBooks(emptyList())

        // Then
        val allBooks = bookDao.getAllBooks().first()
        assertTrue(allBooks.isEmpty())
    }

    // endregion
}
