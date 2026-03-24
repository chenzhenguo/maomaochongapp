package com.maomaochongapp.picturebook.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.maomaochongapp.picturebook.core.image.ImageUtils
import com.maomaochongapp.picturebook.domain.model.Book
import com.maomaochongapp.picturebook.domain.model.BookImage
import com.maomaochongapp.picturebook.domain.repository.BookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

/**
 * UI state for PictureBook screen
 */
data class PictureBookUiState(
    val books: List<Book> = emptyList(),
    val filteredBooks: List<Book> = emptyList(),
    val searchQuery: String = "",
    val selectedTags: Set<String> = emptySet(),
    val allTags: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentBook: Book? = null,
    val bookImages: List<BookImage> = emptyList(),
    val isCreating: Boolean = false,
    val lastMessage: String? = null,
)

/**
 * ViewModel for picture book management
 */
class PictureBookViewModel(
    application: Application,
    private val bookRepository: BookRepository,
    private val imageUtils: ImageUtils,
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(PictureBookUiState())
    val state: StateFlow<PictureBookUiState> = _state

    init {
        loadAllBooks()
    }

    /**
     * Load all books from repository
     */
    fun loadAllBooks() {
        viewModelScope.launch {
            supervisorScope {
                _state.update { it.copy(isLoading = true, error = null) }
                try {
                    bookRepository.getAllBooks().collect { books ->
                        val allTags = books.flatMap { it.tags }.toSet()
                        _state.update { current ->
                            current.copy(
                                books = books,
                                allTags = allTags,
                                isLoading = false,
                                filteredBooks = applyFilters(books, current.searchQuery, current.selectedTags),
                            )
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
            }
        }
    }

    /**
     * Search books by query
     */
    fun searchBooks(query: String) {
        _state.update { it.copy(searchQuery = query) }
        applyCurrentFilters()
    }

    /**
     * Filter books by tags
     */
    fun filterByTags(tags: Set<String>) {
        _state.update { it.copy(selectedTags = tags) }
        applyCurrentFilters()
    }

    /**
     * Toggle a single tag filter
     */
    fun toggleTagFilter(tag: String) {
        _state.update { current ->
            val newTags = if (tag in current.selectedTags) {
                current.selectedTags - tag
            } else {
                current.selectedTags + tag
            }
            current.copy(selectedTags = newTags)
        }
        applyCurrentFilters()
    }

    /**
     * Clear all filters
     */
    fun clearFilters() {
        _state.update { it.copy(searchQuery = "", selectedTags = emptySet()) }
        applyCurrentFilters()
    }

    /**
     * Create a new book
     */
    fun createBook(title: String, description: String = "", tags: List<String> = emptyList()) {
        if (title.isBlank()) {
            _state.update { it.copy(error = "Title cannot be empty") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isCreating = true, error = null) }
            try {
                val book = Book(
                    id = UUID.randomUUID().toString(),
                    title = title.trim(),
                    description = description.trim(),
                    tags = tags.filter { it.isNotBlank() },
                    createdAt = Instant.now(),
                    updatedAt = Instant.now(),
                )
                val bookId = bookRepository.upsertBook(book)
                _state.update { current ->
                    current.copy(
                        isCreating = false,
                        lastMessage = "Book created successfully",
                        currentBook = book.copy(id = bookId),
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isCreating = false, error = e.message) }
            }
        }
    }

    /**
     * Update an existing book
     */
    fun updateBook(book: Book) {
        viewModelScope.launch {
            _state.update { it.copy(isCreating = true, error = null) }
            try {
                val updatedBook = book.copy(
                    updatedAt = Instant.now(),
                )
                bookRepository.upsertBook(updatedBook)
                _state.update { current ->
                    current.copy(
                        isCreating = false,
                        lastMessage = "Book updated successfully",
                        currentBook = updatedBook,
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isCreating = false, error = e.message) }
            }
        }
    }

    /**
     * Delete a book
     */
    fun deleteBook(bookId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                bookRepository.deleteBook(bookId)
                _state.update { current ->
                    current.copy(
                        isLoading = false,
                        lastMessage = "Book deleted successfully",
                        currentBook = if (current.currentBook?.id == bookId) null else current.currentBook,
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    /**
     * Load a specific book
     */
    fun loadBook(bookId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val book = bookRepository.getBookById(bookId)
                _state.update { current ->
                    current.copy(
                        isLoading = false,
                        currentBook = book,
                        error = if (book == null) "Book not found" else null,
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    /**
     * Load images for a book
     */
    fun loadBookImages(bookId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                bookRepository.getBookImages(bookId).collect { images ->
                    _state.update { current ->
                        current.copy(
                            isLoading = false,
                            bookImages = images,
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    /**
     * Add images to a book from URIs
     */
    fun addImagesToBook(bookId: String, imageUris: List<Uri>) {
        if (imageUris.isEmpty()) {
            _state.update { it.copy(error = "No images selected") }
            return
        }

        // Validate URIs before processing
        val validUris = mutableListOf<Uri>()
        for (uri in imageUris) {
            if (!isValidContentUri(uri)) {
                _state.update { it.copy(error = "Invalid URI scheme: ${uri.scheme}") }
                return
            }
            validUris.add(uri)
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val context = getApplication<Application>().applicationContext
                val book = _state.value.currentBook ?: bookRepository.getBookById(bookId)

                if (book == null) {
                    _state.update { it.copy(isLoading = false, error = "Book not found") }
                    return@launch
                }

                val currentImages = _state.value.bookImages
                val newImages = validUris.mapIndexed { index, uri ->
                    // Additional validation for image content
                    if (!imageUtils.isValidImage(context, uri)) {
                        throw IllegalArgumentException("Invalid image file: ${uri.lastPathSegment}")
                    }

                    val imageInfo = imageUtils.getImageInfo(context, uri)
                    BookImage(
                        id = UUID.randomUUID().toString(),
                        bookId = bookId,
                        originalFileName = imageInfo.fileName ?: "image_${index + 1}",
                        displayName = imageInfo.fileName ?: "image_${index + 1}",
                        uri = uri.toString(),
                        mimeType = imageInfo.mimeType ?: "image/jpeg",
                        fileSize = imageInfo.fileSize,
                        width = imageInfo.width,
                        height = imageInfo.height,
                        pageNumber = currentImages.size + index,
                        createdAt = Instant.now(),
                        updatedAt = Instant.now(),
                    )
                }

                bookRepository.upsertBookImages(newImages)

                // Update book page count and cover image if needed
                if (book.coverImageUri == null && newImages.isNotEmpty()) {
                    val updatedBook = book.copy(
                        pageCount = currentImages.size + newImages.size,
                        coverImageUri = newImages.first().uri,
                        updatedAt = Instant.now(),
                    )
                    bookRepository.upsertBook(updatedBook)
                    _state.update { it.copy(currentBook = updatedBook) }
                } else {
                    _state.update { current ->
                        current.copy(
                            bookImages = currentImages + newImages,
                        )
                    }
                }

                _state.update { it.copy(isLoading = false, lastMessage = "${newImages.size} images added") }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun isValidContentUri(uri: Uri): Boolean {
        return when (uri.scheme) {
            "content" -> {
                val authority = uri.authority
                trustedAuthorities.contains(authority)
            }
            else -> false
        }
    }

    private val trustedAuthorities = setOf(
        "com.android.externalstorage.documents",
        "com.android.providers.downloads.documents",
        "com.android.providers.media.documents"
    )

    /**
     * Remove images from a book
     */
    fun removeImages(imageIds: List<String>) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                bookRepository.deleteBookImages(imageIds)
                _state.update { current ->
                    current.copy(
                        isLoading = false,
                        bookImages = current.bookImages.filter { it.id !in imageIds },
                        lastMessage = "${imageIds.size} images removed",
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    /**
     * Clear last message
     */
    fun clearMessage() {
        _state.update { it.copy(lastMessage = null) }
    }

    private fun applyFilters(books: List<Book>, query: String, tags: Set<String>): List<Book> {
        return books.filter { book ->
            val matchesQuery = query.isBlank() ||
                book.title.contains(query, ignoreCase = true) ||
                book.description.contains(query, ignoreCase = true)
            val matchesTags = tags.isEmpty() || tags.any { it in book.tags }
            matchesQuery && matchesTags
        }
    }

    private fun applyCurrentFilters() {
        val currentState = _state.value
        _state.update { current ->
            current.copy(
                filteredBooks = applyFilters(currentState.books, currentState.searchQuery, currentState.selectedTags),
            )
        }
    }
}
