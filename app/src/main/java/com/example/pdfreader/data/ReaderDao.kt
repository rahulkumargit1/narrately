package com.example.pdfreader.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReaderDao {
    // ─── Documents ───
    @Query("SELECT * FROM documents ORDER BY addedTimestamp DESC")
    fun getAllDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun getDocumentById(id: Int): DocumentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: DocumentEntity): Long

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun deleteDocument(id: Int)

    // ─── Progress ───
    @Query("SELECT * FROM reading_progress WHERE documentId = :documentId")
    suspend fun getProgressForDocument(documentId: Int): ProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(progress: ProgressEntity)

    @Query("DELETE FROM reading_progress WHERE documentId = :documentId")
    suspend fun deleteProgress(documentId: Int)

    // ─── Bookmarks ───
    @Query("SELECT * FROM bookmarks WHERE documentId = :documentId ORDER BY chunkIndex ASC")
    fun getBookmarksForDocument(documentId: Int): Flow<List<BookmarkEntity>>

    @Insert
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmark(id: Int)

    @Query("DELETE FROM bookmarks WHERE documentId = :documentId")
    suspend fun deleteBookmarksForDocument(documentId: Int)
}
