package com.example.pdfreader.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pdfreader.data.DailyStatRow
import com.example.pdfreader.ui.theme.*

@Composable
fun ListeningStatsScreen(
    totalSeconds: Long,
    listeningDays: Int,
    completedDocs: Int,
    weeklyStats: List<DailyStatRow>,
    onBack: () -> Unit,
) {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60

    val infiniteTransition = rememberInfiniteTransition(label = "statsGlow")
    val glowX by infiniteTransition.animateFloat(0.2f, 0.8f, infiniteRepeatable(tween(7000, easing = EaseInOutSine), RepeatMode.Reverse), label = "gx")

    Box(
        modifier = Modifier.fillMaxSize().background(Background).drawBehind {
            drawCircle(
                brush = Brush.radialGradient(listOf(AccentTeal.copy(0.12f), Color.Transparent), center = Offset(size.width * glowX, size.height * 0.15f), radius = size.width * 0.5f),
                radius = size.width * 0.5f, center = Offset(size.width * glowX, size.height * 0.15f),
            )
        },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            Row(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = OnSurfaceVariant) }
                Text("Listening Stats", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = OnBackground, modifier = Modifier.padding(start = 4.dp))
            }

            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
                Spacer(Modifier.height(8.dp))

                // ─── Hero stat ───
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                        .background(Brush.verticalGradient(listOf(AccentTeal.copy(0.12f), Color.Transparent)))
                        .border(0.5.dp, AccentTeal.copy(0.2f), RoundedCornerShape(20.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Total Listening", style = MaterialTheme.typography.labelMedium, color = AccentTeal.copy(0.7f), letterSpacing = 1.sp)
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            if (hours > 0) {
                                Text("$hours", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, color = OnBackground)
                                Text("h ", style = MaterialTheme.typography.headlineSmall, color = OnSurfaceVariant.copy(0.5f), modifier = Modifier.padding(bottom = 8.dp))
                            }
                            Text("$minutes", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, color = OnBackground)
                            Text("m", style = MaterialTheme.typography.headlineSmall, color = OnSurfaceVariant.copy(0.5f), modifier = Modifier.padding(bottom = 8.dp))
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ─── Stat cards row ───
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(Modifier.weight(1f), Icons.Default.CalendarMonth, "$listeningDays", "Active Days", AccentPurple)
                    StatCard(Modifier.weight(1f), Icons.Default.CheckCircle, "$completedDocs", "Completed", Primary)
                }

                Spacer(Modifier.height(24.dp))

                // ─── Weekly chart ───
                Text("This Week", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = OnBackground)
                Spacer(Modifier.height(12.dp))

                if (weeklyStats.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(16.dp))
                            .background(GlassSurface).border(0.5.dp, GlassBorder, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Start listening to see your weekly chart!", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant.copy(0.4f))
                    }
                } else {
                    // Simple bar chart
                    val maxSeconds = weeklyStats.maxOfOrNull { it.total } ?: 1L
                    Row(
                        modifier = Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(16.dp))
                            .background(GlassSurface).border(0.5.dp, GlassBorder, RoundedCornerShape(16.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        weeklyStats.reversed().forEach { stat ->
                            val fraction = (stat.total.toFloat() / maxSeconds).coerceIn(0.05f, 1f)
                            val dayLabel = stat.dateKey.takeLast(2)  // Day number
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${stat.total / 60}m", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant.copy(0.5f), fontSize = 9.sp)
                                Spacer(Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier.width(24.dp)
                                        .fillMaxHeight(fraction * 0.7f)
                                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                        .background(Brush.verticalGradient(listOf(AccentTeal, AccentTeal.copy(0.3f))))
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(dayLabel, style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant.copy(0.4f), fontSize = 10.sp)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(120.dp))
            }
        }
    }
}

@Composable
private fun StatCard(modifier: Modifier, icon: ImageVector, value: String, label: String, accent: Color) {
    Box(
        modifier = modifier.clip(RoundedCornerShape(16.dp))
            .background(GlassSurface).border(0.5.dp, GlassBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Column {
            Icon(icon, null, tint = accent.copy(0.6f), modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(10.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = OnBackground)
            Text(label, style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant.copy(0.5f))
        }
    }
}
