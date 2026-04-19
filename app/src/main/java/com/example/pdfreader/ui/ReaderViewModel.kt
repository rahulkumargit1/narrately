package com.example.pdfreader.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pdfreader.data.*
import com.example.pdfreader.parser.DocumentParser
import com.example.pdfreader.parser.ParseResult
import com.example.pdfreader.tts.TTSManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.*
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
        (words / 200.0).toInt().coerceAtLeast(1)
    }.stateIn(viewModelScope, SharingStarted.Lazily, 1)

    val progressPercent: StateFlow<Int> = combine(currentChunkIndex, _textChunks) { idx, chunks ->
        if (chunks.isEmpty()) 0
        else ((idx.toFloat() / chunks.size) * 100).toInt().coerceIn(0, 100)
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0)

    // ─── Bookmarks ───
    private val _bookmarks = MutableStateFlow<List<BookmarkEntity>>(emptyList())
    val bookmarks: StateFlow<List<BookmarkEntity>> = _bookmarks.asStateFlow()
    private var bookmarkJob: Job? = null

    // ─── Sleep Timer ───
    private val _sleepTimerRemaining = MutableStateFlow<Long>(0L) // seconds remaining
    val sleepTimerRemaining: StateFlow<Long> = _sleepTimerRemaining.asStateFlow()
    val isSleepTimerActive: StateFlow<Boolean> = _sleepTimerRemaining.map { it > 0 }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
    private var sleepTimerJob: Job? = null

    // ─── Search ───
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val searchResults: StateFlow<List<Int>> = combine(_searchQuery, _textChunks) { query, chunks ->
        if (query.length < 2) emptyList()
        else chunks.mapIndexedNotNull { index, chunk ->
            if (chunk.contains(query, ignoreCase = true)) index else null
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // ─── Font Size ───
    private val _fontSize = MutableStateFlow(15f) // sp
    val fontSize: StateFlow<Float> = _fontSize.asStateFlow()

    // ─── Listening Stats ───
    private val _totalListeningHours = MutableStateFlow(0f)
    val totalListeningHours: StateFlow<Float> = _totalListeningHours.asStateFlow()

    private val _totalChunksCompleted = MutableStateFlow(0L)
    val totalChunksCompleted: StateFlow<Long> = _totalChunksCompleted.asStateFlow()

    // ─── Progress cache ───
    private val progressCache = mutableMapOf<Int, Int>()
    private var listeningStartTime: Long = 0L

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
        viewModelScope.launch {
            currentChunkIndex.collect { idx ->
                val doc = _currentDocument.value ?: return@collect
                progressCache[doc.id] = idx
                persistProgress(doc.id, idx)
            }
        }
        // Track chunks completed
        viewModelScope.launch {
            currentChunkIndex.collect {
                incrementChunksCompleted()
            }
        }
        // Track listening time
        viewModelScope.launch {
            isPlaying.collect { playing ->
                if (playing) {
                    listeningStartTime = System.currentTimeMillis()
                } else if (listeningStartTime > 0) {
                    val elapsed = (System.currentTimeMillis() - listeningStartTime) / 1000
                    if (elapsed > 0) addListeningTime(elapsed)
                    listeningStartTime = 0L
                }
            }
        }
        // Load total stats
        viewModelScope.launch {
            val totalSec = readerDao.getTotalListeningSeconds() ?: 0L
            _totalListeningHours.value = totalSec / 3600f
            _totalChunksCompleted.value = readerDao.getTotalChunksCompleted() ?: 0L
        }
    }

    private fun persistProgress(docId: Int, chunkIndex: Int) {
        val chunks = _textChunks.value
        if (chunks.isEmpty()) return
        viewModelScope.launch {
            readerDao.saveProgress(ProgressEntity(documentId = docId, currentChunkIndex = chunkIndex, totalChunks = chunks.size))
        }
    }

    // ─── Listening Stats Tracking ───
    private fun todayStr(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private fun addListeningTime(seconds: Long) {
        viewModelScope.launch {
            val date = todayStr()
            val existing = readerDao.getStatsForDate(date)
            if (existing != null) {
                readerDao.saveStats(existing.copy(totalSecondsListened = existing.totalSecondsListened + seconds))
            } else {
                readerDao.saveStats(ListeningStatsEntity(date = date, totalSecondsListened = seconds))
            }
            _totalListeningHours.value = (readerDao.getTotalListeningSeconds() ?: 0L) / 3600f
        }
    }

    private fun incrementChunksCompleted() {
        viewModelScope.launch {
            val date = todayStr()
            val existing = readerDao.getStatsForDate(date)
            if (existing != null) {
                readerDao.saveStats(existing.copy(chunksCompleted = existing.chunksCompleted + 1))
            } else {
                readerDao.saveStats(ListeningStatsEntity(date = date, chunksCompleted = 1))
            }
            _totalChunksCompleted.value = readerDao.getTotalChunksCompleted() ?: 0L
        }
    }

    // ─── Import ───
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
                    progressCache[id] = 0
                    cacheChunks(id, result.chunks)

                    if (mimeType == "application/pdf" || result.title.endsWith(".pdf", ignoreCase = true)) {
                        viewModelScope.launch {
                            val thumbPath = documentParser.extractPdfThumbnail(uri, id)
                            if (thumbPath != null) readerDao.updateThumbnail(id, thumbPath)
                        }
                    }

                    // Track stats
                    viewModelScope.launch {
                        val date = todayStr()
                        val existing = readerDao.getStatsForDate(date)
                        if (existing != null) {
                            readerDao.saveStats(existing.copy(documentsOpened = existing.documentsOpened + 1))
                        } else {
                            readerDao.saveStats(ListeningStatsEntity(date = date, documentsOpened = 1))
                        }
                    }
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

            val cached = readerDao.getCachedChunks(document.id)
            val chunks: List<String>

            if (cached != null) {
                chunks = deserializeChunks(cached.chunksJson)
            } else {
                val uri = Uri.parse(document.fileUri)
                when (val result = documentParser.parseDocument(uri)) {
                    is ParseResult.Success -> {
                        chunks = result.chunks
                        cacheChunks(document.id, chunks)
                    }
                    is ParseResult.Error -> {
                        _errorMessage.value = result.exception.message ?: "Failed to open"
                        _textChunks.value = emptyList()
                        _isLoading.value = false
                        return@launch
                    }
                }
            }

            _textChunks.value = chunks
            val startIndex = progressCache[document.id]
                ?: readerDao.getProgressForDocument(document.id)?.currentChunkIndex
                ?: 0
            ttsManager.loadText(chunks, startIndex)
            progressCache[document.id] = startIndex

            bookmarkJob?.cancel()
            bookmarkJob = viewModelScope.launch {
                readerDao.getBookmarksForDocument(document.id).collect { _bookmarks.value = it }
            }
            _isLoading.value = false
        }
    }

    private suspend fun cacheChunks(docId: Int, chunks: List<String>) {
        val jsonArray = JSONArray()
        for (chunk in chunks) jsonArray.put(chunk)
        readerDao.saveCachedChunks(CachedChunksEntity(documentId = docId, chunksJson = jsonArray.toString()))
    }

    private fun deserializeChunks(json: String): List<String> {
        val arr = JSONArray(json)
        return (0 until arr.length()).map { arr.getString(it) }
    }

    // ─── Playback ───
    fun playPause() { if (isPlaying.value) ttsManager.pause() else ttsManager.play() }
    fun seekForward() { ttsManager.seekTo((currentChunkIndex.value + 1).coerceAtMost(textChunks.value.size - 1)) }
    fun seekBackward() { ttsManager.seekTo((currentChunkIndex.value - 1).coerceAtLeast(0)) }
    fun seekToChunk(index: Int) { ttsManager.seekTo(index) }

    fun setSpeed(speed: Float) { _playbackSpeed.value = speed; ttsManager.setSpeed(speed) }
    fun setPitch(p: Float) { _pitch.value = p; ttsManager.setPitch(p) }

    // ─── Sleep Timer ───
    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _sleepTimerRemaining.value = 0L
            return
        }
        _sleepTimerRemaining.value = minutes * 60L
        sleepTimerJob = viewModelScope.launch {
            while (_sleepTimerRemaining.value > 0) {
                delay(1000)
                _sleepTimerRemaining.value -= 1
            }
            // Timer expired — stop playback
            ttsManager.pause()
            _sleepTimerRemaining.value = 0L
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        _sleepTimerRemaining.value = 0L
    }

    // ─── Search ───
    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun clearSearch() { _searchQuery.value = "" }

    // ─── Font Size ───
    fun setFontSize(size: Float) { _fontSize.value = size.coerceIn(12f, 24f) }

    // ─── Bookmarks ───
    fun addBookmark(label: String = "") {
        val doc = _currentDocument.value ?: return
        val idx = currentChunkIndex.value
        viewModelScope.launch {
            readerDao.insertBookmark(BookmarkEntity(documentId = doc.id, chunkIndex = idx, label = label.ifBlank { "Chunk ${idx + 1}" }))
        }
    }

    fun deleteBookmark(bookmark: BookmarkEntity) { viewModelScope.launch { readerDao.deleteBookmark(bookmark.id) } }
    fun jumpToBookmark(bookmark: BookmarkEntity) { ttsManager.seekTo(bookmark.chunkIndex) }

    // ─── Document management ───
    fun deleteDocument(document: DocumentEntity) {
        viewModelScope.launch {
            readerDao.deleteDocument(document.id)
            readerDao.deleteProgress(document.id)
            readerDao.deleteBookmarksForDocument(document.id)
            readerDao.deleteCachedChunks(document.id)
            progressCache.remove(document.id)
            document.thumbnailPath?.let { java.io.File(it).delete() }
        }
    }

    fun clearError() { _errorMessage.value = null }
    fun stopPlayback() { ttsManager.pause() }

    fun saveCurrentProgress() {
        val doc = _currentDocument.value ?: return
        val idx = currentChunkIndex.value
        progressCache[doc.id] = idx
        persistProgress(doc.id, idx)
    }

    override fun onCleared() {
        saveCurrentProgress()
        // Flush listening time
        if (listeningStartTime > 0) {
            val elapsed = (System.currentTimeMillis() - listeningStartTime) / 1000
            if (elapsed > 0) addListeningTime(elapsed)
        }
        ttsManager.shutdown()
        super.onCleared()
    }
}
