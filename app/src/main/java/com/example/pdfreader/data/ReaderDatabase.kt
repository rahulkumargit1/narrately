package com.example.pdfreader.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        DocumentEntity::class,
        ProgressEntity::class,
        BookmarkEntity::class,
        CachedChunksEntity::class,
        ListeningSessionEntity::class,
    ],
    version = 5,
    exportSchema = false,
)
abstract class ReaderDatabase : RoomDatabase() {
    abstract fun readerDao(): ReaderDao

    companion object {
        /** v3 → v4: No schema changes (only code-level additions) */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) { /* no-op */ }
        }

        /** v4 → v5: Add listening_sessions table */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `listening_sessions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `documentId` INTEGER NOT NULL,
                        `documentTitle` TEXT NOT NULL,
                        `startTimestamp` INTEGER NOT NULL,
                        `durationSeconds` INTEGER NOT NULL,
                        `chunksListened` INTEGER NOT NULL,
                        `dateKey` TEXT NOT NULL
                    )
                """.trimIndent())
            }
        }
    }
}
