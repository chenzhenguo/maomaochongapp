package com.maomaochongapp.picturebook.data.mapper

import com.maomaochongapp.picturebook.data.local.BookEntity
import com.maomaochongapp.picturebook.data.local.BookImageEntity
import com.maomaochongapp.picturebook.domain.model.Book
import com.maomaochongapp.picturebook.domain.model.BookImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/**
 * Unit tests for BookMappers
 *
 * Tests cover:
 * - Happy path: normal conversion between entities and domain models
 * - Edge cases: null values, empty strings, empty lists
 * - Boundary values: special characters, unicode, long strings
 */
class BookMappersTest {

    companion object {
        private val TEST_INSTANT = Instant.ofEpochMilli(1000000)
        private const val TEST_ID = "test-id-123"
        private const val TEST_TITLE = "Test Book"
        private const val TEST_DESCRIPTION = "Test Description"
        private const val TEST_URI = "content://test/uri"
        private const val TEST_TAGS_CSV = "tag1,tag2,tag3"
    }

    // region toDomain() Tests

    @Test
    fun `BookEntity toDomain converts basic fields correctly`() {
        // Given
        val entity = BookEntity(
            id = TEST_ID,
            title = TEST_TITLE,
            description = TEST_DESCRIPTION,
            tags = TEST_TAGS_CSV,
            coverImageUri = TEST_URI,
            pageCount = 10,
            createdAt = TEST_INSTANT,
            updatedAt = TEST_INSTANT,
            sourceFolderUri = TEST_URI,
            exportPath = "/test/path",
        )

        // When
        val book = entity.toDomain()

        // Then
        assertEquals(TEST_ID, book.id)
        assertEquals(TEST_TITLE, book.title)
        assertEquals(TEST_DESCRIPTION, book.description)
        assertEquals(TEST_URI, book.coverImageUri)
        assertEquals(10, book.pageCount)
        assertEquals(TEST_INSTANT, book.createdAt)
        assertEquals(TEST_INSTANT, book.updatedAt)
        assertEquals(TEST_URI, book.sourceFolderUri)
        assertEquals("/test/path", book.exportPath)
    }

    @Test
    fun `BookEntity toDomain parses comma-separated tags correctly`() {
        // Given
        val entity = BookEntity(
            id = TEST_ID,
            title = TEST_TITLE,
            tags = "tag1,tag2,tag3",
        )

        // When
        val book = entity.toDomain()

        // Then
        assertEquals(listOf("tag1", "tag2", "tag3"), book.tags)
    }

    @Test
    fun `BookEntity toDomain handles empty tags string`() {
        // Given
        val entity = BookEntity(
            id = TEST_ID,
            title = TEST_TITLE,
            tags = "",
        )

        // When
        val book = entity.toDomain()

        // Then
        assertTrue(book.tags.isEmpty())
    }

    @Test
    fun `BookEntity toDomain handles single tag without comma`() {
        // Given
        val entity = BookEntity(
            id = TEST_ID,
            title = TEST_TITLE,
            tags = "singleTag",
        )

        // When
        val book = entity.toDomain()

        // Then
        assertEquals(listOf("singleTag"), book.tags)
    }

    @Test
    fun `BookEntity toDomain trims whitespace from tags`() {
        // Given
        val entity = BookEntity(
            id = TEST_ID,
            title = TEST_TITLE,
            tags = " tag1 , tag2 , tag3 ",
        )

        // When
        val book = entity.toDomain()

        // Then
        assertEquals(listOf("tag1", "tag2", "tag3"), book.tags)
    }

    @Test
    fun `BookEntity toDomain handles null coverImageUri`() {
        // Given
        val entity = BookEntity(
            id = TEST_ID,
            title = TEST_TITLE,
            coverImageUri = null,
        )

        // When
        val book = entity.toDomain()

        // Then
        assertEquals(null, book.coverImageUri)
    }

