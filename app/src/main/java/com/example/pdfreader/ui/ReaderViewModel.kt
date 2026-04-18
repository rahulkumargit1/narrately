package com.example.pdfreader.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pdfreader.data.DocumentEntity
import com.example.pdfreader.data.ProgressEntity
import com.example.pdfreader.data.ReaderDao
import com.example.pdfreader.parser.DocumentParser
import com.example.pdfreader.parser.ParseResult
import com.example.pdfreader.tts.TTSManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReaderViewModel @Inject constructor(
    application: Application,
    private val readerDao: ReaderDao,
) : AndroidViewModel(application) {

    private val documentParser = DocumentParser(application.applicationContext)
    val ttsManager = TTSManager(application.applicationContext)

    // ─── Library ───
    val libraryDocuments: StateFlow<List<DocumentEntity>> = readerDao.getAllDocuments()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _progressMap = MutableStateFlow<Map<Int, ProgressEntity>>(emptyMap())
    val progressMap: StateFlow<Map<Int, ProgressEntity>> = _progressMap.asStateFlow()

    // ─── Active document ───
    private val _currentDocument = MutableStateFlow<DocumentEntity?>(null)
    val currentDocument: StateFlow<DocumentEntity?> = _currentDocument.asStateFlow()

    private val _textChunks = MutableStateFlow<List<String>>(emptyList())
    val textChunks: StateFlow<List<String>> = _textChunks.asStateFlow()

    // ─── Playback ───
    val isPlaying: StateFlow<Boolean> = ttsManager.isPlaying
    val currentChunkIndex: StateFlow<Int> = ttsManager.currentSentenceIndex

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _pitch = MutableStateFlow(1.0f)
    val pitch: StateFlow<Float> = _pitch.asStateFlow()

    // ─── UI state ───
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // ─── Reading stats ───
    val totalWordCount: StateFlow<Int> = _textChunks.map { chunks ->
        chunks.sumOf { it.split("\\s+".toRegex()).count { w -> w.isNotBlank() } }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val estimatedMinutes: StateFlow<Int> = totalWordCount.map { words ->
        // Average reading speed ~200 WPM for TTS at 1x
        (words / 200.0).toInt().coerceAtLeast(1)
    }.stateIn(viewModelScope, SharingStarted.Lazily, 1)

    val progressPercent: StateFlow<Int> = combine(currentChunkIndex, _textChunks) { idx, chunks ->
        if (chunks.isEmpty()) 0
        else ((idx.toFloat() / chunks.size) * 100).toInt().coerceIn(0, 100)
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0)

    init {
        viewModelScope.launch {
            libraryDocuments.collect { docs ->
                val map = mutableMapOf<Int, ProgressEntity>()
                for (doc in docs) {
                    readerDao.getProgressForDocument(doc.id)?.let { map[doc.id] = it }
                }
                _progressMap.value = map
            }
        }
        // Auto-save on chunk change
        viewModelScope.launch {
            currentChunkIndex.collect { saveCurrentProgress() }
        }
    }

    fun importDocument(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                getApplication<Application>().contentResolver.takePersistableUriPermission(
                    uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) { }

            when (val result = documentParser.parseDocument(uri)) {
                is ParseResult.Success -> {
                    val mimeType = getApplication<Application>().contentResolver.getType(uri) ?: "unknown"
                    val newDoc = DocumentEntity(title = result.title, fileUri = uri.toString(), mimeType = mimeType)
                    val id = readerDao.insertDocument(newDoc).toInt()
                    readerDao.saveProgress(ProgressEntity(documentId = id, currentChunkIndex = 0, totalChunks = result.chunks.size))
                }
                is ParseResult.Error -> {
                    _errorMessage.value = result.exception.message ?: "Failed to parse document"
                }
            }
            _isLoading.value = false
        }
    }

    fun openDocument(document: DocumentEntity) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _currentDocument.value = document

            val uri = Uri.parse(document.fileUri)
            when (val result = documentParser.parseDocument(uri)) {
                is ParseResult.Success -> {
                    _textChunks.value = result.chunks
                    val progress = readerDao.getProgressForDocument(document.id)
                    val startIndex = progress?.currentChunkIndex ?: 0
                    ttsManager.loadText(result.chunks, startIndex)
                }
                is ParseResult.Error -> {
                    _errorMessage.value = result.exception.message ?: "Failed to open document"
                    _textChunks.value = emptyList()
                }
            }
            _isLoading.value = false
        }
    }

    // ─── Playback controls ───
    fun playPause() {
        if (isPlaying.value) ttsManager.pause() else ttsManager.play()
    }

    fun seekForward() {
        val next = (currentChunkIndex.value + 1).coerceAtMost(textChunks.value.size - 1)
        ttsManager.seekTo(next)
    }

    fun seekBackward() {
        val prev = (currentChunkIndex.value - 1).coerceAtLeast(0)
        ttsManager.seekTo(prev)
    }

    fun seekToChunk(index: Int) { ttsManager.seekTo(index) }

    fun setSpeed(speed: Float) {
        _playbackSpeed.value = speed
        ttsManager.setSpeed(speed)  // Instant — re-speaks if playing
    }

    fun setPitch(p: Float) {
        _pitch.value = p
        ttsManager.setPitch(p)  // Instant — re-speaks if playing
    }

    fun deleteDocument(document: DocumentEntity) {
        viewModelScope.launch {
            readerDao.deleteDocument(document.id)
            readerDao.deleteProgress(document.id)
        }
    }

    fun clearError() { _errorMessage.value = null }
    fun stopPlayback() { ttsManager.pause() }

    fun saveCurrentProgress() {
        viewModelScope.launch {
            val doc = _currentDocument.value ?: return@launch
            val chunks = _textChunks.value
            if (chunks.isNotEmpty()) {
                readerDao.saveProgress(ProgressEntity(
                    documentId = doc.id,
                    currentChunkIndex = currentChunkIndex.value,
                    totalChunks = chunks.size,
                ))
            }
        }
    }

    override fun onCleared() {
        saveCurrentProgress()
        ttsManager.shutdown()
        super.onCleared()
    }
}
