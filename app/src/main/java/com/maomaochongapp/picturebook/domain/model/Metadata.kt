package com.maomaochongapp.picturebook.domain.model

/**
 * Metadata container for books and images
 */
data class Metadata(
    val title: String = "",
    val description: String = "",
    val tags: List<String> = emptyList(),
    val customFields: Map<String, String> = emptyMap(),
)