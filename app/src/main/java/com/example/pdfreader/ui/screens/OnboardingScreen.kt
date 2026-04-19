package com.example.pdfreader.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pdfreader.ui.theme.*
import kotlinx.coroutines.launch

data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val accent: Color,
)

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val pages = listOf(
        OnboardingPage(Icons.Default.AutoStories, "Import Anything", "Drop in any PDF or text file\nand Narrately brings it to life", Primary),
        OnboardingPage(Icons.Default.Headphones, "Listen Naturally", "Human-like neural voices\nread your documents like a podcast", Secondary),
        OnboardingPage(Icons.Default.Fingerprint, "Private & Secure", "Fingerprint lock, encrypted storage\nyour documents stay yours", AccentTeal),
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    val infiniteTransition = rememberInfiniteTransition(label = "onboardGlow")
    val glowPhase by infiniteTransition.animateFloat(0f, 1f, infiniteRepeatable(tween(5000, easing = EaseInOutSine), RepeatMode.Reverse), label = "gp")

    Box(
        modifier = Modifier.fillMaxSize().background(Background).drawBehind {
            val page = pages.getOrNull(pagerState.currentPage) ?: pages[0]
            drawCircle(
                brush = Brush.radialGradient(listOf(page.accent.copy(0.15f), Color.Transparent), center = Offset(size.width * (0.3f + glowPhase * 0.4f), size.height * 0.35f), radius = size.width * 0.5f),
                radius = size.width * 0.5f, center = Offset(size.width * (0.3f + glowPhase * 0.4f), size.height * 0.35f),
            )
        },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Skip
            Row(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onFinished) { Text("Skip", color = OnSurfaceVariant.copy(0.5f)) }
            }

            // Pager
            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
                OnboardingPageContent(pages[page])
            }

            // Dots
            Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.Center) {
                repeat(pages.size) { i ->
                    val active = i == pagerState.currentPage
                    val dotScale by animateFloatAsState(if (active) 1f else 0.6f, spring(dampingRatio = 0.6f), label = "ds$i")
                    Box(
                        Modifier.padding(horizontal = 4.dp).size(8.dp).scale(dotScale).clip(CircleShape)
                            .background(if (active) pages[i].accent else GlassSurface)
                            .border(0.5.dp, if (active) pages[i].accent.copy(0.5f) else GlassBorder, CircleShape)
                    )
                }
            }

            // Button
            Button(
                onClick = {
                    if (pagerState.currentPage < pages.size - 1) {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    } else {
                        onFinished()
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 48.dp).padding(bottom = 48.dp).height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = pages[pagerState.currentPage].accent, contentColor = Color.White),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(
                    if (pagerState.currentPage < pages.size - 1) "Continue" else "Get Started",
                    fontWeight = FontWeight.Bold, fontSize = 16.sp,
                )
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    val infiniteTransition = rememberInfiniteTransition(label = "iconFloat")
    val floatY by infiniteTransition.animateFloat(0f, 12f, infiniteRepeatable(tween(2000, easing = EaseInOutSine), RepeatMode.Reverse), label = "fy")

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Floating icon with glow ring
        Box(contentAlignment = Alignment.Center, modifier = Modifier.offset(y = floatY.dp)) {
            // Glow ring
            Box(
                Modifier.size(120.dp).clip(CircleShape)
                    .background(page.accent.copy(alpha = 0.08f))
                    .border(1.dp, page.accent.copy(0.15f), CircleShape)
            )
            Icon(page.icon, null, tint = page.accent, modifier = Modifier.size(52.dp))
        }

        Spacer(Modifier.height(48.dp))

        Text(page.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = OnBackground, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(page.subtitle, style = MaterialTheme.typography.bodyLarge, color = OnSurfaceVariant.copy(0.6f), textAlign = TextAlign.Center, lineHeight = 24.sp)
    }
}
