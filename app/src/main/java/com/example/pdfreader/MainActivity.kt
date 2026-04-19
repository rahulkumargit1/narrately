package com.example.pdfreader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.example.pdfreader.security.SecurityManager
import com.example.pdfreader.tts.SleepTimerManager
import com.example.pdfreader.ui.ReaderViewModel
import com.example.pdfreader.ui.screens.*
import com.example.pdfreader.ui.theme.LumenTheme
import dagger.hilt.android.AndroidEntryPoint

enum class Screen { LOCK, SPLASH, LIBRARY, PLAYER, SETTINGS }

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            LumenTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    NarratelyApp()
                }
            }
        }
    }
}

@Composable
fun NarratelyApp() {
    val context = LocalContext.current
    val viewModel: ReaderViewModel = hiltViewModel()
    val securityManager = remember { SecurityManager(context) }
    val sleepTimerManager = remember { SleepTimerManager() }

    // Determine initial screen
    val needsLock = securityManager.isAppLockEnabled
    var currentScreen by remember { mutableStateOf(if (needsLock) Screen.LOCK else Screen.SPLASH) }

    // Auto-lock when app goes to background
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    securityManager.lastBackgroundTimestamp = System.currentTimeMillis()
                }
                Lifecycle.Event.ON_START -> {
                    if (securityManager.shouldLockOnResume() && currentScreen != Screen.LOCK) {
                        currentScreen = Screen.LOCK
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Collect states
    val documents by viewModel.libraryDocuments.collectAsState()
    val progressMap by viewModel.progressMap.collectAsState()
    val currentDocument by viewModel.currentDocument.collectAsState()
    val textChunks by viewModel.textChunks.collectAsState()
    val currentChunkIndex by viewModel.currentChunkIndex.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val playbackSpeed by viewModel.playbackSpeed.collectAsState()
    val pitch by viewModel.pitch.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val totalWords by viewModel.totalWordCount.collectAsState()
    val estimatedMinutes by viewModel.estimatedMinutes.collectAsState()
    val progressPercent by viewModel.progressPercent.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()
    val sleepActive by sleepTimerManager.isActive.collectAsState()
    val sleepSeconds by sleepTimerManager.remainingSeconds.collectAsState()

    val fontSize = securityManager.fontSize

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            if (targetState.ordinal > initialState.ordinal) {
                slideInVertically { it / 4 } + fadeIn(tween(350)) togetherWith
                    slideOutVertically { -it / 8 } + fadeOut(tween(200))
            } else {
                slideInVertically { -it / 8 } + fadeIn(tween(350)) togetherWith
                    slideOutVertically { it / 4 } + fadeOut(tween(200))
            }
        },
        label = "nav",
    ) { screen ->
        when (screen) {
            Screen.LOCK -> {
                LockScreen(
                    securityManager = securityManager,
                    onUnlocked = { currentScreen = Screen.SPLASH },
                )
            }
            Screen.SPLASH -> {
                SplashScreen(onSplashFinished = { currentScreen = Screen.LIBRARY })
            }
            Screen.LIBRARY -> {
                LibraryScreen(
                    documents = documents,
                    progressMap = progressMap,
                    isLoading = isLoading,
                    errorMessage = errorMessage,
                    onImportDocument = { uri -> viewModel.importDocument(uri) },
                    onDocumentClick = { doc ->
                        viewModel.openDocument(doc)
                        viewModel.setSpeed(securityManager.defaultSpeed)
                        viewModel.setPitch(securityManager.defaultPitch)
                        currentScreen = Screen.PLAYER
                    },
                    onDeleteDocument = { doc -> viewModel.deleteDocument(doc) },
                    onClearError = { viewModel.clearError() },
                    onOpenSettings = { currentScreen = Screen.SETTINGS },
                )
            }
            Screen.PLAYER -> {
                PlayerScreen(
                    documentTitle = currentDocument?.title ?: "Document",
                    textChunks = textChunks,
                    currentChunkIndex = currentChunkIndex,
                    isPlaying = isPlaying,
                    isLoading = isLoading,
                    playbackSpeed = playbackSpeed,
                    pitch = pitch,
                    totalWords = totalWords,
                    estimatedMinutes = estimatedMinutes,
                    progressPercent = progressPercent,
                    bookmarks = bookmarks,
                    fontSize = fontSize,
                    sleepTimerActive = sleepActive,
                    sleepTimerRemaining = sleepTimerManager.formatRemaining(),
                    onPlayPause = { viewModel.playPause() },
                    onSeekForward = { viewModel.seekForward() },
                    onSeekBackward = { viewModel.seekBackward() },
                    onSpeedChange = { viewModel.setSpeed(it) },
                    onPitchChange = { viewModel.setPitch(it) },
                    onSeekToChunk = { viewModel.seekToChunk(it) },
                    onAddBookmark = { viewModel.addBookmark() },
                    onDeleteBookmark = { viewModel.deleteBookmark(it) },
                    onJumpToBookmark = { viewModel.jumpToBookmark(it) },
                    onStartSleepTimer = { min -> sleepTimerManager.start(min) { viewModel.stopPlayback() } },
                    onCancelSleepTimer = { sleepTimerManager.cancel() },
                    onBack = {
                        viewModel.saveCurrentProgress()
                        viewModel.stopPlayback()
                        sleepTimerManager.cancel()
                        currentScreen = Screen.LIBRARY
                    },
                )
            }
            Screen.SETTINGS -> {
                SettingsScreen(
                    securityManager = securityManager,
                    onBack = { currentScreen = Screen.LIBRARY },
                )
            }
        }
    }
}
