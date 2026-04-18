package com.example.pdfreader.service

import android.content.Intent
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.pdfreader.tts.TTSManager

class MediaLibraryService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private lateinit var player: Player
    
    // In a real implementation this would be injected via Hilt or bound
    // We instantiate it here as a placeholder wrapper for the system audio session
    private lateinit var ttsManager: TTSManager

    override fun onCreate() {
        super.onCreate()
        
        // Setup a dummy ExoPlayer just to hold the MediaSession lock-screen controls alive
        // Real playback is handled by TTSManager natively.
        player = ExoPlayer.Builder(this).build()
        
        mediaSession = MediaSession.Builder(this, player)
            .setCallback(object : MediaSession.Callback {
                // intercept play/pause to route to TTS engine instead of ExoPlayer
            })
            .build()
            
        ttsManager = TTSManager(this)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player != null && !player.playWhenReady || player?.playbackState == Player.STATE_ENDED) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        ttsManager.shutdown()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
