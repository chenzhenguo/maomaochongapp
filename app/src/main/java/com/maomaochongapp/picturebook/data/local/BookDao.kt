package com.maomaochongapp.picturebook.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for picture book operations
 */
@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY updated_at DESC")
    fun getAllBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%' ORDER BY updated_at DESC")
    fun searchBooks(query: String): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE tags LIKE '%' || :tag || '%' ORDER BY updated_at DESC")
    fun getBooksByTag(tag: String): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :bookId")
    suspend fun getBookById(bookId: String): BookEntity?

    @Query("SELECT * FROM book_images WHERE book_id = :bookId ORDER BY page_number ASC")
    fun getBookImages(bookId: String): Flow<List<BookImageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooks(books: List<BookEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookImages(images: List<BookImageEntity>)

    @Update
    suspend fun updateBook(book: BookEntity)

    @Delete
    suspend fun deleteBook(book: BookEntity)

    @Query("DELETE FROM book_images WHERE id IN (:imageIds)")
    suspend fun deleteBookImages(imageIds: List<String>)

    @Query("DELETE FROM book_images WHERE book_id = :bookId")
    suspend fun deleteAllBookImagesForBook(bookId: String)
}