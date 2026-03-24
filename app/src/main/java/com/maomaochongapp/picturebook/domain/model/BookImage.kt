package com.maomaochongapp.picturebook.domain.model

import java.time.Instant

/**
 * Individual image/page in a picture book
 */
data class BookImage(
    val id: String,
    val bookId: String,
    val originalFileName: String,
    val displayName: String = "",
    val uri: String,
    val mimeType: String,
    val fileSize: Long = 0L,
    val width: Int = 0,
    val height: Int = 0,
    val pageNumber: Int = 0,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
)