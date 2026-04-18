package com.example.pdfreader.parser

import android.content.Context
import android.net.Uri
import com.tomroush.pdfbox.pdmodel.PDDocument
import com.tomroush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
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
            
            // Extract a reasonable title from the URI
            var title = "Unknown Document"
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val displayNameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (displayNameIndex != -1) {
                        title = it.getString(displayNameIndex)
                    }
                }
            }

            val text = when {
                mimeType == "application/pdf" || title.endsWith(".pdf", ignoreCase = true) -> {
                    parsePdf(uri)
                }
                mimeType?.startsWith("text/") == true || title.endsWith(".txt", ignoreCase = true) -> {
                    parseTextFile(uri)
                }
                else -> {
                    throw IllegalArgumentException("Unsupported file type: $mimeType")
                }
            }

            val chunks = chunkTextForTTS(text)
            ParseResult.Success(title, chunks)
        } catch (e: Exception) {
            ParseResult.Error(e)
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
        val stringBuilder = java.lang.StringBuilder()
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                var line: String? = reader.readLine()
                while (line != null) {
                    stringBuilder.append(line).append("\n")
                    line = reader.readLine()
                }
            }
        }
        return stringBuilder.toString()
    }

    /**
     * TTS engines have limits (usually ~4000 chars per utterance).
     * This splits the text into reasonable chunks (sentences/paragraphs).
     */
    private fun chunkTextForTTS(text: String): List<String> {
        val maxChunkLength = 3000
        val chunks = mutableListOf<String>()
        
        // Simple chunking by regex (splitting by sentence endings like . ! ? followed by space)
        val sentences = text.split(Regex("(?<=[.!?])\\s+"))
        
        var currentChunk = StringBuilder()
        for (sentence in sentences) {
            if (currentChunk.length + sentence.length > maxChunkLength) {
                if (currentChunk.isNotEmpty()) {
                    chunks.add(currentChunk.toString().trim())
                    currentChunk = java.lang.StringBuilder()
                }
                // If a single sentence is incredibly long (rare), hard chop it
                if (sentence.length > maxChunkLength) {
                    val hardChunks = sentence.chunked(maxChunkLength)
                    chunks.addAll(hardChunks)
                } else {
                    currentChunk.append(sentence).append(" ")
                }
            } else {
                currentChunk.append(sentence).append(" ")
            }
        }
        if (currentChunk.isNotEmpty() && currentChunk.isNotBlank()) {
            chunks.add(currentChunk.toString().trim())
        }
        return chunks.filter { it.isNotBlank() }
    }
}
