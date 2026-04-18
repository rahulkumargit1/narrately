package com.example.pdfreader.di

import android.content.Context
import androidx.room.Room
import com.example.pdfreader.data.ReaderDao
import com.example.pdfreader.data.ReaderDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideReaderDatabase(@ApplicationContext context: Context): ReaderDatabase {
        return Room.databaseBuilder(
            context,
            ReaderDatabase::class.java,
            "reader_database"
        ).build()
    }

    @Provides
    fun provideReaderDao(database: ReaderDatabase): ReaderDao {
        return database.readerDao()
    }
}
