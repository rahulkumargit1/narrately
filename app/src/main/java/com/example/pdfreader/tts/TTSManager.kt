package com.example.pdfreader.tts

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Manages Android TextToSpeech engine with chunk-based playback.
 *
 * Key fix: [_isPlaying] no longer flickers false between chunks.
 * We track an [_isAutoAdvancing] flag so the UI stays stable during
 * the brief gap between one utterance ending and the next starting.
 */
class TTSManager(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null

    // Engine readiness
    private val _initState = MutableStateFlow(InitState.LOADING)
    val initState: StateFlow<InitState> = _initState.asStateFlow()

    // Playback state — stable, does not flicker between chunks
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    // Current chunk index
    private val _currentSentenceIndex = MutableStateFlow(0)
    val currentSentenceIndex: StateFlow<Int> = _currentSentenceIndex.asStateFlow()

    private var textChunks: List<String> = emptyList()
    private var isAutoAdvancing = false

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            _initState.value = if (result == TextToSpeech.LANG_MISSING_DATA ||
                result == TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                // Fallback to default locale
                tts?.setLanguage(Locale.getDefault())
                InitState.READY
            } else {
                InitState.READY
            }

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    isAutoAdvancing = false
                    _isPlaying.value = true
                    utteranceId?.toIntOrNull()?.let {
                        _currentSentenceIndex.value = it
                    }
                }

                override fun onDone(utteranceId: String?) {
                    val index = utteranceId?.toIntOrNull() ?: return
                    if (index < textChunks.size - 1) {
                        // Auto-advance: keep isPlaying = true to prevent UI flicker
                        isAutoAdvancing = true
                        playChunk(index + 1)
                    } else {
                        // Reached end of document
                        _isPlaying.value = false
                        isAutoAdvancing = false
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isPlaying.value = false
                    isAutoAdvancing = false
                }
            })
        } else {
            _initState.value = InitState.ERROR
        }
    }

    fun loadText(chunks: List<String>, startIndex: Int = 0) {
        textChunks = chunks
        _currentSentenceIndex.value = startIndex.coerceIn(0, (chunks.size - 1).coerceAtLeast(0))
    }

    fun play() {
        if (_initState.value != InitState.READY || textChunks.isEmpty()) return
        playChunk(_currentSentenceIndex.value)
    }

    fun pause() {
        tts?.stop()
        _isPlaying.value = false
        isAutoAdvancing = false
    }

    private fun playChunk(index: Int) {
        if (index !in textChunks.indices) return
        val text = textChunks[index]
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, index.toString())
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, index.toString())
    }

    fun setSpeed(speed: Float) {
        tts?.setSpeechRate(speed.coerceIn(0.25f, 4.0f))
    }

    fun setPitch(pitch: Float) {
        tts?.setPitch(pitch.coerceIn(0.25f, 4.0f))
    }

    fun seekTo(index: Int) {
        if (index in textChunks.indices) {
            _currentSentenceIndex.value = index
            if (_isPlaying.value || isAutoAdvancing) {
                playChunk(index)
            }
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    enum class InitState { LOADING, READY, ERROR }
}
