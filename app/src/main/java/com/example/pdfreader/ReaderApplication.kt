package com.example.pdfreader

import android.app.Application
import com.tomroush.pdfbox.android.PDFBoxResourceLoader
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ReaderApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize PDFBox
        PDFBoxResourceLoader.init(applicationContext)
    }
}
