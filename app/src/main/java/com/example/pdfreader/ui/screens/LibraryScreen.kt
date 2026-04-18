package com.example.pdfreader.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pdfreader.data.DocumentEntity
import com.example.pdfreader.data.ProgressEntity
import com.example.pdfreader.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    documents: List<DocumentEntity>,
    progressMap: Map<Int, ProgressEntity>,
    onImportDocument: (Uri) -> Unit,
    onDocumentClick: (DocumentEntity) -> Unit,
    onDeleteDocument: (DocumentEntity) -> Unit,
) {
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { onImportDocument(it) }
    }

    // Determine greeting based on time of day
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when {
            hour < 12 -> "Good Morning"
            hour < 17 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // --- Top Bar ---
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {}) {
                            Icon(Icons.Default.Menu, "Menu", tint = OnSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = greeting,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryContainer,
                        )
                    }
                    IconButton(onClick = {
                        filePickerLauncher.launch(arrayOf("application/pdf", "text/plain"))
                    }) {
                        Icon(Icons.Default.Add, "Import", tint = PrimaryContainer)
                    }
                }
            }

            // --- Hero Section ---
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(SurfaceContainerLow)
                        .padding(28.dp)
                ) {
                    // Glow orb
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = 60.dp, y = (-60).dp)
                            .blur(80.dp)
                            .background(Primary.copy(alpha = 0.12f), CircleShape)
                    )

                    Column {
                        Text(
                            text = "The Bookshelf",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = OnBackground,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Continue your journey through the ethereal library.\nEvery word is a portal to another realm.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = {
                                filePickerLauncher.launch(arrayOf("application/pdf", "text/plain"))
                            },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                            ),
                            contentPadding = PaddingValues(),
                            modifier = Modifier.height(52.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(PrimaryContainer, InversePrimary)
                                        ),
                                        shape = RoundedCornerShape(50)
                                    )
                                    .padding(horizontal = 32.dp, vertical = 14.dp),
                            ) {
                                Text(
                                    "Import Document",
                                    color = OnPrimary,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall,
                                )
                            }
                        }
                    }
                }
            }

            // --- Recently Read Section ---
            if (documents.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Recently Read",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = OnSurface,
                        )
                        Text(
                            text = "VIEW ALL →",
                            style = MaterialTheme.typography.labelSmall,
                            color = Primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        items(documents.take(5)) { doc ->
                            RecentlyReadCard(
                                document = doc,
                                progress = progressMap[doc.id],
                                onClick = { onDocumentClick(doc) }
                            )
                        }
                    }
                }
            }

            // --- Your Library ---
            item {
                Spacer(modifier = Modifier.height(32.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Your Library",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = OnSurface,
                    )
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Search, "Search", tint = OnSurfaceVariant)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (documents.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 60.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "📚",
                                fontSize = 48.sp,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No documents yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = OnSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tap the + button to import your first PDF or text file",
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceVariant.copy(alpha = 0.6f),
                            )
                        }
                    }
                }
            }

            items(documents) { doc ->
                LibraryListItem(
                    document = doc,
                    progress = progressMap[doc.id],
                    onClick = { onDocumentClick(doc) },
                    onDelete = { onDeleteDocument(doc) }
                )
            }
        }

        // --- FAB ---
        FloatingActionButton(
            onClick = {
                filePickerLauncher.launch(arrayOf("application/pdf", "text/plain"))
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 32.dp),
            containerColor = Primary,
            contentColor = OnPrimary,
            shape = CircleShape,
        ) {
            Icon(Icons.Default.Add, contentDescription = "Import")
        }
    }
}

// --- Recently Read Glass Card ---
@Composable
fun RecentlyReadCard(
    document: DocumentEntity,
    progress: ProgressEntity?,
    onClick: () -> Unit,
) {
    val progressPercent = if (progress != null && progress.totalChunks > 0) {
        (progress.currentChunkIndex.toFloat() / progress.totalChunks * 100).toInt()
    } else 0

    val fileTag = if (document.title.endsWith(".pdf", ignoreCase = true)) "PDF" else "TXT"
    val tagColor = if (fileTag == "PDF") Primary else Secondary

    Card(
        modifier = Modifier
            .width(220.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceContainerHigh.copy(alpha = 0.5f)
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Book icon placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                SurfaceContainerHighest,
                                SurfaceContainerLow,
                            )
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text("📖", fontSize = 48.sp)

                // Progress circle overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Background.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "${progressPercent}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = Secondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = document.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = OnBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .background(tagColor.copy(alpha = 0.15f), RoundedCornerShape(50))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = fileTag,
                        style = MaterialTheme.typography.labelSmall,
                        color = tagColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                    )
                }
            }
        }
    }
}

// --- Library List Item ---
@Composable
fun LibraryListItem(
    document: DocumentEntity,
    progress: ProgressEntity?,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val fileTag = if (document.title.endsWith(".pdf", ignoreCase = true)) "PDF" else "TXT"
    val tagColor = if (fileTag == "PDF") Primary else Secondary
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val dateStr = dateFormat.format(Date(document.addedTimestamp))
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainerLow)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .size(width = 48.dp, height = 60.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            Text("📄", fontSize = 24.sp)
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = document.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = OnBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .background(tagColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = fileTag,
                        style = MaterialTheme.typography.labelSmall,
                        color = tagColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Added $dateStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant,
                )
            }
        }

        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Default.MoreVert, "Options", tint = OnSurfaceVariant.copy(alpha = 0.5f))
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = {
                        showMenu = false
                        onDelete()
                    }
                )
            }
        }
    }
}
