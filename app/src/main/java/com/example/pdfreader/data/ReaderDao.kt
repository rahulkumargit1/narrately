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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: DocumentEntity): Long

    @Query("UPDATE documents SET thumbnailPath = :path WHERE id = :id")
    suspend fun updateThumbnail(id: Int, path: String)

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

    // ─── Cached Chunks ───
    @Query("SELECT * FROM cached_chunks WHERE documentId = :documentId")
    suspend fun getCachedChunks(documentId: Int): CachedChunksEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCachedChunks(cached: CachedChunksEntity)

    @Query("DELETE FROM cached_chunks WHERE documentId = :documentId")
    suspend fun deleteCachedChunks(documentId: Int)

    // ─── Listening Stats ───
    @Query("SELECT * FROM listening_stats WHERE date = :date")
    suspend fun getStatsForDate(date: String): ListeningStatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveStats(stats: ListeningStatsEntity)

    @Query("SELECT SUM(totalSecondsListened) FROM listening_stats")
    suspend fun getTotalListeningSeconds(): Long?

    @Query("SELECT SUM(chunksCompleted) FROM listening_stats")
    suspend fun getTotalChunksCompleted(): Long?

    @Query("SELECT * FROM listening_stats ORDER BY date DESC LIMIT 7")
    suspend fun getRecentStats(): List<ListeningStatsEntity>
}
