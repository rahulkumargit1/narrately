package com.example.pdfreader

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.pdfreader.security.SecurityManager
import com.example.pdfreader.tts.MediaPlaybackService
import com.example.pdfreader.tts.SleepTimerManager
import com.example.pdfreader.ui.ReaderViewModel
import com.example.pdfreader.ui.screens.*
import com.example.pdfreader.ui.theme.LumenTheme
import dagger.hilt.android.AndroidEntryPoint

enum class Screen { ONBOARDING, LOCK, SPLASH, LIBRARY, PLAYER, SETTINGS, STATS }

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private var playbackService: MediaPlaybackService? = null
    private var serviceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            playbackService = (binder as? MediaPlaybackService.LocalBinder)?.getService()
            serviceBound = true
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            playbackService = null
            serviceBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Prevent screenshots when app lock is enabled
        val secMgr = SecurityManager(this)
        if (secMgr.isAppLockEnabled) {
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        }

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                registerForActivityResult(ActivityResultContracts.RequestPermission()) {}.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            LumenTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    NarratelyApp(
                        getPlaybackService = { playbackService },
                        startService = { startAndBindService() },
                        stopService = { stopAndUnbindService() },
                    )
                }
            }
        }
    }

    private fun startAndBindService() {
        MediaPlaybackService.start(this)
        bindService(Intent(this, MediaPlaybackService::class.java), serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun stopAndUnbindService() {
        if (serviceBound) {
            unbindService(serviceConnection)
            serviceBound = false
        }
        MediaPlaybackService.stop(this)
        playbackService = null
    }

    override fun onDestroy() {
        if (serviceBound) {
            unbindService(serviceConnection)
            serviceBound = false
        }
        super.onDestroy()
    }
}

@Composable
fun NarratelyApp(
    getPlaybackService: () -> MediaPlaybackService?,
    startService: () -> Unit,
    stopService: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel: ReaderViewModel = hiltViewModel()
    val securityManager = remember { SecurityManager(context) }
    val sleepTimerManager = remember { SleepTimerManager() }

    // Determine initial screen
    val initialScreen = remember {
        when {
            !securityManager.hasCompletedOnboarding -> Screen.ONBOARDING
            securityManager.isAppLockEnabled -> Screen.LOCK
            else -> Screen.SPLASH
        }
    }
    var currentScreen by remember { mutableStateOf(initialScreen) }

    // Auto-lock when app goes to background
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    securityManager.lastBackgroundTimestamp = System.currentTimeMillis()
                }
                Lifecycle.Event.ON_START -> {
                    if (securityManager.shouldLockOnResume() && currentScreen != Screen.LOCK && currentScreen != Screen.ONBOARDING) {
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

    // Stats
    val totalListeningSeconds by viewModel.totalListeningSeconds.collectAsState()
    val listeningDays by viewModel.listeningDays.collectAsState()
    val completedDocs by viewModel.completedDocs.collectAsState()
    val weeklyStats by viewModel.weeklyStats.collectAsState()

    val fontSize = securityManager.fontSize

    // Update notification when playback state changes
    LaunchedEffect(isPlaying, currentDocument, currentChunkIndex) {
        val service = getPlaybackService()
        val doc = currentDocument
        if (service != null && doc != null) {
            val progress = if (textChunks.isNotEmpty()) "${currentChunkIndex + 1}/${textChunks.size}" else ""
            service.updateNotification(
                doc.title.removeSuffix(".pdf").removeSuffix(".txt"),
                if (isPlaying) "Playing • $progress" else "Paused • $progress",
                isPlaying,
            )
        }
    }

    // Wire service callbacks
    LaunchedEffect(currentScreen) {
        if (currentScreen == Screen.PLAYER) {
            val service = getPlaybackService()
            service?.onPlayPause = { viewModel.playPause() }
            service?.onNext = { viewModel.seekForward() }
            service?.onPrev = { viewModel.seekBackward() }
            service?.onStop = { viewModel.stopPlayback() }
        }
    }

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
            Screen.ONBOARDING -> {
                OnboardingScreen(onFinished = {
                    securityManager.hasCompletedOnboarding = true
                    currentScreen = if (securityManager.isAppLockEnabled) Screen.LOCK else Screen.SPLASH
                })
            }
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
                        // Start background playback service
                        startService()
                        currentScreen = Screen.PLAYER
                    },
                    onDeleteDocument = { doc -> viewModel.deleteDocument(doc) },
                    onClearError = { viewModel.clearError() },
                    onOpenSettings = { currentScreen = Screen.SETTINGS },
                    onOpenStats = { viewModel.refreshStats(); currentScreen = Screen.STATS },
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
                        stopService()
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
            Screen.STATS -> {
                ListeningStatsScreen(
                    totalSeconds = totalListeningSeconds,
                    listeningDays = listeningDays,
                    completedDocs = completedDocs,
                    weeklyStats = weeklyStats,
                    onBack = { currentScreen = Screen.LIBRARY },
                )
            }
        }
    }
}
