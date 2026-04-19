package com.example.pdfreader.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [DocumentEntity::class, ProgressEntity::class, BookmarkEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class ReaderDatabase : RoomDatabase() {
    abstract fun readerDao(): ReaderDao
}
