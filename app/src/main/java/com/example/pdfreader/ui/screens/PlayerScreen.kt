package com.example.pdfreader.ui.screens

import androidx.compose.animation.*
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
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
    fontSize: Float,
    sleepTimerRemaining: Long,
    isSleepTimerActive: Boolean,
    searchQuery: String,
    searchResults: List<Int>,
    onPlayPause: () -> Unit,
    onSeekForward: () -> Unit,
    onSeekBackward: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onPitchChange: (Float) -> Unit,
    onSeekToChunk: (Int) -> Unit,
    onAddBookmark: () -> Unit,
    onDeleteBookmark: (BookmarkEntity) -> Unit,
    onJumpToBookmark: (BookmarkEntity) -> Unit,
    onSetSleepTimer: (Int) -> Unit,
    onCancelSleepTimer: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onBack: () -> Unit,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(currentChunkIndex) {
        if (textChunks.isNotEmpty() && currentChunkIndex in textChunks.indices) {
            listState.animateScrollToItem(currentChunkIndex, scrollOffset = -200)
        }
    }

    // ─── Ambient glow ───
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowX by infiniteTransition.animateFloat(0.3f, 0.7f, infiniteRepeatable(tween(6000, easing = EaseInOutSine), RepeatMode.Reverse), label = "gx")
    val glowY by infiniteTransition.animateFloat(0.1f, 0.35f, infiniteRepeatable(tween(8000, easing = EaseInOutSine), RepeatMode.Reverse), label = "gy")

    // Play button pulse
    val pulseScale by infiniteTransition.animateFloat(1f, 1.08f, infiniteRepeatable(tween(1200, easing = EaseInOutSine), RepeatMode.Reverse), label = "pulse")

    // Sheet states
    var showSpeedSheet by remember { mutableStateOf(false) }
    var showPitchSheet by remember { mutableStateOf(false) }
    var showBookmarksSheet by remember { mutableStateOf(false) }
    var showSleepSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }

    if (showSpeedSheet) SpeedPitchSheet("Playback Speed", playbackSpeed, listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.5f, 3.0f), { "${it}x" }, onSpeedChange) { showSpeedSheet = false }
    if (showPitchSheet) SpeedPitchSheet("Voice Pitch", pitch, listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f), { if (it == 1.0f) "Normal" else "$it" }, onPitchChange) { showPitchSheet = false }
    if (showBookmarksSheet) BookmarksSheet(bookmarks, textChunks, currentChunkIndex, { onJumpToBookmark(it); showBookmarksSheet = false }, onDeleteBookmark) { showBookmarksSheet = false }
    if (showSleepSheet) SleepTimerSheet(sleepTimerRemaining, isSleepTimerActive, onSetSleepTimer, onCancelSleepTimer) { showSleepSheet = false }
    if (showSettingsSheet) SettingsSheet(fontSize, onFontSizeChange) { showSettingsSheet = false }

    Box(
        modifier = Modifier.fillMaxSize().background(Background).drawBehind {
            drawCircle(Brush.radialGradient(listOf(PrimaryGlow, Color.Transparent), Offset(size.width * glowX, size.height * glowY), size.width * 0.6f), size.width * 0.6f, Offset(size.width * glowX, size.height * glowY))
        }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ─── Top Bar ───
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) { Icon(Icons.Default.KeyboardArrowDown, "Close", tint = OnSurfaceVariant, modifier = Modifier.size(28.dp)) }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("NOW PLAYING", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant.copy(alpha = 0.35f), letterSpacing = 1.5.sp, fontSize = 9.sp)
                    Spacer(Modifier.height(2.dp))
                    Text(documentTitle.removeSuffix(".pdf").removeSuffix(".txt"), style = MaterialTheme.typography.titleSmall, color = OnBackground, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = { showSearch = !showSearch; if (!showSearch) onClearSearch() }) {
                    Icon(if (showSearch) Icons.Default.Close else Icons.Default.Search, "Search", tint = if (showSearch) Primary else OnSurfaceVariant, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onAddBookmark) { Icon(Icons.Default.BookmarkAdd, "Bookmark", tint = Primary, modifier = Modifier.size(20.dp)) }
                IconButton(onClick = { showSettingsSheet = true }) { Icon(Icons.Default.Settings, "Settings", tint = OnSurfaceVariant, modifier = Modifier.size(20.dp)) }
            }

            // ─── Search Bar ───
            AnimatedVisibility(visible = showSearch, enter = slideInVertically { -it } + fadeIn(), exit = slideOutVertically { -it } + fadeOut()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 6.dp).clip(RoundedCornerShape(14.dp)).background(GlassSurface).border(0.5.dp, GlassBorder, RoundedCornerShape(14.dp)).padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Search, null, tint = OnSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        singleLine = true,
                        textStyle = TextStyle(color = OnBackground, fontSize = 14.sp),
                        cursorBrush = SolidColor(Primary),
                        modifier = Modifier.weight(1f),
                        decorationBox = { inner ->
                            if (searchQuery.isEmpty()) Text("Search in document…", color = OnSurfaceVariant.copy(alpha = 0.3f), fontSize = 14.sp)
                            inner()
                        },
                    )
                    if (searchResults.isNotEmpty()) {
                        Text("${searchResults.size} found", style = MaterialTheme.typography.labelSmall, color = Primary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }

            // ─── Stats Glass Bar ───
            if (textChunks.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp).clip(RoundedCornerShape(12.dp)).background(GlassSurface).border(0.5.dp, GlassBorder, RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    MiniStat("$totalWords", "words"); MiniStat("~${estimatedMinutes}m", "listen"); MiniStat("${progressPercent}%", "done"); MiniStat("${currentChunkIndex + 1}/${textChunks.size}", "chunk")
                }
            }

            // ─── Content ───
            if (isLoading) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary, strokeWidth = 2.dp, modifier = Modifier.size(32.dp))
                }
            } else if (textChunks.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No content", style = MaterialTheme.typography.bodyLarge, color = OnSurfaceVariant)
                }
            } else {
                LazyColumn(state = listState, modifier = Modifier.weight(1f).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(vertical = 16.dp)) {
                    itemsIndexed(textChunks) { index, chunk ->
                        val isActive = index == currentChunkIndex
                        val isBookmarked = bookmarks.any { it.chunkIndex == index }
                        val isSearchHit = searchResults.contains(index)
                        val textColor by animateColorAsState(
                            when {
                                isActive -> OnBackground
                                index < currentChunkIndex -> OnSurfaceVariant.copy(alpha = 0.25f)
                                else -> OnSurfaceVariant.copy(alpha = 0.45f)
                            }, tween(400), label = "cc"
                        )

                        // Staggered entrance
                        val enterAlpha by animateFloatAsState(
                            targetValue = 1f,
                            animationSpec = tween(500, delayMillis = (index % 5) * 50),
                            label = "stagger"
                        )

                        Row(modifier = Modifier.fillMaxWidth().graphicsLayer { alpha = enterAlpha }, verticalAlignment = Alignment.Top) {
                            if (isBookmarked) {
                                Box(Modifier.padding(top = if (isActive) 18.dp else 6.dp).size(4.dp).clip(CircleShape).background(Primary))
                                Spacer(Modifier.width(4.dp))
                            }
                            Text(
                                text = chunk,
                                style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp, fontSize = fontSize.sp),
                                color = textColor,
                                modifier = Modifier.weight(1f)
                                    .then(
                                        if (isActive) Modifier.clip(RoundedCornerShape(14.dp)).background(GlassSurface).border(0.5.dp, GlassBorder, RoundedCornerShape(14.dp)).padding(16.dp)
                                        else if (isSearchHit) Modifier.clip(RoundedCornerShape(14.dp)).background(AccentOrange.copy(alpha = 0.06f)).border(0.5.dp, AccentOrange.copy(alpha = 0.2f), RoundedCornerShape(14.dp)).padding(12.dp)
                                        else Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                    )
                                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onSeekToChunk(index) },
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
                        onValueChange = { onSeekToChunk((it * (textChunks.size - 1)).toInt()) },
                        modifier = Modifier.fillMaxWidth().height(20.dp),
                        colors = SliderDefaults.colors(thumbColor = OnBackground, activeTrackColor = OnBackground, inactiveTrackColor = GlassSurfaceHigh),
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${currentChunkIndex + 1}", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant.copy(alpha = 0.4f), fontSize = 10.sp)
                        if (isSleepTimerActive) {
                            val min = sleepTimerRemaining / 60
                            val sec = sleepTimerRemaining % 60
                            Text("⏱ ${min}:${sec.toString().padStart(2, '0')}", style = MaterialTheme.typography.labelSmall, color = AccentOrange, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Text("${textChunks.size}", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant.copy(alpha = 0.4f), fontSize = 10.sp)
                    }
                }
            }

            // ─── Playback Controls ───
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onSeekBackward, modifier = Modifier.size(52.dp)) { Icon(Icons.Default.SkipPrevious, "Prev", tint = OnBackground, modifier = Modifier.size(34.dp)) }
                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier.size(72.dp).scale(if (isPlaying) pulseScale else 1f).clip(CircleShape).background(OnBackground),
                ) {
                    Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, if (isPlaying) "Pause" else "Play", tint = Background, modifier = Modifier.size(38.dp))
                }
                IconButton(onClick = onSeekForward, modifier = Modifier.size(52.dp)) { Icon(Icons.Default.SkipNext, "Next", tint = OnBackground, modifier = Modifier.size(34.dp)) }
            }

            // ─── Bottom Controls ───
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 12.dp).navigationBarsPadding(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GlassChip(Modifier.weight(1f), "${playbackSpeed}x", { Icon(Icons.Outlined.Speed, null, tint = if (playbackSpeed != 1.0f) Primary else OnSurfaceVariant, modifier = Modifier.size(14.dp)) }, playbackSpeed != 1.0f, Primary) { showSpeedSheet = true }
                GlassChip(Modifier.weight(1f), if (pitch == 1.0f) "Pitch" else "$pitch", { Icon(Icons.Outlined.Tune, null, tint = if (pitch != 1.0f) Secondary else OnSurfaceVariant, modifier = Modifier.size(14.dp)) }, pitch != 1.0f, Secondary) { showPitchSheet = true }
                GlassChip(Modifier.weight(1f), if (bookmarks.isEmpty()) "Marks" else "${bookmarks.size}", { Icon(Icons.Default.Bookmarks, null, tint = if (bookmarks.isNotEmpty()) AccentOrange else OnSurfaceVariant, modifier = Modifier.size(14.dp)) }, bookmarks.isNotEmpty(), AccentOrange) { showBookmarksSheet = true }
                GlassChip(Modifier.weight(1f), if (isSleepTimerActive) "⏱" else "Sleep", { Icon(Icons.Default.Bedtime, null, tint = if (isSleepTimerActive) AccentPurple else OnSurfaceVariant, modifier = Modifier.size(14.dp)) }, isSleepTimerActive, AccentPurple) { showSleepSheet = true }
            }
        }
    }
}

