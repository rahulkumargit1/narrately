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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
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
    sleepTimerActive: Boolean,
    sleepTimerRemaining: String,
    onPlayPause: () -> Unit,
    onSeekForward: () -> Unit,
    onSeekBackward: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onPitchChange: (Float) -> Unit,
    onSeekToChunk: (Int) -> Unit,
    onAddBookmark: () -> Unit,
    onDeleteBookmark: (BookmarkEntity) -> Unit,
    onJumpToBookmark: (BookmarkEntity) -> Unit,
    onStartSleepTimer: (Int) -> Unit,
    onCancelSleepTimer: () -> Unit,
    onBack: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val listState = rememberLazyListState()

    LaunchedEffect(currentChunkIndex) {
        if (textChunks.isNotEmpty() && currentChunkIndex in textChunks.indices) {
            listState.animateScrollToItem(currentChunkIndex, scrollOffset = -200)
        }
    }

    // Ambient glow
    val infiniteTransition = rememberInfiniteTransition(label = "playerAmbient")
    val glowX by infiniteTransition.animateFloat(0.3f, 0.7f, infiniteRepeatable(tween(6000, easing = EaseInOutSine), RepeatMode.Reverse), label = "glowX")
    val glowY by infiniteTransition.animateFloat(0.1f, 0.35f, infiniteRepeatable(tween(8000, easing = EaseInOutSine), RepeatMode.Reverse), label = "glowY")

    // Play button pulse when playing
    val playPulse by infiniteTransition.animateFloat(1f, 1.06f, infiniteRepeatable(tween(1200, easing = EaseInOutSine), RepeatMode.Reverse), label = "pulse")

    // Sheets
    var showSpeedSheet by remember { mutableStateOf(false) }
    var showPitchSheet by remember { mutableStateOf(false) }
    var showBookmarksSheet by remember { mutableStateOf(false) }
    var showSleepSheet by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    if (showSpeedSheet) SpeedPitchSheet("Playback Speed", playbackSpeed, listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.5f, 3.0f), { "${it}x" }, onSpeedChange, { showSpeedSheet = false })
    if (showPitchSheet) SpeedPitchSheet("Voice Pitch", pitch, listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f), { if (it == 1.0f) "Normal" else "$it" }, onPitchChange, { showPitchSheet = false })
    if (showBookmarksSheet) BookmarksSheet(bookmarks, textChunks, currentChunkIndex, { onJumpToBookmark(it); showBookmarksSheet = false }, onDeleteBookmark, { showBookmarksSheet = false })
    if (showSleepSheet) SleepTimerSheet(sleepTimerActive, onStart = { onStartSleepTimer(it); showSleepSheet = false }, onCancel = { onCancelSleepTimer(); showSleepSheet = false }, onDismiss = { showSleepSheet = false })

    // Search results
    val searchResults = remember(searchQuery, textChunks) {
        if (searchQuery.length >= 2) textChunks.indices.filter { textChunks[it].contains(searchQuery, ignoreCase = true) } else emptyList()
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Background).drawBehind {
            drawCircle(brush = Brush.radialGradient(listOf(PrimaryGlow, Color.Transparent), center = Offset(size.width * glowX, size.height * glowY), radius = size.width * 0.6f), radius = size.width * 0.6f, center = Offset(size.width * glowX, size.height * glowY))
        },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ─── Top Bar ───
            Row(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.KeyboardArrowDown, "Close", tint = OnSurfaceVariant, modifier = Modifier.size(28.dp)) }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("NOW PLAYING", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant.copy(alpha = 0.4f), letterSpacing = 1.5.sp, fontSize = 9.sp)
                    Spacer(Modifier.height(2.dp))
                    Text(documentTitle.removeSuffix(".pdf").removeSuffix(".txt"), style = MaterialTheme.typography.titleSmall, color = OnBackground, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = { showSearch = !showSearch }) { Icon(Icons.Default.Search, "Search", tint = if (showSearch) Primary else OnSurfaceVariant, modifier = Modifier.size(20.dp)) }
                IconButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onAddBookmark() }) { Icon(Icons.Default.BookmarkAdd, "Bookmark", tint = Primary, modifier = Modifier.size(22.dp)) }
            }

            // ─── Search Bar ───
            if (showSearch) {
                OutlinedTextField(
                    value = searchQuery, onValueChange = { searchQuery = it },
                    placeholder = { Text("Search in document…", color = OnSurfaceVariant.copy(0.3f)) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp).height(48.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, unfocusedBorderColor = GlassBorder, cursorColor = Primary, focusedTextColor = OnBackground, unfocusedTextColor = OnBackground),
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${searchResults.size}", style = MaterialTheme.typography.labelSmall, color = Primary)
                                IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Close, "Clear", modifier = Modifier.size(16.dp), tint = OnSurfaceVariant) }
                            }
                        }
                    },
                )
                // Quick jump to results
                if (searchResults.isNotEmpty()) {
                    Row(modifier = Modifier.padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        searchResults.take(5).forEach { idx ->
                            Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onSeekToChunk(idx) }.background(Primary.copy(0.1f)).border(0.5.dp, Primary.copy(0.3f), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                                Text("#${idx + 1}", style = MaterialTheme.typography.labelSmall, color = Primary, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (searchResults.size > 5) Text("+${searchResults.size - 5} more", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant.copy(0.4f), modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }

            // ─── Stats Bar ───
            if (textChunks.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 6.dp).clip(RoundedCornerShape(12.dp)).background(GlassSurface).border(0.5.dp, GlassBorder, RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    MiniStat("$totalWords", "words"); MiniStat("~${estimatedMinutes}m", "listen"); MiniStat("${progressPercent}%", "done"); MiniStat("${currentChunkIndex + 1}/${textChunks.size}", "chunk")
                }
            }

            // ─── Sleep Timer Badge ───
            if (sleepTimerActive) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 2.dp).clip(RoundedCornerShape(20.dp)).background(AccentOrange.copy(0.1f)).border(0.5.dp, AccentOrange.copy(0.3f), RoundedCornerShape(20.dp)).padding(horizontal = 12.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Bedtime, null, tint = AccentOrange, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Sleep in $sleepTimerRemaining", style = MaterialTheme.typography.labelSmall, color = AccentOrange, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    Text("Cancel", style = MaterialTheme.typography.labelSmall, color = AccentOrange.copy(0.6f), modifier = Modifier.clickable { onCancelSleepTimer() })
                }
            }

            // ─── Content ───
            if (isLoading) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Primary, strokeWidth = 2.dp, modifier = Modifier.size(32.dp)) }
            } else if (textChunks.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("No content", style = MaterialTheme.typography.bodyLarge, color = OnSurfaceVariant) }
            } else {
                LazyColumn(state = listState, modifier = Modifier.weight(1f).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp), contentPadding = PaddingValues(vertical = 12.dp)) {
                    itemsIndexed(textChunks) { index, chunk ->
                        val isActive = index == currentChunkIndex
                        val isBookmarked = bookmarks.any { it.chunkIndex == index }
                        val isSearchHit = searchQuery.length >= 2 && chunk.contains(searchQuery, ignoreCase = true)
                        val textColor by animateColorAsState(
                            when { isActive -> OnBackground; index < currentChunkIndex -> OnSurfaceVariant.copy(0.25f); else -> OnSurfaceVariant.copy(0.45f) },
                            tween(400), label = "cc",
                        )

                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                            if (isBookmarked) { Box(Modifier.padding(top = if (isActive) 18.dp else 6.dp).size(4.dp).clip(CircleShape).background(Primary)); Spacer(Modifier.width(4.dp)) }

                            val annotatedText = if (isSearchHit && searchQuery.isNotEmpty()) {
                                buildAnnotatedString {
                                    var start = 0
                                    val lc = chunk.lowercase()
                                    val q = searchQuery.lowercase()
                                    while (true) {
                                        val idx = lc.indexOf(q, start)
                                        if (idx == -1) { append(chunk.substring(start)); break }
                                        append(chunk.substring(start, idx))
                                        withStyle(SpanStyle(background = Primary.copy(0.3f), color = OnBackground, fontWeight = FontWeight.Bold)) { append(chunk.substring(idx, idx + searchQuery.length)) }
                                        start = idx + searchQuery.length
                                    }
                                }
                            } else null

                            if (annotatedText != null) {
                                Text(
                                    text = annotatedText,
                                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp, fontSize = fontSize.sp),
                                    modifier = Modifier.weight(1f).then(
                                        if (isActive) Modifier.clip(RoundedCornerShape(14.dp)).background(GlassSurface).border(0.5.dp, GlassBorder, RoundedCornerShape(14.dp)).padding(16.dp)
                                        else Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                    ).clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onSeekToChunk(index) },
                                )
                            } else {
                                Text(
                                    text = chunk,
                                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp, fontSize = fontSize.sp),
                                    color = textColor,
                                    modifier = Modifier.weight(1f).then(
                                        if (isActive) Modifier.clip(RoundedCornerShape(14.dp)).background(GlassSurface).border(0.5.dp, GlassBorder, RoundedCornerShape(14.dp)).padding(16.dp)
                                        else Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                    ).clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onSeekToChunk(index) },
                                )
                            }
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
                        colors = SliderDefaults.colors(thumbColor = OnBackground, activeTrackColor = OnBackground, inactiveTrackColor = GlassSurfaceHigh),
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${currentChunkIndex + 1}", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant.copy(0.4f), fontSize = 10.sp)
                        Text("${textChunks.size}", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant.copy(0.4f), fontSize = 10.sp)
                    }
                }
            }

            // ─── Playback Controls ───
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onSeekBackward() }, modifier = Modifier.size(52.dp)) { Icon(Icons.Default.SkipPrevious, "Prev", tint = OnBackground, modifier = Modifier.size(34.dp)) }
                IconButton(
                    onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onPlayPause() },
                    modifier = Modifier.size(72.dp).scale(if (isPlaying) playPulse else 1f).clip(CircleShape).background(OnBackground),
                ) { Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, if (isPlaying) "Pause" else "Play", tint = Background, modifier = Modifier.size(38.dp)) }
                IconButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onSeekForward() }, modifier = Modifier.size(52.dp)) { Icon(Icons.Default.SkipNext, "Next", tint = OnBackground, modifier = Modifier.size(34.dp)) }
            }

            // ─── Bottom Controls ───
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 12.dp).navigationBarsPadding(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                GlassChip(Modifier.weight(1f), "${playbackSpeed}x", { Icon(Icons.Outlined.Speed, null, tint = if (playbackSpeed != 1f) Primary else OnSurfaceVariant, modifier = Modifier.size(14.dp)) }, playbackSpeed != 1f, Primary) { showSpeedSheet = true }
                GlassChip(Modifier.weight(1f), if (pitch == 1f) "Pitch" else "$pitch", { Icon(Icons.Outlined.Tune, null, tint = if (pitch != 1f) Secondary else OnSurfaceVariant, modifier = Modifier.size(14.dp)) }, pitch != 1f, Secondary) { showPitchSheet = true }
                GlassChip(Modifier.weight(1f), if (bookmarks.isEmpty()) "Marks" else "${bookmarks.size}", { Icon(Icons.Default.Bookmarks, null, tint = if (bookmarks.isNotEmpty()) AccentOrange else OnSurfaceVariant, modifier = Modifier.size(14.dp)) }, bookmarks.isNotEmpty(), AccentOrange) { showBookmarksSheet = true }
                GlassChip(Modifier.weight(1f), if (sleepTimerActive) "⏰" else "Sleep", { Icon(Icons.Outlined.Bedtime, null, tint = if (sleepTimerActive) AccentTeal else OnSurfaceVariant, modifier = Modifier.size(14.dp)) }, sleepTimerActive, AccentTeal) { showSleepSheet = true }
            }
        }
    }
}

