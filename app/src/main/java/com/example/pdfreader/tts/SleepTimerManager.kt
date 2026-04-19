package com.example.pdfreader.tts

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Countdown timer that auto-pauses playback.
 * Supports preset durations and "end of current chapter" mode.
 */
class SleepTimerManager {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var timerJob: Job? = null

    private val _remainingSeconds = MutableStateFlow(0)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    /** Start a sleep timer for [minutes]. Calls [onExpired] when done. */
    fun start(minutes: Int, onExpired: () -> Unit) {
        cancel()
        _remainingSeconds.value = minutes * 60
        _isActive.value = true

        timerJob = scope.launch {
            while (_remainingSeconds.value > 0) {
                delay(1000)
                _remainingSeconds.value -= 1
            }
            _isActive.value = false
            onExpired()
        }
    }

    fun cancel() {
        timerJob?.cancel()
        timerJob = null
        _remainingSeconds.value = 0
        _isActive.value = false
    }

    fun formatRemaining(): String {
        val s = _remainingSeconds.value
        val m = s / 60
        val sec = s % 60
        return if (m > 0) "${m}m ${sec}s" else "${sec}s"
    }

    fun destroy() {
        cancel()
        scope.cancel()
    }
}
