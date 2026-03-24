package com.maomaochongapp.picturebook.data.repository

import com.maomaochongapp.picturebook.data.local.BookDao
import com.maomaochongapp.picturebook.data.mapper.BookMappers
import com.maomaochongapp.picturebook.domain.model.Book
import com.maomaochongapp.picturebook.domain.model.BookImage
import com.maomaochongapp.picturebook.domain.repository.BookRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository implementation for picture book data operations
 */
class BookRepositoryImpl(
    private val bookDao: BookDao
) : BookRepository {
    override fun getAllBooks(): Flow<List<Book>> {
        return bookDao.getAllBooks().map { entities -> entities.map { it.toDomain() } }
    }

    override fun searchBooks(query: String): Flow<List<Book>> {
        return bookDao.searchBooks(query).map { entities -> entities.map { it.toDomain() } }
    }

    override fun getBooksByTags(tags: List<String>): Flow<List<Book>> {
        // For simplicity, we'll search by the first tag if multiple tags are provided
        // In a real app, you might want to implement more complex tag filtering
        return if (tags.isNotEmpty()) {
            bookDao.getBooksByTag(tags[0]).map { entities -> entities.map { it.toDomain() } }
        } else {
            getAllBooks()
        }
    }

    override suspend fun getBookById(bookId: String): Book? {
        val entity = bookDao.getBookById(bookId)
        return entity?.toDomain()
    }

    override fun getBookImages(bookId: String): Flow<List<BookImage>> {
        return bookDao.getBookImages(bookId).map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun upsertBook(book: Book): String {
        val entity = book.toEntity()
        bookDao.insertBook(entity)
        return book.id
    }

    override suspend fun upsertBookImages(images: List<BookImage>) {
        val entities = images.map { it.toEntity() }
        bookDao.insertBookImages(entities)
    }

    override suspend fun deleteBook(bookId: String) {
        val book = getBookById(bookId)
        if (book != null) {
            bookDao.deleteAllBookImagesForBook(bookId)
            bookDao.deleteBook(book.toEntity())
        }
    }

    override suspend fun deleteBookImages(imageIds: List<String>) {
        bookDao.deleteBookImages(imageIds)
    }
}