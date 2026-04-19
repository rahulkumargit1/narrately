package com.example.pdfreader.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        DocumentEntity::class,
        ProgressEntity::class,
        BookmarkEntity::class,
        CachedChunksEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class ReaderDatabase : RoomDatabase() {
    abstract fun readerDao(): ReaderDao
}
