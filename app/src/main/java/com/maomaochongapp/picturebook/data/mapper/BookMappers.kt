package com.maomaochongapp.picturebook.data.mapper

import com.maomaochongapp.picturebook.data.local.BookEntity
import com.maomaochongapp.picturebook.data.local.BookImageEntity
import com.maomaochongapp.picturebook.domain.model.Book
import com.maomaochongapp.picturebook.domain.model.BookImage

/**
 * Mapper functions to convert between domain models and Room entities.
 * Defined as top-level extension functions for easy static import.
 */
fun BookEntity.toDomain(): Book {
    return Book(
        id = id,
        title = title,
        description = description,
        tags = if (tags.isNotBlank()) tags.split(",").map { it.trim() } else emptyList(),
        coverImageUri = coverImageUri,
        pageCount = pageCount,
        createdAt = createdAt,
        updatedAt = updatedAt,
        sourceFolderUri = sourceFolderUri,
        exportPath = exportPath,
    )
}

fun Book.toEntity(): BookEntity {
    return BookEntity(
        id = id,
        title = title,
        description = description,
        tags = tags.joinToString(","),
        coverImageUri = coverImageUri,
        pageCount = pageCount,
        createdAt = createdAt,
        updatedAt = updatedAt,
        sourceFolderUri = sourceFolderUri,
        exportPath = exportPath,
    )
}

fun BookImageEntity.toDomain(): BookImage {
    return BookImage(
        id = id,
        bookId = bookId,
        originalFileName = originalFileName,
        displayName = displayName,
        uri = uri,
        mimeType = mimeType,
        fileSize = fileSize,
        width = width,
        height = height,
        pageNumber = pageNumber,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

fun BookImage.toEntity(): BookImageEntity {
    return BookImageEntity(
        id = id,
        bookId = bookId,
        originalFileName = originalFileName,
        displayName = displayName,
        uri = uri,
        mimeType = mimeType,
        fileSize = fileSize,
        width = width,
        height = height,
        pageNumber = pageNumber,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
