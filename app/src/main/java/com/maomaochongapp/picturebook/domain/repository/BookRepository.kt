package com.maomaochongapp.picturebook.domain.repository

import com.maomaochongapp.picturebook.domain.model.Book
import com.maomaochongapp.picturebook.domain.model.BookImage
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for picture book data operations
 */
interface BookRepository {
    /**
     * Get all books
     */
    fun getAllBooks(): Flow<List<Book>>

    /**
     * Get books by search query
     */
    fun searchBooks(query: String): Flow<List<Book>>

    /**
     * Get books by tags
     */
    fun getBooksByTags(tags: List<String>): Flow<List<Book>>

    /**
     * Get a book by ID
     */
    suspend fun getBookById(bookId: String): Book?

    /**
     * Get all images for a book
     */
    fun getBookImages(bookId: String): Flow<List<BookImage>>

    /**
     * Insert or update a book
     */
    suspend fun upsertBook(book: Book): String

    /**
     * Insert or update multiple book images
     */
    suspend fun upsertBookImages(images: List<BookImage>)

    /**
     * Delete a book and all its images
     */
    suspend fun deleteBook(bookId: String)

    /**
     * Delete specific images from a book
     */
    suspend fun deleteBookImages(imageIds: List<String>)
}