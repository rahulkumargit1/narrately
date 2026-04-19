package com.example.pdfreader.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pdfreader.data.DocumentEntity
import com.example.pdfreader.data.ProgressEntity
import com.example.pdfreader.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    documents: List<DocumentEntity>,
    progressMap: Map<Int, ProgressEntity>,
    isLoading: Boolean,
    errorMessage: String?,
    totalListeningHours: Float,
    totalChunksCompleted: Long,
    onImportDocument: (Uri) -> Unit,
    onDocumentClick: (DocumentEntity) -> Unit,
    onDeleteDocument: (DocumentEntity) -> Unit,
    onClearError: () -> Unit,
) {
    val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { onImportDocument(it) } }

    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when { hour < 12 -> "Good Morning"; hour < 17 -> "Good Afternoon"; else -> "Good Evening" }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(errorMessage) { errorMessage?.let { snackbarHostState.showSnackbar(it); onClearError() } }

    var documentToDelete by remember { mutableStateOf<DocumentEntity?>(null) }
    if (documentToDelete != null) {
        AlertDialog(
            onDismissRequest = { documentToDelete = null },
            title = { Text("Delete Document", fontWeight = FontWeight.Bold) },
            text = { Text("Remove \"${documentToDelete!!.title}\" from your library?") },
            confirmButton = { TextButton(onClick = { documentToDelete?.let { onDeleteDocument(it) }; documentToDelete = null }, colors = ButtonDefaults.textButtonColors(contentColor = Error)) { Text("Delete", fontWeight = FontWeight.SemiBold) } },
            dismissButton = { TextButton(onClick = { documentToDelete = null }) { Text("Cancel") } },
            containerColor = SurfaceContainerHigh, titleContentColor = OnBackground, textContentColor = OnSurfaceVariant, shape = RoundedCornerShape(20.dp),
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "ambient")
    val glowOffset by infiniteTransition.animateFloat(0f, 1f, infiniteRepeatable(tween(8000, easing = EaseInOutSine), RepeatMode.Reverse), label = "glow")

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) { data -> Snackbar(snackbarData = data, containerColor = SurfaceContainerHigh, contentColor = OnBackground, actionColor = Primary, shape = RoundedCornerShape(14.dp)) } },
        containerColor = Background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { filePickerLauncher.launch(arrayOf("application/pdf", "text/plain")) },
                containerColor = Primary, contentColor = OnPrimary, shape = CircleShape,
                modifier = Modifier.padding(bottom = 8.dp).size(60.dp),
                elevation = FloatingActionButtonDefaults.elevation(8.dp),
            ) { Icon(Icons.Default.Add, "Import", modifier = Modifier.size(26.dp)) }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Box(modifier = Modifier.fillMaxSize().drawBehind {
                drawCircle(Brush.radialGradient(listOf(PrimaryGlow, Color.Transparent), Offset(size.width * (0.2f + glowOffset * 0.6f), size.height * 0.15f), size.width * 0.5f), size.width * 0.5f, Offset(size.width * (0.2f + glowOffset * 0.6f), size.height * 0.15f))
            })

            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 96.dp)) {
                // Header
                item {
                    Column(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 4.dp)) {
                        Text(greeting, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant.copy(alpha = 0.7f))
                        Spacer(Modifier.height(2.dp))
                        Text("Listen Now", style = MaterialTheme.typography.headlineLarge.copy(fontSize = 34.sp), color = OnBackground, fontWeight = FontWeight.Bold)
                    }
                }

                // Recently Read Carousel
                if (documents.isNotEmpty()) {
                    item { Spacer(Modifier.height(20.dp)); SectionHeader("Continue Listening"); Spacer(Modifier.height(12.dp)) }
                    item {
                        LazyRow(contentPadding = PaddingValues(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            items(documents.take(8)) { doc -> RecentlyReadCard(doc, progressMap[doc.id]) { onDocumentClick(doc) } }
                        }
                    }
                }

                // Stats Bar with listening data
                item { Spacer(Modifier.height(24.dp)); GlassStatsBar(documents.size, totalListeningHours, totalChunksCompleted) }

                // Library list with staggered animation
                item { Spacer(Modifier.height(20.dp)); SectionHeader("Your Library"); Spacer(Modifier.height(8.dp)) }
                if (documents.isEmpty() && !isLoading) { item { EmptyLibraryState() } }
                itemsIndexed(documents) { index, doc ->
                    val staggerDelay = (index * 60).coerceAtMost(400)
                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) { kotlinx.coroutines.delay(staggerDelay.toLong()); visible = true }
                    AnimatedVisibility(visible = visible, enter = slideInHorizontally { it / 3 } + fadeIn(tween(400))) {
                        LibraryListItem(doc, progressMap[doc.id], { onDocumentClick(doc) }, { documentToDelete = doc })
                    }
                }
            }

            if (isLoading) {
                Box(Modifier.fillMaxSize().background(Background.copy(alpha = 0.7f)), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary, strokeWidth = 2.5.dp, modifier = Modifier.size(36.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = OnBackground, modifier = Modifier.padding(horizontal = 24.dp))
}

@Composable
private fun GlassStatsBar(count: Int, listeningHours: Float, chunksCompleted: Long) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).clip(RoundedCornerShape(16.dp)).background(GlassSurface).border(1.dp, GlassBorder, RoundedCornerShape(16.dp)).padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically,
    ) {
        StatItem("$count", "Documents")
        Box(Modifier.width(1.dp).height(28.dp).background(GlassBorder))
        StatItem(String.format("%.1f", listeningHours), "Hours")
        Box(Modifier.width(1.dp).height(28.dp).background(GlassBorder))
        StatItem("$chunksCompleted", "Chunks")
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = OnBackground, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant.copy(alpha = 0.6f))
    }
}

