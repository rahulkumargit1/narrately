package com.example.pdfreader.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [DocumentEntity::class, ProgressEntity::class], version = 1, exportSchema = false)
abstract class ReaderDatabase : RoomDatabase() {
    abstract fun readerDao(): ReaderDao
}
