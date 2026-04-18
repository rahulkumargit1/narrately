package com.example.pdfreader.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
    isLoading: Boolean,
    playbackSpeed: Float,
    pitch: Float,
    totalWords: Int,
    estimatedMinutes: Int,
    progressPercent: Int,
    onPlayPause: () -> Unit,
    onSeekForward: () -> Unit,
    onSeekBackward: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onPitchChange: (Float) -> Unit,
    onSeekToChunk: (Int) -> Unit,
    onBack: () -> Unit,
) {
    val listState = rememberLazyListState()

    // Auto-scroll
    LaunchedEffect(currentChunkIndex) {
        if (textChunks.isNotEmpty() && currentChunkIndex in textChunks.indices) {
            listState.animateScrollToItem(currentChunkIndex, scrollOffset = -200)
        }
    }

    // Ambient background glow animation
    val infiniteTransition = rememberInfiniteTransition(label = "playerAmbient")
    val glowX by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(6000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "glowX",
    )
    val glowY by infiniteTransition.animateFloat(
        initialValue = 0.1f, targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(8000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "glowY",
    )

    // Speed/Pitch bottom sheet state
    var showSpeedSheet by remember { mutableStateOf(false) }
    var showPitchSheet by remember { mutableStateOf(false) }

    if (showSpeedSheet) {
        SpeedPitchSheet(
            title = "Playback Speed",
            currentValue = playbackSpeed,
            values = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.5f, 3.0f),
            formatLabel = { "${it}x" },
            onChange = onSpeedChange,
            onDismiss = { showSpeedSheet = false },
        )
    }
    if (showPitchSheet) {
        SpeedPitchSheet(
            title = "Voice Pitch",
            currentValue = pitch,
            values = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f),
            formatLabel = { if (it == 1.0f) "Normal" else "$it" },
            onChange = onPitchChange,
            onDismiss = { showPitchSheet = false },
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .drawBehind {
                // Floating ambient glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(PrimaryGlow, Color.Transparent),
                        center = Offset(size.width * glowX, size.height * glowY),
                        radius = size.width * 0.6f,
                    ),
                    radius = size.width * 0.6f,
                    center = Offset(size.width * glowX, size.height * glowY),
                )
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ─── Top Bar ───
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.KeyboardArrowDown, "Close", tint = OnSurfaceVariant, modifier = Modifier.size(28.dp))
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("NOW PLAYING", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant.copy(alpha = 0.4f), letterSpacing = 1.5.sp, fontSize = 9.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = documentTitle.removeSuffix(".pdf").removeSuffix(".txt"),
                        style = MaterialTheme.typography.titleSmall,
                        color = OnBackground,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(modifier = Modifier.size(48.dp))
            }

            // ─── Stats Glass Bar ───
            if (textChunks.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(GlassSurface)
                        .border(0.5.dp, GlassBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    MiniStat("${totalWords}", "words")
                    MiniStat("~${estimatedMinutes}m", "listen")
                    MiniStat("${progressPercent}%", "done")
                    MiniStat("${currentChunkIndex + 1}/${textChunks.size}", "chunk")
                }
            }

            // ─── Content Area ───
            if (isLoading) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary, strokeWidth = 2.dp, modifier = Modifier.size(32.dp))
                }
            } else if (textChunks.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📄", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No text content found", style = MaterialTheme.typography.bodyLarge, color = OnSurfaceVariant)
                    }
                }
            } else {
                // ─── Karaoke Reading Area ───
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                ) {
                    itemsIndexed(textChunks) { index, chunk ->
                        val isActive = index == currentChunkIndex
                        val textColor by animateColorAsState(
                            targetValue = when {
                                isActive -> OnBackground
                                index < currentChunkIndex -> OnSurfaceVariant.copy(alpha = 0.25f)
                                else -> OnSurfaceVariant.copy(alpha = 0.45f)
                            },
                            animationSpec = tween(400),
                            label = "chunkColor",
                        )

                        Text(
                            text = chunk,
                            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp, fontSize = 15.sp),
                            color = textColor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (isActive) Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(GlassSurface)
                                        .border(0.5.dp, GlassBorder, RoundedCornerShape(14.dp))
                                        .padding(16.dp)
                                    else Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
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
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                    Slider(
                        value = currentChunkIndex.toFloat() / (textChunks.size - 1).coerceAtLeast(1).toFloat(),
                        onValueChange = { f -> onSeekToChunk((f * (textChunks.size - 1)).toInt()) },
                        modifier = Modifier.fillMaxWidth().height(20.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = OnBackground,
                            activeTrackColor = OnBackground,
                            inactiveTrackColor = GlassSurfaceHigh,
                        ),
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${currentChunkIndex + 1}", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant.copy(alpha = 0.4f), fontSize = 10.sp)
                        Text("${textChunks.size}", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant.copy(alpha = 0.4f), fontSize = 10.sp)
                    }
                }
            }

            // ─── Playback Controls ───
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onSeekBackward, modifier = Modifier.size(52.dp)) {
                    Icon(Icons.Default.SkipPrevious, "Previous", tint = OnBackground, modifier = Modifier.size(34.dp))
                }
                // Play/Pause — large glass button
                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(OnBackground),
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Background,
                        modifier = Modifier.size(38.dp),
                    )
                }
                IconButton(onClick = onSeekForward, modifier = Modifier.size(52.dp)) {
                    Icon(Icons.Default.SkipNext, "Next", tint = OnBackground, modifier = Modifier.size(34.dp))
                }
            }

            // ─── Speed & Pitch — Glass Buttons ───
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp)
                    .padding(bottom = 20.dp)
                    .navigationBarsPadding(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Speed button
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showSpeedSheet = true }
                        .background(GlassSurface)
                        .border(0.5.dp, GlassBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Outlined.Speed, null, tint = if (playbackSpeed != 1.0f) Primary else OnSurfaceVariant, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${playbackSpeed}x",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (playbackSpeed != 1.0f) Primary else OnSurfaceVariant,
                    )
                }
                // Pitch button
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showPitchSheet = true }
                        .background(GlassSurface)
                        .border(0.5.dp, GlassBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Outlined.Tune, null, tint = if (pitch != 1.0f) Secondary else OnSurfaceVariant, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (pitch == 1.0f) "Pitch" else "Pitch $pitch",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (pitch != 1.0f) Secondary else OnSurfaceVariant,
                    )
                }
            }
        }
    }
}

// ─── Mini Stat ───
@Composable
private fun MiniStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.labelMedium, color = OnBackground, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Text(label, style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant.copy(alpha = 0.4f), fontSize = 9.sp)
    }
}

// ─── Speed/Pitch Selection Sheet ───
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpeedPitchSheet(
    title: String,
    currentValue: Float,
    values: List<Float>,
    formatLabel: (Float) -> String,
    onChange: (Float) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceContainer,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(OnSurfaceVariant.copy(alpha = 0.3f)),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = OnBackground,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            // Grid of chips
            val chunked = values.chunked(4)
            for (row in chunked) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    for (v in row) {
                        val isSelected = v == currentValue
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onChange(v)
                                    // Don't dismiss — user can hear the change live
                                }
                                .background(if (isSelected) Primary.copy(alpha = 0.2f) else GlassSurface)
                                .border(
                                    width = if (isSelected) 1.5.dp else 0.5.dp,
                                    color = if (isSelected) Primary else GlassBorder,
                                    shape = RoundedCornerShape(12.dp),
                                )
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = formatLabel(v),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Primary else OnSurfaceVariant,
                            )
                        }
                    }
                    // Fill remaining slots if row is incomplete
                    repeat(4 - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
