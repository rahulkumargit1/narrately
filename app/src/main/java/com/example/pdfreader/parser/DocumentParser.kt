package com.example.pdfreader.parser

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.rendering.PDFRenderer
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader

sealed class ParseResult {
    data class Success(val title: String, val chunks: List<String>) : ParseResult()
    data class Error(val exception: Exception) : ParseResult()
}

class DocumentParser(private val context: Context) {

    suspend fun parseDocument(uri: Uri): ParseResult = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri)

            var title = "Unknown Document"
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val idx = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (idx != -1) title = it.getString(idx)
                }
            }

            val text = when {
                mimeType == "application/pdf" || title.endsWith(".pdf", ignoreCase = true) -> parsePdf(uri)
                mimeType?.startsWith("text/") == true || title.endsWith(".txt", ignoreCase = true) -> parseTextFile(uri)
                else -> throw IllegalArgumentException("Unsupported file type: $mimeType")
            }

            val chunks = chunkTextForTTS(text)
            ParseResult.Success(title, chunks)
        } catch (e: Exception) {
            ParseResult.Error(e)
        }
    }

    /**
     * Render the first page of a PDF as a thumbnail bitmap.
     * Returns the file path of the saved thumbnail, or null on failure.
     */
    suspend fun extractPdfThumbnail(uri: Uri, docId: Int): String? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                PDDocument.load(inputStream).use { document ->
                    if (document.numberOfPages > 0) {
                        val renderer = PDFRenderer(document)
                        // Render at 72 DPI — small but clear enough for a card
                        val bitmap = renderer.renderImageWithDPI(0, 72f)
                        val thumbDir = File(context.filesDir, "thumbnails")
                        thumbDir.mkdirs()
                        val thumbFile = File(thumbDir, "thumb_$docId.jpg")
                        FileOutputStream(thumbFile).use { out ->
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
                        }
                        bitmap.recycle()
                        return@withContext thumbFile.absolutePath
                    }
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun parsePdf(uri: Uri): String {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            PDDocument.load(inputStream).use { document ->
                val stripper = PDFTextStripper()
                return stripper.getText(document)
            }
        }
        return ""
    }

    private fun parseTextFile(uri: Uri): String {
        val sb = StringBuilder()
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                var line = reader.readLine()
                while (line != null) {
                    sb.append(line).append("\n")
                    line = reader.readLine()
                }
            }
        }
        return sb.toString()
    }

    /**
     * Sentence-level chunking for fine-grained tap-to-seek.
     *
     * Each chunk is 1-3 sentences (~200-600 chars) so the user can tap
     * on a specific passage and playback starts from that exact point.
     * This also makes the karaoke-style highlighting more precise.
     */
    private fun chunkTextForTTS(text: String): List<String> {
        // Split into individual sentences
        val sentences = text
            .replace("\r\n", "\n")
            .split(Regex("(?<=[.!?])\\s+|\\n{2,}"))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (sentences.isEmpty()) return emptyList()

        // Group 2-3 sentences per chunk for natural reading flow
        // Target: ~400 chars per chunk (sweet spot for TTS + readability)
        val chunks = mutableListOf<String>()
        var current = StringBuilder()
        var sentenceCount = 0

        for (sentence in sentences) {
            if (current.length + sentence.length > 500 || sentenceCount >= 3) {
                if (current.isNotEmpty()) {
                    chunks.add(current.toString().trim())
                    current = StringBuilder()
                    sentenceCount = 0
                }
            }
            if (current.isNotEmpty()) current.append(" ")
            current.append(sentence)
            sentenceCount++
        }
        if (current.isNotBlank()) {
            chunks.add(current.toString().trim())
        }

        return chunks.filter { it.isNotBlank() }
    }
}
