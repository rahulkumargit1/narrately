package com.example.pdfreader.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    isLoading: Boolean,
    errorMessage: String?,
    onImportDocument: (Uri) -> Unit,
    onDocumentClick: (DocumentEntity) -> Unit,
    onDeleteDocument: (DocumentEntity) -> Unit,
    onClearError: () -> Unit,
) {
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { onImportDocument(it) }
    }

    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when {
            hour < 12 -> "Good Morning"
            hour < 17 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }

    // Snackbar for errors
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            onClearError()
        }
    }

    // Delete confirmation dialog
    var documentToDelete by remember { mutableStateOf<DocumentEntity?>(null) }
    if (documentToDelete != null) {
        AlertDialog(
            onDismissRequest = { documentToDelete = null },
            title = {
                Text("Delete Document", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Remove \"${documentToDelete!!.title}\" from your library? This cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        documentToDelete?.let { onDeleteDocument(it) }
                        documentToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Error),
                ) {
                    Text("Delete", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { documentToDelete = null }) {
                    Text("Cancel")
                }
            },
            containerColor = SurfaceContainer,
            titleContentColor = OnBackground,
            textContentColor = OnSurfaceVariant,
        )
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = SurfaceContainerHigh,
                    contentColor = OnBackground,
                    actionColor = Primary,
                )
            }
        },
        containerColor = Background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { filePickerLauncher.launch(arrayOf("application/pdf", "text/plain")) },
                containerColor = Primary,
                contentColor = OnPrimary,
                shape = CircleShape,
                modifier = Modifier.padding(bottom = 8.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = "Import document")
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 96.dp),
            ) {
                // ─── Header ───
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 8.dp)
                    ) {
                        Text(
                            text = greeting,
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Listen Now",
                            style = MaterialTheme.typography.headlineLarge,
                            color = OnBackground,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                // ─── Recently Read (horizontal scroll) ───
                if (documents.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Continue Listening",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = OnBackground,
                            modifier = Modifier.padding(horizontal = 24.dp),
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            items(documents.take(6)) { doc ->
                                RecentlyReadCard(
                                    document = doc,
                                    progress = progressMap[doc.id],
                                    onClick = { onDocumentClick(doc) },
                                )
                            }
                        }
                    }
                }

                // ─── Your Library list ───
                item {
                    Spacer(modifier = Modifier.height(28.dp))
                    Text(
                        text = "Your Library",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OnBackground,
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (documents.isEmpty() && !isLoading) {
                    item { EmptyLibraryState() }
                }

                items(documents) { doc ->
                    LibraryListItem(
                        document = doc,
                        progress = progressMap[doc.id],
                        onClick = { onDocumentClick(doc) },
                        onDelete = { documentToDelete = doc },
                    )
                }
            }

            // Loading overlay
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Background.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        color = Primary,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(40.dp),
                    )
                }
            }
        }
    }
}

// ─── Empty State ───
@Composable
private fun EmptyLibraryState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("📖", fontSize = 56.sp)
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Your library is empty",
            style = MaterialTheme.typography.titleMedium,
            color = OnSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Import a PDF or text file to start listening",
            style = MaterialTheme.typography.bodySmall,
            color = OnSurfaceVariant.copy(alpha = 0.6f),
        )
    }
}

// ─── Recently Read Card ───
@Composable
private fun RecentlyReadCard(
    document: DocumentEntity,
    progress: ProgressEntity?,
    onClick: () -> Unit,
) {
    val progressPercent = if (progress != null && progress.totalChunks > 0) {
        (progress.currentChunkIndex.toFloat() / progress.totalChunks * 100).toInt()
    } else 0

    val isPdf = document.title.endsWith(".pdf", ignoreCase = true)
    // Each file type gets a distinct gradient so cards look different
    val gradientColors = if (isPdf) {
        listOf(Color(0xFF2C1810), Color(0xFF1A0F0A))
    } else {
        listOf(Color(0xFF0F1A2C), Color(0xFF0A0F1A))
    }

    Card(
        modifier = Modifier
            .width(180.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Column {
            // Cover art area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(Brush.verticalGradient(gradientColors), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (isPdf) "PDF" else "TXT",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isPdf) Primary.copy(alpha = 0.4f) else Secondary.copy(alpha = 0.4f),
                )

                // Progress pill
                if (progressPercent > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(10.dp)
                            .background(
                                Background.copy(alpha = 0.75f),
                                RoundedCornerShape(50),
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        Text(
                            text = "${progressPercent}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = OnBackground,
                            fontSize = 10.sp,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = document.title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = OnBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}

// ─── Library List Item ───
@Composable
private fun LibraryListItem(
    document: DocumentEntity,
    progress: ProgressEntity?,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val isPdf = document.title.endsWith(".pdf", ignoreCase = true)
    val tagText = if (isPdf) "PDF" else "TXT"
    val tagColor = if (isPdf) Primary else Secondary
    val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
    val dateStr = dateFormat.format(Date(document.addedTimestamp))
    var showMenu by remember { mutableStateOf(false) }

    val progressPercent = if (progress != null && progress.totalChunks > 0) {
        (progress.currentChunkIndex.toFloat() / progress.totalChunks * 100).toInt()
    } else 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .background(SurfaceContainerLow)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (isPdf) Color(0xFF2C1810) else Color(0xFF0F1A2C)
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = tagText,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = tagColor.copy(alpha = 0.7f),
                fontSize = 11.sp,
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = document.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = OnBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant.copy(alpha = 0.6f),
                )
                if (progressPercent > 0) {
                    Text(
                        text = "  ·  ${progressPercent}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = Primary.copy(alpha = 0.7f),
                    )
                }
            }
        }

        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    Icons.Default.MoreVert,
                    "Options",
                    tint = OnSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp),
                )
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
            ) {
                DropdownMenuItem(
                    text = { Text("Delete", color = Error) },
                    onClick = {
                        showMenu = false
                        onDelete()
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Delete, null, tint = Error, modifier = Modifier.size(18.dp))
                    },
                )
            }
        }
    }
}