    @Test
    fun `BookEntity toDomain handles null sourceFolderUri`() {
        // Given
        val entity = BookEntity(
            id = TEST_ID,
            title = TEST_TITLE,
            sourceFolderUri = null,
        )

        // When
        val book = entity.toDomain()

        // Then
        assertEquals(null, book.sourceFolderUri)
    }

    @Test
    fun `BookEntity toDomain handles null exportPath`() {
        // Given
        val entity = BookEntity(
            id = TEST_ID,
            title = TEST_TITLE,
            exportPath = null,
        )

        // When
        val book = entity.toDomain()

        // Then
        assertEquals(null, book.exportPath)
    }

    @Test
    fun `BookEntity toDomain handles empty description`() {
        // Given
        val entity = BookEntity(
            id = TEST_ID,
            title = TEST_TITLE,
            description = "",
        )

        // When
        val book = entity.toDomain()

        // Then
        assertEquals("", book.description)
    }

    @Test
    fun `BookEntity toDomain handles zero pageCount`() {
        // Given
        val entity = BookEntity(
            id = TEST_ID,
            title = TEST_TITLE,
            pageCount = 0,
        )

        // When
        val book = entity.toDomain()

        // Then
        assertEquals(0, book.pageCount)
    }

    @Test
    fun `BookEntity toDomain handles unicode characters in title`() {
        // Given
        val entity = BookEntity(
            id = TEST_ID,
            title = "测试绘本",
            description = "Descripcion en español",
        )

        // When
        val book = entity.toDomain()

        // Then
        assertEquals("测试绘本", book.title)
        assertEquals("Descripcion en español", book.description)
    }

    @Test
    fun `BookEntity toDomain handles emoji in description`() {
        // Given
        val entity = BookEntity(
            id = TEST_ID,
            title = TEST_TITLE,
            description = "A book with 🎨 and 📚",
        )

        // When
        val book = entity.toDomain()

        // Then
        assertEquals("A book with 🎨 and 📚", book.description)
    }

    @Test
    fun `BookEntity toDomain handles very long title`() {
        // Given
        val longTitle = "A".repeat(1000)
        val entity = BookEntity(
            id = TEST_ID,
            title = longTitle,
        )

        // When
        val book = entity.toDomain()

        // Then
        assertEquals(longTitle, book.title)
        assertEquals(1000, book.title.length)
    }

    @Test
    fun `BookEntity toDomain handles special characters in title`() {
        // Given
        val entity = BookEntity(
            id = TEST_ID,
            title = "Book: \"Title\" & <Tags>",
            description = "Description with 'quotes' and \"double quotes\"",
        )

        // When
        val book = entity.toDomain()

        // Then
        assertEquals("Book: \"Title\" & <Tags>", book.title)
        assertEquals("Description with 'quotes' and \"double quotes\"", book.description)
    }

    // endregion

    // region toEntity() Tests

    @Test
    fun `Book toEntity converts basic fields correctly`() {
        // Given
        val book = Book(
            id = TEST_ID,
            title = TEST_TITLE,
            description = TEST_DESCRIPTION,
            tags = listOf("tag1", "tag2", "tag3"),
            coverImageUri = TEST_URI,
            pageCount = 10,
            createdAt = TEST_INSTANT,
            updatedAt = TEST_INSTANT,
            sourceFolderUri = TEST_URI,
            exportPath = "/test/path",
        )

        // When
        val entity = book.toEntity()

        // Then
        assertEquals(TEST_ID, entity.id)
        assertEquals(TEST_TITLE, entity.title)
        assertEquals(TEST_DESCRIPTION, entity.description)
        assertEquals("tag1,tag2,tag3", entity.tags)
        assertEquals(TEST_URI, entity.coverImageUri)
        assertEquals(10, entity.pageCount)
        assertEquals(TEST_INSTANT, entity.createdAt)
        assertEquals(TEST_INSTANT, entity.updatedAt)
        assertEquals(TEST_URI, entity.sourceFolderUri)
        assertEquals("/test/path", entity.exportPath)
    }

