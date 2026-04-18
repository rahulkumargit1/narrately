package com.example.pdfreader.tts

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class TTSManager(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentSentenceIndex = MutableStateFlow(0)
    val currentSentenceIndex: StateFlow<Int> = _currentSentenceIndex.asStateFlow()

    private var textChunks: List<String> = emptyList()

    init {
        tts = TextToSpeech(context, this)
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isPlaying.value = true
                utteranceId?.toIntOrNull()?.let {
                    _currentSentenceIndex.value = it
                }
            }

            override fun onDone(utteranceId: String?) {
                _isPlaying.value = false
                val index = utteranceId?.toIntOrNull() ?: return
                if (index < textChunks.size - 1) {
                    playChunk(index + 1)
                }
            }

            @Deprecated("Deprecated in Java", ReplaceWith("onStart(utteranceId)"))
            override fun onError(utteranceId: String?) {
                _isPlaying.value = false
            }
        })
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            tts?.language = Locale.getDefault()
        }
    }

    fun loadText(chunks: List<String>, startIndex: Int = 0) {
        textChunks = chunks
        _currentSentenceIndex.value = startIndex
    }

    fun play() {
        if (!isInitialized || textChunks.isEmpty()) return
        playChunk(_currentSentenceIndex.value)
    }

    fun pause() {
        tts?.stop()
        _isPlaying.value = false
    }

    private fun playChunk(index: Int) {
        if (index >= textChunks.size || index < 0) return
        val text = textChunks[index]
        
        val params = Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, index.toString())
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, index.toString())
    }

    fun setSpeed(speed: Float) {
        tts?.setSpeechRate(speed)
    }

    fun setPitch(pitch: Float) {
        tts?.setPitch(pitch)
    }

    fun seekTo(index: Int) {
        if (index in textChunks.indices) {
            _currentSentenceIndex.value = index
            if (_isPlaying.value) {
                playChunk(index)
            }
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}
