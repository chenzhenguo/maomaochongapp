package com.maomaochongapp.picturebook.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Room entity for Book table
 */
@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "description")
    val description: String = "",

    @ColumnInfo(name = "tags")
    val tags: String = "", // Comma-separated tags

    @ColumnInfo(name = "cover_image_uri")
    val coverImageUri: String? = null,

    @ColumnInfo(name = "page_count")
    val pageCount: Int = 0,

    @ColumnInfo(name = "created_at")
    val createdAt: Instant = Instant.now(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant = Instant.now(),

    @ColumnInfo(name = "source_folder_uri")
    val sourceFolderUri: String? = null,

    @ColumnInfo(name = "export_path")
    val exportPath: String? = null,
)