// ─── Glass Chip ───
@Composable
private fun GlassChip(modifier: Modifier, label: String, icon: @Composable () -> Unit, isActive: Boolean, activeColor: Color, onClick: () -> Unit) {
    Row(
        modifier = modifier.clip(RoundedCornerShape(12.dp)).clickable { onClick() }
            .background(if (isActive) activeColor.copy(alpha = 0.08f) else GlassSurface)
            .border(0.5.dp, if (isActive) activeColor.copy(alpha = 0.3f) else GlassBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        icon(); Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium, color = if (isActive) activeColor else OnSurfaceVariant, fontSize = 11.sp)
    }
}

@Composable
private fun MiniStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.labelMedium, color = OnBackground, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Text(label, style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant.copy(alpha = 0.4f), fontSize = 9.sp)
    }
}

// ─── Sleep Timer Sheet ───
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SleepTimerSheet(remaining: Long, isActive: Boolean, onSet: (Int) -> Unit, onCancel: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = SurfaceContainer, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = { Box(Modifier.padding(vertical = 12.dp).width(36.dp).height(4.dp).clip(RoundedCornerShape(50)).background(OnSurfaceVariant.copy(alpha = 0.3f))) }
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 40.dp)) {
            Text("Sleep Timer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = OnBackground)
            Spacer(Modifier.height(6.dp))

            if (isActive) {
                val min = remaining / 60; val sec = remaining % 60
                Text("Stopping in ${min}:${sec.toString().padStart(2, '0')}", style = MaterialTheme.typography.bodyMedium, color = AccentPurple, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onCancel(); onDismiss() }.background(Error.copy(alpha = 0.1f)).border(0.5.dp, Error.copy(alpha = 0.3f), RoundedCornerShape(12.dp)).padding(14.dp), horizontalArrangement = Arrangement.Center) {
                    Text("Cancel Timer", color = Error, fontWeight = FontWeight.SemiBold)
                }
            } else {
                Text("Stop playback after:", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant.copy(alpha = 0.5f))
            }

            Spacer(Modifier.height(16.dp))
            val options = listOf(5 to "5 min", 15 to "15 min", 30 to "30 min", 45 to "45 min", 60 to "1 hour", 90 to "1.5 hours", 120 to "2 hours")
            val chunked = options.chunked(4)
            for (row in chunked) {
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for ((min, label) in row) {
                        Box(
                            Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable { onSet(min); onDismiss() }.background(GlassSurface).border(0.5.dp, GlassBorder, RoundedCornerShape(12.dp)).padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center,
                        ) { Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium, color = OnSurfaceVariant) }
                    }
                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

// ─── Settings Sheet (Font Size) ───
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSheet(fontSize: Float, onFontSizeChange: (Float) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = SurfaceContainer, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = { Box(Modifier.padding(vertical = 12.dp).width(36.dp).height(4.dp).clip(RoundedCornerShape(50)).background(OnSurfaceVariant.copy(alpha = 0.3f))) }
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 40.dp)) {
            Text("Reading Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = OnBackground)
            Spacer(Modifier.height(20.dp))
            Text("Font Size: ${fontSize.toInt()}sp", style = MaterialTheme.typography.labelLarge, color = OnSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("A", color = OnSurfaceVariant, fontSize = 12.sp)
                Slider(value = fontSize, onValueChange = onFontSizeChange, valueRange = 12f..24f, modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                    colors = SliderDefaults.colors(thumbColor = Primary, activeTrackColor = Primary, inactiveTrackColor = GlassSurfaceHigh))
                Text("A", color = OnSurfaceVariant, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            Text("Preview: The quick brown fox jumps over the lazy dog.", style = MaterialTheme.typography.bodyLarge.copy(fontSize = fontSize.sp), color = OnBackground, modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(GlassSurface).border(0.5.dp, GlassBorder, RoundedCornerShape(12.dp)).padding(16.dp).fillMaxWidth())
        }
    }
}

// ─── Bookmarks Sheet ───
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookmarksSheet(bookmarks: List<BookmarkEntity>, textChunks: List<String>, currentChunkIndex: Int, onJump: (BookmarkEntity) -> Unit, onDelete: (BookmarkEntity) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = SurfaceContainer, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = { Box(Modifier.padding(vertical = 12.dp).width(36.dp).height(4.dp).clip(RoundedCornerShape(50)).background(OnSurfaceVariant.copy(alpha = 0.3f))) }
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 40.dp)) {
            Text("Bookmarks", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = OnBackground)
            Spacer(Modifier.height(16.dp))
            if (bookmarks.isEmpty()) {
                Text("No bookmarks yet. Tap the bookmark icon to save your position.", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 24.dp))
            } else {
                bookmarks.forEach { bm ->
                    val preview = textChunks.getOrNull(bm.chunkIndex)?.take(80) ?: ""
                    val isCurrent = bm.chunkIndex == currentChunkIndex
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(12.dp)).clickable { onJump(bm) }
                            .background(if (isCurrent) Primary.copy(alpha = 0.1f) else GlassSurface)
                            .border(0.5.dp, if (isCurrent) Primary.copy(alpha = 0.3f) else GlassBorder, RoundedCornerShape(12.dp)).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(bm.label, style = MaterialTheme.typography.labelLarge, color = if (isCurrent) Primary else OnBackground, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(2.dp))
                            Text("$preview…", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant.copy(alpha = 0.5f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        IconButton(onClick = { onDelete(bm) }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Delete, "Del", tint = OnSurfaceVariant.copy(alpha = 0.3f), modifier = Modifier.size(16.dp)) }
                    }
                }
            }
        }
    }
}

// ─── Speed/Pitch Sheet ───
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpeedPitchSheet(title: String, currentValue: Float, values: List<Float>, formatLabel: (Float) -> String, onChange: (Float) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = SurfaceContainer, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = { Box(Modifier.padding(vertical = 12.dp).width(36.dp).height(4.dp).clip(RoundedCornerShape(50)).background(OnSurfaceVariant.copy(alpha = 0.3f))) }
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 40.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = OnBackground, modifier = Modifier.padding(bottom = 16.dp))
            val chunked = values.chunked(4)
            for (row in chunked) {
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (v in row) {
                        val isSelected = v == currentValue
                        Box(
                            Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable { onChange(v) }
                                .background(if (isSelected) Primary.copy(alpha = 0.2f) else GlassSurface)
                                .border(if (isSelected) 1.5.dp else 0.5.dp, if (isSelected) Primary else GlassBorder, RoundedCornerShape(12.dp)).padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center,
                        ) { Text(formatLabel(v), style = MaterialTheme.typography.labelLarge, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, color = if (isSelected) Primary else OnSurfaceVariant) }
                    }
                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}
