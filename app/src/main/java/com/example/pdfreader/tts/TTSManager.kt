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
 * TTS engine with flicker-free playback and instant speed/pitch.
 *
 * Speed/pitch changes take effect IMMEDIATELY — if currently speaking,
 * we stop and re-speak the current chunk with the new rate/pitch so
 * the user hears the change in real time, not on the next chunk.
 */
class TTSManager(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null

    private val _initState = MutableStateFlow(InitState.LOADING)
    val initState: StateFlow<InitState> = _initState.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentSentenceIndex = MutableStateFlow(0)
    val currentSentenceIndex: StateFlow<Int> = _currentSentenceIndex.asStateFlow()

    private var textChunks: List<String> = emptyList()
    private var isAutoAdvancing = false
    private var currentSpeed = 1.0f
    private var currentPitch = 1.0f

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA ||
                result == TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                tts?.setLanguage(Locale.getDefault())
            }
            _initState.value = InitState.READY

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
                        isAutoAdvancing = true
                        playChunk(index + 1)
                    } else {
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

    /**
     * INSTANT speed change — applies immediately to current speech.
     * If currently speaking, stops and re-speaks at new rate.
     */
    fun setSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.25f, 4.0f)
        currentSpeed = clamped
        tts?.setSpeechRate(clamped)
        // Instant: re-speak current chunk if playing
        if (_isPlaying.value) {
            playChunk(_currentSentenceIndex.value)
        }
    }

    /**
     * INSTANT pitch change — applies immediately.
     */
    fun setPitch(pitch: Float) {
        val clamped = pitch.coerceIn(0.25f, 4.0f)
        currentPitch = clamped
        tts?.setPitch(clamped)
        if (_isPlaying.value) {
            playChunk(_currentSentenceIndex.value)
        }
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
