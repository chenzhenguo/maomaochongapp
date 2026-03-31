package com.maomaochongapp.ui.tabs

import android.app.Application
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import com.maomaochongapp.picturebook.core.image.ImageUtils
import com.maomaochongapp.picturebook.data.local.BookDatabase
import com.maomaochongapp.picturebook.data.repository.BookRepositoryImpl
import com.maomaochongapp.picturebook.domain.model.Book
import com.maomaochongapp.picturebook.domain.model.BookImage
import com.maomaochongapp.picturebook.ui.viewmodel.PictureBookUiState
import com.maomaochongapp.picturebook.ui.viewmodel.PictureBookViewModel
import kotlinx.coroutines.flow.StateFlow

/**
 * Picture Book Management Tab
 *
 * Complete implementation of the picture book management feature with:
 * - Book creation, editing, and deletion
 * - Tag-based filtering and searching
 * - Image management capabilities
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PictureBookTab(
    modifier: Modifier = Modifier,
    viewModel: PictureBookViewModel = viewModel(
        factory = PictureBookViewModelFactory(LocalContext.current as Application)
    )
) {
    val state by viewModel.state.collectAsState()

    PictureBookScreen(
        state = state,
        onEvent = { event ->
            when (event) {
                is PictureBookEvent.CreateBook -> {
                    viewModel.createBook(event.title, event.description, event.tags)
                }
                is PictureBookEvent.UpdateBook -> {
                    viewModel.updateBook(event.book)
                }
                is PictureBookEvent.DeleteBook -> {
                    viewModel.deleteBook(event.bookId)
                }
                is PictureBookEvent.LoadBook -> {
                    viewModel.loadBook(event.bookId)
                    viewModel.loadBookImages(event.bookId)
                }
                is PictureBookEvent.ClearCurrentBook -> {
                    // Clear the current book selection
                    viewModel.clearCurrentBook()
                }
                is PictureBookEvent.SearchBooks -> {
                    viewModel.searchBooks(event.query)
                }
                is PictureBookEvent.FilterByTags -> {
                    viewModel.filterByTags(event.tags)
                }
                is PictureBookEvent.ToggleTagFilter -> {
                    viewModel.toggleTagFilter(event.tag)
                }
                is PictureBookEvent.ClearFilters -> {
                    viewModel.clearFilters()
                }
                is PictureBookEvent.ClearError -> {
                    viewModel.clearError()
                }
                is PictureBookEvent.ClearMessage -> {
                    viewModel.clearMessage()
                }
            }
        },
        modifier = modifier
    )
}

@Composable
private fun PictureBookScreen(
    state: PictureBookUiState,
    onEvent: (PictureBookEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Error and message display
        if (state.error != null) {
            ErrorMessage(message = state.error) {
                onEvent(PictureBookEvent.ClearError)
            }
        }

        if (state.lastMessage != null) {
            SuccessMessage(message = state.lastMessage) {
                onEvent(PictureBookEvent.ClearMessage)
            }
        }

        // Search and filter controls
        SearchAndFilterControls(
            searchQuery = state.searchQuery,
            selectedTags = state.selectedTags,
            allTags = state.allTags,
            onSearch = { query -> onEvent(PictureBookEvent.SearchBooks(query)) },
            onToggleTag = { tag -> onEvent(PictureBookEvent.ToggleTagFilter(tag)) },
            onClearFilters = { onEvent(PictureBookEvent.ClearFilters()) }
        )

        // Create new book form (shown when no current book selected)
        if (state.currentBook == null) {
            CreateBookForm(
                onCreate = { title, description, tags ->
                    onEvent(PictureBookEvent.CreateBook(title, description, tags))
                },
                enabled = !state.isCreating && !state.isLoading
            )
        }

        // Book detail view (shown when current book selected)
        state.currentBook?.let { book ->
            BookDetailView(
                book = book,
                images = state.bookImages,
                onUpdate = { updatedBook -> onEvent(PictureBookEvent.UpdateBook(updatedBook)) },
                onDelete = { onEvent(PictureBookEvent.DeleteBook(book.id)) },
                onBack = {
                    // Clear the current book selection to return to the list view
                    onEvent(PictureBookEvent.ClearCurrentBook)
                },
                enabled = !state.isCreating && !state.isLoading
            )
        }

        // Book list (only shown when no current book selected)
        if (state.currentBook == null) {
            BookList(
                books = state.filteredBooks,
                onSelect = { bookId ->
                    // Load book details when selected
                    onEvent(PictureBookEvent.LoadBook(bookId))
                },
                onDelete = { bookId -> onEvent(PictureBookEvent.DeleteBook(bookId)) },
                isLoading = state.isLoading
            )
        }

        // Loading indicator
        if (state.isLoading || state.isCreating) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
private fun ErrorMessage(message: String, onDismiss: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss error",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
private fun SuccessMessage(message: String, onDismiss: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss message",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun SearchAndFilterControls(
    searchQuery: String,
    selectedTags: Set<String>,
    allTags: Set<String>,
    onSearch: (String) -> Unit,
    onToggleTag: (String) -> Unit,
    onClearFilters: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearch,
            label = { Text("Search books") },
            modifier = Modifier.fillMaxWidth()
        )

        if (allTags.isNotEmpty()) {
            Text("Filter by tags:", style = MaterialTheme.typography.labelMedium)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                allTags.forEach { tag ->
                    FilterChip(
                        selected = tag in selectedTags,
                        onClick = { onToggleTag(tag) },
                        label = { Text(tag) }
                    )
                }
            }
        }

        if (searchQuery.isNotEmpty() || selectedTags.isNotEmpty()) {
            Button(
                onClick = onClearFilters,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Clear filters")
            }
        }
    }
}

@Composable
private fun CreateBookForm(
    onCreate: (String, String, List<String>) -> Unit,
    enabled: Boolean
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var tagsText by remember { mutableStateOf("") }

    val tags = tagsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Create New Picture Book", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title *") },
            isError = title.isBlank(),
            supportingText = if (title.isBlank()) {
                { Text("Title cannot be empty") }
            } else null
        )

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = tagsText,
            onValueChange = { tagsText = it },
            label = { Text("Tags (comma-separated)") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = { onCreate(title, description, tags) },
            enabled = title.isNotBlank() && enabled
        ) {
            Text("Create Book")
        }
    }
}

@Composable
private fun BookDetailView(
    book: Book,
    images: List<BookImage>,
    onUpdate: (Book) -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
    enabled: Boolean
) {
    var title by remember { mutableStateOf(book.title) }
    var description by remember { mutableStateOf(book.description) }
    var tagsText by remember { mutableStateOf(book.tags.joinToString(", ")) }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("Edit Picture Book", style = MaterialTheme.typography.headlineSmall)
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back to list"
                )
            }
        }

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title *") },
            isError = title.isBlank(),
            supportingText = if (title.isBlank()) {
                { Text("Title cannot be empty") }
            } else null
        )

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = tagsText,
            onValueChange = { tagsText = it },
            label = { Text("Tags (comma-separated)") },
            modifier = Modifier.fillMaxWidth()
        )

        Text("Pages: ${images.size}", style = MaterialTheme.typography.bodyMedium)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    val updatedBook = book.copy(
                        title = title,
                        description = description,
                        tags = tagsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    )
                    onUpdate(updatedBook)
                },
                enabled = title.isNotBlank() && enabled
            ) {
                Text("Save Changes")
            }

            Button(
                onClick = onDelete,
                enabled = enabled,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete Book")
            }
        }
    }
}

@Composable
private fun BookList(
    books: List<Book>,
    onSelect: (String) -> Unit,
    onDelete: (String) -> Unit,
    isLoading: Boolean
) {
    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (books.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("No picture books found", style = MaterialTheme.typography.bodyLarge)
            Text("Create your first picture book!", style = MaterialTheme.typography.bodyMedium)
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(books, key = { book -> book.id }) { book ->
                BookListItem(
                    book = book,
                    onSelect = { onSelect(book.id) },
                    onDelete = { onDelete(book.id) }
                )
            }
        }
    }
}

@Composable
private fun BookListItem(
    book: Book,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onSelect
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(book.title, style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete book"
                    )
                }
            }

            if (book.description.isNotEmpty()) {
                Text(book.description, style = MaterialTheme.typography.bodyMedium)
            }

            if (book.tags.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    book.tags.forEach { tag ->
                        SuggestionChip(
                            onClick = {
                                // Filter books by this specific tag
                                onEvent(PictureBookEvent.FilterByTags(setOf(tag)))
                            },
                            label = { Text(tag) }
                        )
                    }
                }
            }

            Text(
                "Created: ${book.createdAt.toString().substring(0, 10)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ViewModel Factory
class PictureBookViewModelFactory(
    private val application: Application
) : androidx.lifecycle.ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PictureBookViewModel::class.java)) {
            val context = application.applicationContext

            // Initialize Room database
            val database = Room.databaseBuilder(
                context,
                BookDatabase::class.java,
                "picture_books.db"
            ).build()

            val repository = BookRepositoryImpl(database.bookDao())
            val imageUtils = ImageUtils()

            return PictureBookViewModel(application, repository, imageUtils) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// Events
sealed interface PictureBookEvent {
    data class CreateBook(val title: String, val description: String, val tags: List<String>) : PictureBookEvent
    data class UpdateBook(val book: Book) : PictureBookEvent
    data class DeleteBook(val bookId: String) : PictureBookEvent
    data class LoadBook(val bookId: String) : PictureBookEvent
    object ClearCurrentBook : PictureBookEvent
    data class SearchBooks(val query: String) : PictureBookEvent
    data class FilterByTags(val tags: Set<String>) : PictureBookEvent
    data class ToggleTagFilter(val tag: String) : PictureBookEvent
    object ClearFilters : PictureBookEvent
    object ClearError : PictureBookEvent
    object ClearMessage : PictureBookEvent
}