    @Test
    fun `Book toEntity converts tags list to comma-separated string`() {
        // Given
        val book = Book(
            id = TEST_ID,
            title = TEST_TITLE,
            tags = listOf("tag1", "tag2", "tag3"),
        )

        // When
        val entity = book.toEntity()

        // Then
        assertEquals("tag1,tag2,tag3", entity.tags)
    }

    @Test
    fun `Book toEntity handles empty tags list`() {
        // Given
        val book = Book(
            id = TEST_ID,
            title = TEST_TITLE,
            tags = emptyList(),
        )

        // When
        val entity = book.toEntity()

        // Then
        assertEquals("", entity.tags)
    }

    @Test
    fun `Book toEntity handles null coverImageUri`() {
        // Given
        val book = Book(
            id = TEST_ID,
            title = TEST_TITLE,
            coverImageUri = null,
        )

        // When
        val entity = book.toEntity()

        // Then
        assertEquals(null, entity.coverImageUri)
    }

    @Test
    fun `Book toEntity handles null sourceFolderUri`() {
        // Given
        val book = Book(
            id = TEST_ID,
            title = TEST_TITLE,
            sourceFolderUri = null,
        )

        // When
        val entity = book.toEntity()

        // Then
        assertEquals(null, entity.sourceFolderUri)
    }

    @Test
    fun `Book toEntity handles null exportPath`() {
        // Given
        val book = Book(
            id = TEST_ID,
            title = TEST_TITLE,
            exportPath = null,
        )

        // When
        val entity = book.toEntity()

        // Then
        assertEquals(null, entity.exportPath)
    }

    @Test
    fun `Book toEntity handles empty description`() {
        // Given
        val book = Book(
            id = TEST_ID,
            title = TEST_TITLE,
            description = "",
        )

        // When
        val entity = book.toEntity()

        // Then
        assertEquals("", entity.description)
    }

    @Test
    fun `Book toEntity handles zero pageCount`() {
        // Given
        val book = Book(
            id = TEST_ID,
            title = TEST_TITLE,
            pageCount = 0,
        )

        // When
        val entity = book.toEntity()

        // Then
        assertEquals(0, entity.pageCount)
    }

    @Test
    fun `Book toEntity with single tag creates correct csv`() {
        // Given
        val book = Book(
            id = TEST_ID,
            title = TEST_TITLE,
            tags = listOf("onlyTag"),
        )

        // When
        val entity = book.toEntity()

        // Then
        assertEquals("onlyTag", entity.tags)
    }

    // endregion

    // region BookImageEntity toDomain() Tests

    @Test
    fun `BookImageEntity toDomain converts all fields correctly`() {
        // Given
        val entity = BookImageEntity(
            id = "image-id",
            bookId = TEST_ID,
            originalFileName = "original.jpg",
            displayName = "display.jpg",
            uri = TEST_URI,
            mimeType = "image/jpeg",
            fileSize = 1024L,
            width = 800,
            height = 600,
            pageNumber = 1,
            createdAt = TEST_INSTANT,
            updatedAt = TEST_INSTANT,
        )

        // When
        val bookImage = entity.toDomain()

        // Then
        assertEquals("image-id", bookImage.id)
        assertEquals(TEST_ID, bookImage.bookId)
        assertEquals("original.jpg", bookImage.originalFileName)
        assertEquals("display.jpg", bookImage.displayName)
        assertEquals(TEST_URI, bookImage.uri)
        assertEquals("image/jpeg", bookImage.mimeType)
        assertEquals(1024L, bookImage.fileSize)
        assertEquals(800, bookImage.width)
        assertEquals(600, bookImage.height)
        assertEquals(1, bookImage.pageNumber)
        assertEquals(TEST_INSTANT, bookImage.createdAt)
        assertEquals(TEST_INSTANT, bookImage.updatedAt)
    }

    @Test
    fun `BookImageEntity toDomain handles empty displayName`() {
        // Given
        val entity = BookImageEntity(
            id = "image-id",
            bookId = TEST_ID,
            originalFileName = "original.jpg",
            displayName = "",
            uri = TEST_URI,
            mimeType = "image/jpeg",
        )

        // When
        val bookImage = entity.toDomain()

        // Then
        assertEquals("", bookImage.displayName)
    }

