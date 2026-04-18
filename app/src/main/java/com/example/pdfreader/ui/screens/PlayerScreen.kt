package com.example.pdfreader.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pdfreader.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    documentTitle: String,
    textChunks: List<String>,
    currentChunkIndex: Int,
    isPlaying: Boolean,
    playbackSpeed: Float,
    pitch: Float,
    onPlayPause: () -> Unit,
    onSeekForward: () -> Unit,
    onSeekBackward: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onPitchChange: (Float) -> Unit,
    onSeekToChunk: (Int) -> Unit,
    onBack: () -> Unit,
) {
    val listState = rememberLazyListState()

    // Auto-scroll to current chunk
    LaunchedEffect(currentChunkIndex) {
        if (textChunks.isNotEmpty() && currentChunkIndex in textChunks.indices) {
            listState.animateScrollToItem(currentChunkIndex, scrollOffset = -200)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // Background glow for currently playing state
        if (isPlaying) {
            Box(
                modifier = Modifier
                    .size(400.dp)
                    .align(Alignment.TopCenter)
                    .offset(y = (-100).dp)
                    .blur(120.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                PrimaryContainer.copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // --- Top Bar ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = OnSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "NOW PLAYING",
                        style = MaterialTheme.typography.labelSmall,
                        color = Primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = documentTitle,
                        style = MaterialTheme.typography.titleSmall,
                        color = OnBackground,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }
            }

            // --- Reading Text Area (Karaoke Highlight) ---
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 24.dp),
            ) {
                itemsIndexed(textChunks) { index, chunk ->
                    val isActive = index == currentChunkIndex
                    val textColor = animateColorAsState(
                        targetValue = when {
                            isActive -> OnBackground
                            index < currentChunkIndex -> OnSurfaceVariant.copy(alpha = 0.35f)
                            else -> OnSurfaceVariant.copy(alpha = 0.55f)
                        },
                        animationSpec = tween(400),
                        label = "textColor"
                    )

                    Text(
                        text = chunk,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            lineHeight = 28.sp,
                            fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal,
                            fontSize = if (isActive) 17.sp else 16.sp,
                        ),
                        color = textColor.value,
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (isActive) {
                                    Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(SurfaceContainerHigh.copy(alpha = 0.4f))
                                        .padding(16.dp)
                                } else {
                                    Modifier.padding(horizontal = 16.dp)
                                }
                            ),
                    )
                }
            }

            // --- Progress Indicator ---
            if (textChunks.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    Slider(
                        value = currentChunkIndex.toFloat(),
                        onValueChange = { onSeekToChunk(it.toInt()) },
                        valueRange = 0f..(textChunks.size - 1).toFloat().coerceAtLeast(0f),
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = Primary,
                            activeTrackColor = PrimaryContainer,
                            inactiveTrackColor = SurfaceContainerHighest,
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "${currentChunkIndex + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            color = OnSurfaceVariant,
                        )
                        Text(
                            text = "${textChunks.size} chunks",
                            style = MaterialTheme.typography.labelSmall,
                            color = OnSurfaceVariant,
                        )
                    }
                }
            }

            // --- Playback Controls Panel (Glassmorphism) ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .background(SurfaceContainer.copy(alpha = 0.6f))
                    .padding(vertical = 24.dp, horizontal = 32.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Main controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Rewind
                        IconButton(
                            onClick = onSeekBackward,
                            modifier = Modifier.size(56.dp),
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("⏪", fontSize = 22.sp)
                                Text(
                                    "15s",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = OnSurfaceVariant,
                                    fontSize = 9.sp,
                                )
                            }
                        }

                        // Play / Pause
                        FilledIconButton(
                            onClick = onPlayPause,
                            modifier = Modifier.size(72.dp),
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = Color.Transparent,
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(PrimaryContainer, InversePrimary)
                                        ),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = if (isPlaying) "⏸" else "▶",
                                    fontSize = 28.sp,
                                    color = OnPrimary,
                                )
                            }
                        }

                        // Forward
                        IconButton(
                            onClick = onSeekForward,
                            modifier = Modifier.size(56.dp),
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("⏩", fontSize = 22.sp)
                                Text(
                                    "15s",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = OnSurfaceVariant,
                                    fontSize = 9.sp,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // --- Speed & Pitch Controls ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        // Speed button
                        val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                        var speedIndex by remember {
                            mutableIntStateOf(speeds.indexOf(playbackSpeed).coerceAtLeast(2))
                        }
                        OutlinedButton(
                            onClick = {
                                speedIndex = (speedIndex + 1) % speeds.size
                                onSpeedChange(speeds[speedIndex])
                            },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Primary,
                            ),
                        ) {
                            Text(
                                text = "${speeds[speedIndex]}x Speed",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                            )
                        }

                        // Pitch button
                        val pitches = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f)
                        var pitchIndex by remember {
                            mutableIntStateOf(pitches.indexOf(pitch).coerceAtLeast(2))
                        }
                        OutlinedButton(
                            onClick = {
                                pitchIndex = (pitchIndex + 1) % pitches.size
                                onPitchChange(pitches[pitchIndex])
                            },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Secondary,
                            ),
                        ) {
                            Text(
                                text = "Pitch ${pitches[pitchIndex]}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}
