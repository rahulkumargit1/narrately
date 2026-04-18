package com.example.pdfreader.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pdfreader.ui.theme.*

@Composable
fun PlayerScreen(
    documentTitle: String,
    textChunks: List<String>,
    currentChunkIndex: Int,
    isPlaying: Boolean,
    isLoading: Boolean,
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // ─── Top Bar ───
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = "Close",
                    tint = OnSurfaceVariant,
                    modifier = Modifier.size(28.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "PLAYING FROM LIBRARY",
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant.copy(alpha = 0.5f),
                    letterSpacing = 1.sp,
                    fontSize = 10.sp,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = documentTitle,
                    style = MaterialTheme.typography.titleSmall,
                    color = OnBackground,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            // Spacer to balance the back button
            Spacer(modifier = Modifier.size(48.dp))
        }

        // ─── Loading State ───
        if (isLoading) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    color = Primary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(32.dp),
                )
            }
        } else if (textChunks.isEmpty()) {
            // ─── Empty State ───
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📄", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No text content found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = OnSurfaceVariant,
                    )
                }
            }
        } else {
            // ─── Reading Area (Karaoke Highlight) ───
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 20.dp),
            ) {
                itemsIndexed(textChunks) { index, chunk ->
                    val isActive = index == currentChunkIndex
                    val textColor by animateColorAsState(
                        targetValue = when {
                            isActive -> OnBackground
                            index < currentChunkIndex -> OnSurfaceVariant.copy(alpha = 0.3f)
                            else -> OnSurfaceVariant.copy(alpha = 0.5f)
                        },
                        animationSpec = tween(350),
                        label = "chunkColor",
                    )

                    Text(
                        text = chunk,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            lineHeight = 26.sp,
                            fontWeight = if (isActive) FontWeight.Normal else FontWeight.Normal,
                            fontSize = 15.sp,
                        ),
                        color = textColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (isActive) {
                                    Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(SurfaceContainerLow)
                                        .padding(14.dp)
                                } else {
                                    Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                                }
                            )
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                            ) { onSeekToChunk(index) },
                    )
                }
            }
        }

        // ─── Progress Slider ───
        if (textChunks.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                // Thin Apple Music-style slider
                Slider(
                    value = if (textChunks.isNotEmpty()) {
                        currentChunkIndex.toFloat() / (textChunks.size - 1).coerceAtLeast(1).toFloat()
                    } else 0f,
                    onValueChange = { fraction ->
                        val idx = (fraction * (textChunks.size - 1)).toInt()
                        onSeekToChunk(idx)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = OnBackground,
                        activeTrackColor = OnBackground,
                        inactiveTrackColor = SurfaceContainerHighest,
                    ),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "${currentChunkIndex + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceVariant.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                    )
                    Text(
                        text = "${textChunks.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceVariant.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                    )
                }
            }
        }

        // ─── Playback Controls ───
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Previous
            IconButton(
                onClick = onSeekBackward,
                modifier = Modifier.size(52.dp),
            ) {
                Icon(
                    Icons.Default.SkipPrevious,
                    contentDescription = "Previous",
                    tint = OnBackground,
                    modifier = Modifier.size(36.dp),
                )
            }

            // Play / Pause — large center button
            IconButton(
                onClick = onPlayPause,
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(OnBackground),
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Background,
                    modifier = Modifier.size(36.dp),
                )
            }

            // Next
            IconButton(
                onClick = onSeekForward,
                modifier = Modifier.size(52.dp),
            ) {
                Icon(
                    Icons.Default.SkipNext,
                    contentDescription = "Next",
                    tint = OnBackground,
                    modifier = Modifier.size(36.dp),
                )
            }
        }

        // ─── Speed & Pitch Controls ───
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            // Speed button — reflects ViewModel state
            val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
            val currentSpeedIndex = speeds.indexOf(playbackSpeed).let {
                if (it < 0) 2 else it  // default to 1.0x
            }
            TextButton(
                onClick = {
                    val nextIndex = (currentSpeedIndex + 1) % speeds.size
                    onSpeedChange(speeds[nextIndex])
                },
            ) {
                Text(
                    text = "${playbackSpeed}x",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (playbackSpeed != 1.0f) Primary else OnSurfaceVariant,
                )
            }

            // Pitch button — reflects ViewModel state
            val pitches = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f)
            val currentPitchIndex = pitches.indexOf(pitch).let {
                if (it < 0) 2 else it
            }
            TextButton(
                onClick = {
                    val nextIndex = (currentPitchIndex + 1) % pitches.size
                    onPitchChange(pitches[nextIndex])
                },
            ) {
                Text(
                    text = "Pitch ${pitch}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (pitch != 1.0f) Secondary else OnSurfaceVariant,
                )
            }
        }
    }
}