    @Test
    fun `BookImageEntity toDomain handles zero fileSize`() {
        // Given
        val entity = BookImageEntity(
            id = "image-id",
            bookId = TEST_ID,
            originalFileName = "original.jpg",
            uri = TEST_URI,
            mimeType = "image/jpeg",
            fileSize = 0L,
        )

        // When
        val bookImage = entity.toDomain()

        // Then
        assertEquals(0L, bookImage.fileSize)
    }

    @Test
    fun `BookImageEntity toDomain handles zero dimensions`() {
        // Given
        val entity = BookImageEntity(
            id = "image-id",
            bookId = TEST_ID,
            originalFileName = "original.jpg",
            uri = TEST_URI,
            mimeType = "image/jpeg",
            width = 0,
            height = 0,
        )

        // When
        val bookImage = entity.toDomain()

        // Then
        assertEquals(0, bookImage.width)
        assertEquals(0, bookImage.height)
    }

    @Test
    fun `BookImageEntity toDomain handles zero pageNumber`() {
        // Given
        val entity = BookImageEntity(
            id = "image-id",
            bookId = TEST_ID,
            originalFileName = "original.jpg",
            uri = TEST_URI,
            mimeType = "image/jpeg",
            pageNumber = 0,
        )

        // When
        val bookImage = entity.toDomain()

        // Then
        assertEquals(0, bookImage.pageNumber)
    }

    @Test
    fun `BookImageEntity toDomain handles unicode in fileName`() {
        // Given
        val entity = BookImageEntity(
            id = "image-id",
            bookId = TEST_ID,
            originalFileName = "图像.jpg",
            displayName = "画像.png",
            uri = TEST_URI,
            mimeType = "image/jpeg",
        )

        // When
        val bookImage = entity.toDomain()

        // Then
        assertEquals("图像.jpg", bookImage.originalFileName)
        assertEquals("画像.png", bookImage.displayName)
    }

    // endregion

    // region BookImage toEntity() Tests

    @Test
    fun `BookImage toEntity converts all fields correctly`() {
        // Given
        val bookImage = BookImage(
            id = "image-id",
            bookId = TEST_ID,
            originalFileName = "original.jpg",
            displayName = "display.jpg",
            uri = TEST_URI,
            mimeType = "image/jpeg",
            fileSize = 1024L,
            width = 800,
            height = 600,
            pageNumber = 1,
            createdAt = TEST_INSTANT,
            updatedAt = TEST_INSTANT,
        )

        // When
        val entity = bookImage.toEntity()

        // Then
        assertEquals("image-id", entity.id)
        assertEquals(TEST_ID, entity.bookId)
        assertEquals("original.jpg", entity.originalFileName)
        assertEquals("display.jpg", entity.displayName)
        assertEquals(TEST_URI, entity.uri)
        assertEquals("image/jpeg", entity.mimeType)
        assertEquals(1024L, entity.fileSize)
        assertEquals(800, entity.width)
        assertEquals(600, entity.height)
        assertEquals(1, entity.pageNumber)
        assertEquals(TEST_INSTANT, entity.createdAt)
        assertEquals(TEST_INSTANT, entity.updatedAt)
    }

    @Test
    fun `BookImage toEntity handles empty displayName`() {
        // Given
        val bookImage = BookImage(
            id = "image-id",
            bookId = TEST_ID,
            originalFileName = "original.jpg",
            displayName = "",
            uri = TEST_URI,
            mimeType = "image/jpeg",
        )

        // When
        val entity = bookImage.toEntity()

        // Then
        assertEquals("", entity.displayName)
    }

    @Test
    fun `BookImage toEntity handles zero values`() {
        // Given
        val bookImage = BookImage(
            id = "image-id",
            bookId = TEST_ID,
            originalFileName = "original.jpg",
            uri = TEST_URI,
            mimeType = "image/jpeg",
            fileSize = 0L,
            width = 0,
            height = 0,
            pageNumber = 0,
        )

        // When
        val entity = bookImage.toEntity()

        // Then
        assertEquals(0L, entity.fileSize)
        assertEquals(0, entity.width)
        assertEquals(0, entity.height)
        assertEquals(0, entity.pageNumber)
    }

