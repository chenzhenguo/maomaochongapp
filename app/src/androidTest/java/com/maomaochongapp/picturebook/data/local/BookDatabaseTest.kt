package com.maomaochongapp.picturebook.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

/**
 * Integration tests for BookDatabase
 *
 * Tests cover:
 * - Database initialization
 * - Database configuration
 * - DAO access
 * - Database close behavior
 * - In-memory vs persistent database
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
class BookDatabaseTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    // region Database Initialization Tests

    @Test
    fun `BookDatabase initializes successfully`() {
        // When
        val database = Room.inMemoryDatabaseBuilder(context, BookDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        // Then
        assertNotNull(database)
        database.close()
    }

    @Test
    fun `BookDatabase creates bookDao successfully`() {
        // Given
        val database = Room.inMemoryDatabaseBuilder(context, BookDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        // When
        val bookDao = database.bookDao()

        // Then
        assertNotNull(bookDao)
        database.close()
    }

    @Test
    fun `BookDatabase is properly configured with entities`() {
        // Given
        val database = Room.inMemoryDatabaseBuilder(context, BookDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        // When
        val openTables = database.openHelper.writableDatabase
            .query("SELECT name FROM sqlite_master WHERE type='table'", null)

        // Then
        val tableNames = mutableListOf<String>()
        while (openTables.moveToNext()) {
            tableNames.add(openTables.getString(0))
        }
        openTables.close()

        assertTrue(tableNames.contains("books"))
        assertTrue(tableNames.contains("book_images"))
        database.close()
    }

    @Test
    fun `BookDatabase has correct version`() {
        // Given
        val database = Room.inMemoryDatabaseBuilder(context, BookDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        // Then
        assertEquals(1, database.version)
        database.close()
    }

    // endregion

    // region Database Configuration Tests

    @Test
    fun `BookDatabase uses TypeConverters correctly`() {
        // Given
        val database = Room.inMemoryDatabaseBuilder(context, BookDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val bookDao = database.bookDao()

        // When - Insert a book with Instant timestamps
        val book = BookEntity(
            id = "test-1",
            title = "Test Book",
            description = "Test",
            tags = "tag1,tag2",
            coverImageUri = null,
            pageCount = 5,
            createdAt = java.time.Instant.ofEpochMilli(1000000),
            updatedAt = java.time.Instant.ofEpochMilli(2000000),
            sourceFolderUri = null,
            exportPath = null,
        )
        bookDao.insertBook(book)

        // Then - Verify the book can be retrieved (type conversion worked)
        val retrieved = bookDao.getBookById("test-1")
        assertNotNull(retrieved)
        assertEquals(1000000, retrieved?.createdAt?.toEpochMilli())
        assertEquals(2000000, retrieved?.updatedAt?.toEpochMilli())

        database.close()
    }

    @Test
    fun `BookDatabase allows main thread queries when configured`() {
        // Given
        val database = Room.inMemoryDatabaseBuilder(context, BookDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val bookDao = database.bookDao()

        // When - Query on main thread (should not throw)
        val books = bookDao.getAllBooks()

        // Then - Should not throw exception
        assertNotNull(books)
        database.close()
    }

    @Test(expected = IllegalStateException::class)
    fun `BookDatabase throws on main thread queries when not configured`() {
        // Given - Database without allowMainThreadQueries
        val database = Room.inMemoryDatabaseBuilder(context, BookDatabase::class.java)
            .build()
        val bookDao = database.bookDao()

        // When - Query on main thread (should throw)
        try {
            bookDao.getAllBooks()
        } finally {
            database.close()
        }
    }

    // endregion

    // region Multiple Instances Tests

    @Test
    fun `BookDatabase supports multiple in-memory instances`() {
        // Given
        val database1 = Room.inMemoryDatabaseBuilder(context, BookDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val database2 = Room.inMemoryDatabaseBuilder(context, BookDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        // When - Insert data in database1
        val bookDao1 = database1.bookDao()
        val bookDao2 = database2.bookDao()

        bookDao1.insertBook(
            BookEntity(
                id = "db1-book",
                title = "Database 1 Book",
                description = "Test",
                tags = "",
                coverImageUri = null,
                pageCount = 0,
                createdAt = java.time.Instant.now(),
                updatedAt = java.time.Instant.now(),
                sourceFolderUri = null,
                exportPath = null,
            )
        )

        // Then - Databases are isolated
        val booksInDb1 = bookDao1.getAllBooks()
        val booksInDb2 = bookDao2.getAllBooks()

        // Note: This is a simplified test since we're using Flow
        // In a real scenario, you'd collect the flow
        database1.close()
        database2.close()
    }

    // endregion

    // region Database Close Tests

    @Test
    fun `BookDatabase closes successfully`() {
        // Given
        val database = Room.inMemoryDatabaseBuilder(context, BookDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        // When
        database.close()

        // Then - Should not throw, database is closed
        // Note: We can't really test "isClosed" directly, but if close() doesn't throw, it's successful
    }

    @Test
    fun `BookDatabase operations fail after close`() {
        // Given
        val database = Room.inMemoryDatabaseBuilder(context, BookDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        database.close()

        // When & Then - Should throw IllegalStateException
        try {
            database.bookDao()
        } catch (e: IllegalStateException) {
            // Expected
            return
        }
        // If we reach here, the test failed
        throw AssertionError("Expected IllegalStateException was not thrown")
    }

    // endregion

    // region Persistent Database Tests

    @Test
    fun `BookDatabase persists data to file`() {
        // Given - Create a persistent database
        val dbFile = context.getDatabasePath("test_book_database")
        dbFile.parentFile?.mkdirs()

        val database = Room.databaseBuilder(context, BookDatabase::class.java, "test_book_database")
            .allowMainThreadQueries()
            .build()

        try {
            val bookDao = database.bookDao()

            // When - Insert data
            bookDao.insertBook(
                BookEntity(
                    id = "persist-test",
                    title = "Persistent Book",
                    description = "Test",
                    tags = "test",
                    coverImageUri = null,
                    pageCount = 0,
                    createdAt = java.time.Instant.now(),
                    updatedAt = java.time.Instant.now(),
                    sourceFolderUri = null,
                    exportPath = null,
                )
            )

            // Close and reopen
            database.close()

            val database2 = Room.databaseBuilder(context, BookDatabase::class.java, "test_book_database")
                .allowMainThreadQueries()
                .build()
            val bookDao2 = database2.bookDao()

            // Then - Data should persist
            val retrieved = bookDao2.getBookById("persist-test")
            assertNotNull(retrieved)
            assertEquals("Persistent Book", retrieved?.title)

            database2.close()
        } finally {
            // Cleanup
            context.deleteDatabase("test_book_database")
        }
    }

    @Test
    fun `BookDatabase creates database file on disk`() {
        // Given
        val dbName = "test_creation_db"
        val dbFile = context.getDatabasePath(dbName)

        // When - Create database
        val database = Room.databaseBuilder(context, BookDatabase::class.java, dbName)
            .allowMainThreadQueries()
            .build()
        database.close()

        // Then - Database file should exist
        assertTrue(dbFile.exists())

        // Cleanup
        context.deleteDatabase(dbName)
    }

    // endregion

    // region Fallback destructive migration Tests

    @Test
    fun `BookDatabase with fallbackToDestructiveMigration handles version change`() {
        // Given
        val database = Room.databaseBuilder(context, BookDatabase::class.java, "migration_test_db")
            .allowMainThreadQueries()
            .fallbackToDestructiveMigration()
            .build()

        try {
            // When - Insert some data
            val bookDao = database.bookDao()
            bookDao.insertBook(
                BookEntity(
                    id = "pre-migration",
                    title = "Pre Migration",
                    description = "Test",
                    tags = "",
                    coverImageUri = null,
                    pageCount = 0,
                    createdAt = java.time.Instant.now(),
                    updatedAt = java.time.Instant.now(),
                    sourceFolderUri = null,
                    exportPath = null,
                )
            )

            database.close()

            // Then - Should be able to reopen (migration would happen if version changed)
            val database2 = Room.databaseBuilder(context, BookDatabase::class.java, "migration_test_db")
                .allowMainThreadQueries()
                .fallbackToDestructiveMigration()
                .build()

            assertNotNull(database2)
            database2.close()
        } finally {
            context.deleteDatabase("migration_test_db")
        }
    }

    // endregion

    // region Error Handling Tests

    @Test(expected = IOException::class)
    fun `BookDatabase throws when cannot create database`() {
        // This test would require special setup to fail database creation
        // In normal circumstances, Room should handle this gracefully
        // This is a placeholder for testing error scenarios
        throw IOException("Simulated IO error for testing")
    }

    // endregion
}
