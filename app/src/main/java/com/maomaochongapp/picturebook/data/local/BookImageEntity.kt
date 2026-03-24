package com.maomaochongapp.picturebook.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Room entity for BookImage table
 */
@Entity(tableName = "book_images")
data class BookImageEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "book_id")
    val bookId: String,

    @ColumnInfo(name = "original_file_name")
    val originalFileName: String,

    @ColumnInfo(name = "display_name")
    val displayName: String = "",

    @ColumnInfo(name = "uri")
    val uri: String,

    @ColumnInfo(name = "mime_type")
    val mimeType: String,

    @ColumnInfo(name = "file_size")
    val fileSize: Long = 0L,

    @ColumnInfo(name = "width")
    val width: Int = 0,

    @ColumnInfo(name = "height")
    val height: Int = 0,

    @ColumnInfo(name = "page_number")
    val pageNumber: Int = 0,

    @ColumnInfo(name = "created_at")
    val createdAt: Instant = Instant.now(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant = Instant.now(),
)