    // endregion

    // region Round-trip conversion Tests

    @Test
    fun `BookEntity toDomain toEntity round-trip preserves data`() {
        // Given
        val originalEntity = BookEntity(
            id = TEST_ID,
            title = TEST_TITLE,
            description = TEST_DESCRIPTION,
            tags = "tag1,tag2",
            coverImageUri = TEST_URI,
            pageCount = 5,
            createdAt = TEST_INSTANT,
            updatedAt = TEST_INSTANT,
            sourceFolderUri = TEST_URI,
            exportPath = "/path",
        )

        // When
        val book = originalEntity.toDomain()
        val convertedEntity = book.toEntity()

        // Then
        assertEquals(originalEntity.id, convertedEntity.id)
        assertEquals(originalEntity.title, convertedEntity.title)
        assertEquals(originalEntity.description, convertedEntity.description)
        assertEquals(originalEntity.tags, convertedEntity.tags)
        assertEquals(originalEntity.coverImageUri, convertedEntity.coverImageUri)
        assertEquals(originalEntity.pageCount, convertedEntity.pageCount)
        assertEquals(originalEntity.sourceFolderUri, convertedEntity.sourceFolderUri)
        assertEquals(originalEntity.exportPath, convertedEntity.exportPath)
    }

    @Test
    fun `Book toEntity toDomain round-trip preserves data except tags format`() {
        // Given
        val originalBook = Book(
            id = TEST_ID,
            title = TEST_TITLE,
            description = TEST_DESCRIPTION,
            tags = listOf("tag1", "tag2"),
            coverImageUri = TEST_URI,
            pageCount = 5,
            createdAt = TEST_INSTANT,
            updatedAt = TEST_INSTANT,
            sourceFolderUri = TEST_URI,
            exportPath = "/path",
        )

        // When
        val entity = originalBook.toEntity()
        val convertedBook = entity.toDomain()

        // Then
        assertEquals(originalBook.id, convertedBook.id)
        assertEquals(originalBook.title, convertedBook.title)
        assertEquals(originalBook.description, convertedBook.description)
        assertEquals(originalBook.tags, convertedBook.tags)
        assertEquals(originalBook.coverImageUri, convertedBook.coverImageUri)
        assertEquals(originalBook.pageCount, convertedBook.pageCount)
        assertEquals(originalBook.sourceFolderUri, convertedBook.sourceFolderUri)
        assertEquals(originalBook.exportPath, convertedBook.exportPath)
    }

    @Test
    fun `BookImageEntity toDomain toEntity round-trip preserves data`() {
        // Given
        val originalEntity = BookImageEntity(
            id = "img-1",
            bookId = TEST_ID,
            originalFileName = "test.jpg",
            displayName = "test_display.jpg",
            uri = TEST_URI,
            mimeType = "image/jpeg",
            fileSize = 2048L,
            width = 1920,
            height = 1080,
            pageNumber = 3,
            createdAt = TEST_INSTANT,
            updatedAt = TEST_INSTANT,
        )

        // When
        val bookImage = originalEntity.toDomain()
        val convertedEntity = bookImage.toEntity()

        // Then
        assertEquals(originalEntity.id, convertedEntity.id)
        assertEquals(originalEntity.bookId, convertedEntity.bookId)
        assertEquals(originalEntity.originalFileName, convertedEntity.originalFileName)
        assertEquals(originalEntity.displayName, convertedEntity.displayName)
        assertEquals(originalEntity.uri, convertedEntity.uri)
        assertEquals(originalEntity.mimeType, convertedEntity.mimeType)
        assertEquals(originalEntity.fileSize, convertedEntity.fileSize)
        assertEquals(originalEntity.width, convertedEntity.width)
        assertEquals(originalEntity.height, convertedEntity.height)
        assertEquals(originalEntity.pageNumber, convertedEntity.pageNumber)
    }

    // endregion
}
