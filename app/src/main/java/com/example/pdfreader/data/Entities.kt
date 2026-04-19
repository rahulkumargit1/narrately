package com.example.pdfreader.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val fileUri: String,
    val mimeType: String,
    val thumbnailPath: String? = null,
    val addedTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "reading_progress")
data class ProgressEntity(
    @PrimaryKey val documentId: Int,
    val currentChunkIndex: Int,
    val totalChunks: Int,
    val lastReadTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val documentId: Int,
    val chunkIndex: Int,
    val label: String = "",
    val createdTimestamp: Long = System.currentTimeMillis()
)

/** Cached parsed text — avoids re-parsing PDFs on every open */
@Entity(tableName = "cached_chunks")
data class CachedChunksEntity(
    @PrimaryKey val documentId: Int,
    val chunksJson: String,   // JSON array of chunk strings
    val parsedTimestamp: Long = System.currentTimeMillis()
)
