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
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pdfreader.data.BookmarkEntity
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
    bookmarks: List<BookmarkEntity>,
    onPlayPause: () -> Unit,
    onSeekForward: () -> Unit,
    onSeekBackward: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onPitchChange: (Float) -> Unit,
    onSeekToChunk: (Int) -> Unit,
    onAddBookmark: () -> Unit,
    onDeleteBookmark: (BookmarkEntity) -> Unit,
    onJumpToBookmark: (BookmarkEntity) -> Unit,
    onBack: () -> Unit,
) {
    val listState = rememberLazyListState()

    // Auto-scroll to active chunk
    LaunchedEffect(currentChunkIndex) {
        if (textChunks.isNotEmpty() && currentChunkIndex in textChunks.indices) {
            listState.animateScrollToItem(currentChunkIndex, scrollOffset = -200)
        }
    }

    // Ambient glow
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

    // Sheet states
    var showSpeedSheet by remember { mutableStateOf(false) }
    var showPitchSheet by remember { mutableStateOf(false) }
    var showBookmarksSheet by remember { mutableStateOf(false) }

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
    if (showBookmarksSheet) {
        BookmarksSheet(
            bookmarks = bookmarks,
            textChunks = textChunks,
            currentChunkIndex = currentChunkIndex,
            onJump = { onJumpToBookmark(it); showBookmarksSheet = false },
            onDelete = onDeleteBookmark,
            onDismiss = { showBookmarksSheet = false },
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .drawBehind {
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
                // Bookmark add button
                IconButton(onClick = onAddBookmark) {
                    Icon(Icons.Default.BookmarkAdd, "Add Bookmark", tint = Primary, modifier = Modifier.size(22.dp))
                }
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

            // ─── Content ───
            if (isLoading) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary, strokeWidth = 2.dp, modifier = Modifier.size(32.dp))
                }
            } else if (textChunks.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No content", style = MaterialTheme.typography.bodyLarge, color = OnSurfaceVariant)
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
                        val isBookmarked = bookmarks.any { it.chunkIndex == index }
                        val textColor by animateColorAsState(
                            targetValue = when {
                                isActive -> OnBackground
                                index < currentChunkIndex -> OnSurfaceVariant.copy(alpha = 0.25f)
                                else -> OnSurfaceVariant.copy(alpha = 0.45f)
                            },
                            animationSpec = tween(400),
                            label = "chunkColor",
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top,
                        ) {
                            // Bookmark indicator
                            if (isBookmarked) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = if (isActive) 18.dp else 6.dp)
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(Primary),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }

                            Text(
                                text = chunk,
                                style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp, fontSize = 15.sp),
                                color = textColor,
                                modifier = Modifier
                                    .weight(1f)
                                    .then(
                                        if (isActive) Modifier
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(GlassSurface)
                                            .border(0.5.dp, GlassBorder, RoundedCornerShape(14.dp))
                                            .padding(16.dp)
                                        else Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                    )
                                    .clickable(
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() },
                                    ) { onSeekToChunk(index) },
                            )
                        }
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

            // ─── Bottom Controls: Speed / Pitch / Bookmarks ───
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 16.dp)
                    .navigationBarsPadding(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Speed
                GlassChip(
                    modifier = Modifier.weight(1f),
                    label = "${playbackSpeed}x",
                    icon = { Icon(Icons.Outlined.Speed, null, tint = if (playbackSpeed != 1.0f) Primary else OnSurfaceVariant, modifier = Modifier.size(15.dp)) },
                    isActive = playbackSpeed != 1.0f,
                    activeColor = Primary,
                    onClick = { showSpeedSheet = true },
                )
                // Pitch
                GlassChip(
                    modifier = Modifier.weight(1f),
                    label = if (pitch == 1.0f) "Pitch" else "$pitch",
                    icon = { Icon(Icons.Outlined.Tune, null, tint = if (pitch != 1.0f) Secondary else OnSurfaceVariant, modifier = Modifier.size(15.dp)) },
                    isActive = pitch != 1.0f,
                    activeColor = Secondary,
                    onClick = { showPitchSheet = true },
                )
                // Bookmarks
                GlassChip(
                    modifier = Modifier.weight(1f),
                    label = if (bookmarks.isEmpty()) "Marks" else "${bookmarks.size}",
                    icon = { Icon(Icons.Default.Bookmarks, null, tint = if (bookmarks.isNotEmpty()) AccentOrange else OnSurfaceVariant, modifier = Modifier.size(15.dp)) },
                    isActive = bookmarks.isNotEmpty(),
                    activeColor = AccentOrange,
                    onClick = { showBookmarksSheet = true },
                )
            }
        }
    }
}

// ─── Glass Chip ───
@Composable
private fun GlassChip(
    modifier: Modifier = Modifier,
    label: String,
    icon: @Composable () -> Unit,
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .background(if (isActive) activeColor.copy(alpha = 0.08f) else GlassSurface)
            .border(0.5.dp, if (isActive) activeColor.copy(alpha = 0.3f) else GlassBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        icon()
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            color = if (isActive) activeColor else OnSurfaceVariant,
            fontSize = 12.sp,
        )
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

// ─── Bookmarks Sheet ───
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookmarksSheet(
    bookmarks: List<BookmarkEntity>,
    textChunks: List<String>,
    currentChunkIndex: Int,
    onJump: (BookmarkEntity) -> Unit,
    onDelete: (BookmarkEntity) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceContainer,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier.padding(vertical = 12.dp).width(36.dp).height(4.dp)
                    .clip(RoundedCornerShape(50)).background(OnSurfaceVariant.copy(alpha = 0.3f)),
            )
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 40.dp),
        ) {
            Text("Bookmarks", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = OnBackground)
            Spacer(modifier = Modifier.height(16.dp))

            if (bookmarks.isEmpty()) {
                Text(
                    "No bookmarks yet. Tap the bookmark icon to save your position.",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            } else {
                bookmarks.forEach { bm ->
                    val preview = textChunks.getOrNull(bm.chunkIndex)?.take(80) ?: ""
                    val isCurrent = bm.chunkIndex == currentChunkIndex
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onJump(bm) }
                            .background(if (isCurrent) Primary.copy(alpha = 0.1f) else GlassSurface)
                            .border(0.5.dp, if (isCurrent) Primary.copy(alpha = 0.3f) else GlassBorder, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(bm.label, style = MaterialTheme.typography.labelLarge, color = if (isCurrent) Primary else OnBackground, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("$preview…", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant.copy(alpha = 0.5f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        IconButton(onClick = { onDelete(bm) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Delete, "Delete", tint = OnSurfaceVariant.copy(alpha = 0.3f), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

// ─── Speed/Pitch Sheet ───
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
                modifier = Modifier.padding(vertical = 12.dp).width(36.dp).height(4.dp)
                    .clip(RoundedCornerShape(50)).background(OnSurfaceVariant.copy(alpha = 0.3f)),
            )
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 40.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = OnBackground, modifier = Modifier.padding(bottom = 16.dp))
            val chunked = values.chunked(4)
            for (row in chunked) {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (v in row) {
                        val isSelected = v == currentValue
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onChange(v) }
                                .background(if (isSelected) Primary.copy(alpha = 0.2f) else GlassSurface)
                                .border(if (isSelected) 1.5.dp else 0.5.dp, if (isSelected) Primary else GlassBorder, RoundedCornerShape(12.dp))
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(formatLabel(v), style = MaterialTheme.typography.labelLarge, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, color = if (isSelected) Primary else OnSurfaceVariant)
                        }
                    }
                    repeat(4 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }
        }
    }
}
