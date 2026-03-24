package com.maomaochongapp.picturebook.domain.model

import java.time.Instant

/**
 * Picture book entity with metadata
 */
data class Book(
    val id: String,
    val title: String,
    val description: String = "",
    val tags: List<String> = emptyList(),
    val coverImageUri: String? = null,
    val pageCount: Int = 0,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val sourceFolderUri: String? = null,
    val exportPath: String? = null,
)