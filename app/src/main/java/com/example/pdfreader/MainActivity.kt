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
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.pdfreader.ui.ReaderViewModel
import com.example.pdfreader.ui.screens.LibraryScreen
import com.example.pdfreader.ui.screens.PlayerScreen
import com.example.pdfreader.ui.screens.SplashScreen
import com.example.pdfreader.ui.theme.LumenTheme
import dagger.hilt.android.AndroidEntryPoint

enum class Screen { SPLASH, LIBRARY, PLAYER }

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            LumenTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    NarratelyApp()
                }
            }
        }
    }
}

@Composable
fun NarratelyApp() {
    val viewModel: ReaderViewModel = hiltViewModel()
    var currentScreen by remember { mutableStateOf(Screen.SPLASH) }

    // Collect all states
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
        label = "screenTransition",
    ) { screen ->
        when (screen) {
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
                    onDocumentClick = { doc -> viewModel.openDocument(doc); currentScreen = Screen.PLAYER },
                    onDeleteDocument = { doc -> viewModel.deleteDocument(doc) },
                    onClearError = { viewModel.clearError() },
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
                    onPlayPause = { viewModel.playPause() },
                    onSeekForward = { viewModel.seekForward() },
                    onSeekBackward = { viewModel.seekBackward() },
                    onSpeedChange = { viewModel.setSpeed(it) },
                    onPitchChange = { viewModel.setPitch(it) },
                    onSeekToChunk = { viewModel.seekToChunk(it) },
                    onAddBookmark = { viewModel.addBookmark() },
                    onDeleteBookmark = { viewModel.deleteBookmark(it) },
                    onJumpToBookmark = { viewModel.jumpToBookmark(it) },
                    onBack = {
                        viewModel.saveCurrentProgress()
                        viewModel.stopPlayback()
                        currentScreen = Screen.LIBRARY
                    },
                )
            }
        }
    }
}