@Composable
private fun EmptyLibraryState() {
    Column(Modifier.fillMaxWidth().padding(horizontal = 48.dp, vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Rounded.LibraryMusic, null, tint = OnSurfaceVariant.copy(alpha = 0.3f), modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(20.dp))
        Text("Your library is empty", style = MaterialTheme.typography.titleMedium, color = OnSurfaceVariant, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text("Tap + to import a PDF or text file", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant.copy(alpha = 0.5f))
    }
}

@Composable
private fun RecentlyReadCard(document: DocumentEntity, progress: ProgressEntity?, onClick: () -> Unit) {
    val progressPercent = if (progress != null && progress.totalChunks > 0) (progress.currentChunkIndex.toFloat() / progress.totalChunks * 100).toInt() else 0
    val gradients = listOf(
        listOf(Primary.copy(alpha = 0.25f), Color(0xFF1A0A0E)),
        listOf(AccentPurple.copy(alpha = 0.25f), Color(0xFF120A1A)),
        listOf(AccentTeal.copy(alpha = 0.25f), Color(0xFF0A1A12)),
        listOf(AccentIndigo.copy(alpha = 0.25f), Color(0xFF0E0A1A)),
        listOf(AccentOrange.copy(alpha = 0.25f), Color(0xFF1A120A)),
        listOf(Secondary.copy(alpha = 0.25f), Color(0xFF0A121A)),
    )
    val gradient = gradients[document.id % gradients.size]
    val thumbnail = remember(document.thumbnailPath) {
        document.thumbnailPath?.let { path -> try { if (File(path).exists()) BitmapFactory.decodeFile(path)?.asImageBitmap() else null } catch (_: Exception) { null } }
    }

    Column(Modifier.width(170.dp).clip(RoundedCornerShape(18.dp)).clickable { onClick() }) {
        Box(Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(18.dp)).background(Brush.verticalGradient(gradient)).border(1.dp, GlassBorder, RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
            if (thumbnail != null) {
                Image(thumbnail, "Cover", ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(18.dp)))
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Background.copy(alpha = 0.6f)))))
            } else {
                val isPdf = document.title.endsWith(".pdf", ignoreCase = true)
                Text(if (isPdf) "PDF" else "TXT", style = MaterialTheme.typography.headlineLarge.copy(fontSize = 28.sp), fontWeight = FontWeight.Bold, color = OnBackground.copy(alpha = 0.15f))
            }
            if (progressPercent > 0) {
                Box(Modifier.align(Alignment.BottomStart).padding(10.dp).clip(RoundedCornerShape(50)).background(Background.copy(alpha = 0.85f)).border(0.5.dp, GlassBorder, RoundedCornerShape(50)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                    Text("${progressPercent}%", style = MaterialTheme.typography.labelSmall, color = Primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(document.title.removeSuffix(".pdf").removeSuffix(".txt"), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = OnBackground, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 4.dp))
    }
}

@Composable
private fun LibraryListItem(document: DocumentEntity, progress: ProgressEntity?, onClick: () -> Unit, onDelete: () -> Unit) {
    val isPdf = document.title.endsWith(".pdf", ignoreCase = true)
    val tagColor = if (isPdf) Primary else Secondary
    val dateStr = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(document.addedTimestamp))
    var showMenu by remember { mutableStateOf(false) }
    val progressPercent = if (progress != null && progress.totalChunks > 0) (progress.currentChunkIndex.toFloat() / progress.totalChunks * 100).toInt() else 0
    val thumbnail = remember(document.thumbnailPath) {
        document.thumbnailPath?.let { path -> try { if (File(path).exists()) BitmapFactory.decodeFile(path)?.asImageBitmap() else null } catch (_: Exception) { null } }
    }

    Row(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 5.dp).clip(RoundedCornerShape(16.dp)).clickable { onClick() }
            .background(GlassSurface).border(0.5.dp, GlassBorder, RoundedCornerShape(16.dp)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(
            if (thumbnail == null) Brush.verticalGradient(if (isPdf) listOf(Primary.copy(alpha = 0.2f), Color(0xFF1A0A0E)) else listOf(Secondary.copy(alpha = 0.2f), Color(0xFF0A121A)))
            else Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
        ), contentAlignment = Alignment.Center) {
            if (thumbnail != null) Image(thumbnail, "Cover", ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)))
            else Text(if (isPdf) "PDF" else "TXT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = tagColor.copy(alpha = 0.6f), fontSize = 11.sp)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(document.title.removeSuffix(".pdf").removeSuffix(".txt"), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = OnBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(dateStr, style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant.copy(alpha = 0.5f))
                if (progressPercent > 0) Text("  ·  ${progressPercent}%", style = MaterialTheme.typography.labelSmall, color = Primary.copy(alpha = 0.7f), fontWeight = FontWeight.SemiBold)
            }
        }
        Box {
            IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, "Options", tint = OnSurfaceVariant.copy(alpha = 0.35f), modifier = Modifier.size(18.dp)) }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(text = { Text("Delete", color = Error) }, onClick = { showMenu = false; onDelete() }, leadingIcon = { Icon(Icons.Default.Delete, null, tint = Error, modifier = Modifier.size(18.dp)) })
            }
        }
    }
}
