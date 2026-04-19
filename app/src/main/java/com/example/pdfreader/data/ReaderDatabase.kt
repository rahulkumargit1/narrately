package com.example.pdfreader.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        DocumentEntity::class,
        ProgressEntity::class,
        BookmarkEntity::class,
        CachedChunksEntity::class,
        ListeningStatsEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class ReaderDatabase : RoomDatabase() {
    abstract fun readerDao(): ReaderDao
}