// ─── Glass Chip ───
@Composable
private fun GlassChip(modifier: Modifier, label: String, icon: @Composable () -> Unit, isActive: Boolean, activeColor: Color, onClick: () -> Unit) {
    Row(
        modifier = modifier.clip(RoundedCornerShape(12.dp)).clickable { onClick() }.background(if (isActive) activeColor.copy(0.08f) else GlassSurface).border(0.5.dp, if (isActive) activeColor.copy(0.3f) else GlassBorder, RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
    ) { icon(); Spacer(Modifier.width(4.dp)); Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium, color = if (isActive) activeColor else OnSurfaceVariant, fontSize = 11.sp) }
}

@Composable
private fun MiniStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.labelMedium, color = OnBackground, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Text(label, style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant.copy(0.4f), fontSize = 9.sp)
    }
}

// ─── Sleep Timer Sheet ───
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SleepTimerSheet(isActive: Boolean, onStart: (Int) -> Unit, onCancel: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = SurfaceContainer, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = { Box(Modifier.padding(vertical = 12.dp).width(36.dp).height(4.dp).clip(RoundedCornerShape(50)).background(OnSurfaceVariant.copy(0.3f))) }) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 40.dp)) {
            Text("Sleep Timer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = OnBackground)
            Spacer(Modifier.height(16.dp))
            val options = listOf(5 to "5 min", 15 to "15 min", 30 to "30 min", 45 to "45 min", 60 to "1 hour", 90 to "1.5 hours")
            options.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { (min, label) ->
                        Box(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable { onStart(min) }.background(GlassSurface).border(0.5.dp, GlassBorder, RoundedCornerShape(12.dp)).padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
                            Text(label, style = MaterialTheme.typography.labelLarge, color = OnSurfaceVariant, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
            if (isActive) {
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Cancel Timer", color = Error, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

// ─── Bookmarks Sheet ───
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookmarksSheet(bookmarks: List<BookmarkEntity>, textChunks: List<String>, currentChunkIndex: Int, onJump: (BookmarkEntity) -> Unit, onDelete: (BookmarkEntity) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = SurfaceContainer, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = { Box(Modifier.padding(vertical = 12.dp).width(36.dp).height(4.dp).clip(RoundedCornerShape(50)).background(OnSurfaceVariant.copy(0.3f))) }) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 40.dp)) {
            Text("Bookmarks", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = OnBackground); Spacer(Modifier.height(16.dp))
            if (bookmarks.isEmpty()) { Text("No bookmarks yet. Tap the bookmark icon to save your position.", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant.copy(0.5f), modifier = Modifier.padding(vertical = 24.dp)) }
            else bookmarks.forEach { bm ->
                val preview = textChunks.getOrNull(bm.chunkIndex)?.take(80) ?: ""; val isCurrent = bm.chunkIndex == currentChunkIndex
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(12.dp)).clickable { onJump(bm) }.background(if (isCurrent) Primary.copy(0.1f) else GlassSurface).border(0.5.dp, if (isCurrent) Primary.copy(0.3f) else GlassBorder, RoundedCornerShape(12.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text(bm.label, style = MaterialTheme.typography.labelLarge, color = if (isCurrent) Primary else OnBackground, fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(2.dp)); Text("$preview…", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant.copy(0.5f), maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    IconButton(onClick = { onDelete(bm) }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Delete, "Delete", tint = OnSurfaceVariant.copy(0.3f), modifier = Modifier.size(16.dp)) }
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
        dragHandle = { Box(Modifier.padding(vertical = 12.dp).width(36.dp).height(4.dp).clip(RoundedCornerShape(50)).background(OnSurfaceVariant.copy(0.3f))) }) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 40.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = OnBackground, modifier = Modifier.padding(bottom = 16.dp))
            values.chunked(4).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { v -> val sel = v == currentValue; Box(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable { onChange(v) }.background(if (sel) Primary.copy(0.2f) else GlassSurface).border(if (sel) 1.5.dp else 0.5.dp, if (sel) Primary else GlassBorder, RoundedCornerShape(12.dp)).padding(vertical = 14.dp), contentAlignment = Alignment.Center) { Text(formatLabel(v), style = MaterialTheme.typography.labelLarge, fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium, color = if (sel) Primary else OnSurfaceVariant) } }
                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}
