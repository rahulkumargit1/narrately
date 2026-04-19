package com.example.pdfreader.tts

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * TTS engine with natural voice selection, instant speed/pitch,
 * and flicker-free chunk advancement.
 *
 * Voice selection priority:
 * 1. Network-quality voices (most human-like, Google WaveNet/Neural)
 * 2. High-quality local voices
 * 3. Default fallback
 *
 * Speed/pitch changes re-speak the current chunk immediately.
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
            val engine = tts ?: return

            // Set language
            val langResult = engine.setLanguage(Locale.US)
            if (langResult == TextToSpeech.LANG_MISSING_DATA ||
                langResult == TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                engine.setLanguage(Locale.getDefault())
            }

            // ─── Select the most natural voice available ───
            selectBestVoice(engine)

            // Slightly slower default for podcast-like feel
            engine.setSpeechRate(0.95f)
            currentSpeed = 1.0f  // User-facing 1.0x maps to 0.95 actual

            _initState.value = InitState.READY

            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
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

    /**
     * Select the most human-sounding voice.
     * Priority: network voices > high quality local > default
     */
    private fun selectBestVoice(engine: TextToSpeech) {
        try {
            val voices = engine.voices ?: return
            val englishVoices = voices.filter {
                it.locale.language == "en" && !it.isNetworkConnectionRequired
            }

            // Try to find a female voice (generally sounds more natural for narration)
            val bestVoice = englishVoices
                .sortedByDescending { voice ->
                    var score = 0
                    // Prefer voices with "quality" features
                    if (voice.features.contains("networkTts")) score += 100
                    // Prefer US English
                    if (voice.locale == Locale.US) score += 50
                    // Prefer voices that indicate high quality
                    val name = voice.name.lowercase()
                    if (name.contains("neural") || name.contains("wavenet")) score += 200
                    if (name.contains("enhanced") || name.contains("premium")) score += 150
                    if (name.contains("natural")) score += 120
                    // Prefer female voices for narration
                    if (name.contains("female") || name.contains("-f-") ||
                        name.contains("voice 2") || name.contains("voice 4")) score += 30
                    // Penalize very robotic sounding ones
                    if (voice.quality < Voice.QUALITY_NORMAL) score -= 50
                    score
                }
                .firstOrNull()

            if (bestVoice != null) {
                engine.voice = bestVoice
            }

            // Also try network voices if available (sounds much better)
            val networkVoice = voices.filter {
                it.locale.language == "en" &&
                    it.isNetworkConnectionRequired &&
                    !it.features.contains("notInstalled")
            }.maxByOrNull { it.quality }

            if (networkVoice != null) {
                engine.voice = networkVoice
            }
        } catch (_: Exception) {
            // Fall back to default — still fine
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
            // Request streaming for lower latency
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, android.media.AudioManager.STREAM_MUSIC)
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, index.toString())
    }

    /** INSTANT speed change — re-speaks current chunk immediately */
    fun setSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.25f, 4.0f)
        currentSpeed = clamped
        // Map user speed to slightly lower actual rate for natural feel
        // 1.0x user = 0.95 actual (podcast pace)
        tts?.setSpeechRate(clamped * 0.95f)
        if (_isPlaying.value) {
            playChunk(_currentSentenceIndex.value)
        }
    }

    /** INSTANT pitch change */
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
