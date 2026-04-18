package com.example.pdfreader.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pdfreader.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    // Staggered fade-in for each element
    var showTitle by remember { mutableStateOf(false) }
    var showSubtitle by remember { mutableStateOf(false) }
    var showCredit by remember { mutableStateOf(false) }

    val titleAlpha by animateFloatAsState(
        targetValue = if (showTitle) 1f else 0f,
        animationSpec = tween(1200, easing = EaseOutCubic),
        label = "titleAlpha",
    )
    val subtitleAlpha by animateFloatAsState(
        targetValue = if (showSubtitle) 1f else 0f,
        animationSpec = tween(1000, easing = EaseOutCubic),
        label = "subtitleAlpha",
    )
    val creditAlpha by animateFloatAsState(
        targetValue = if (showCredit) 1f else 0f,
        animationSpec = tween(800, easing = EaseOutCubic),
        label = "creditAlpha",
    )

    // Subtle breathing line
    val infiniteTransition = rememberInfiniteTransition(label = "breathe")
    val lineWidth by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "lineWidth",
    )

    LaunchedEffect(Unit) {
        delay(300)
        showTitle = true
        delay(600)
        showSubtitle = true
        delay(400)
        showCredit = true
        delay(1800)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 48.dp),
        ) {
            // App name — clean, no glow, just weight and spacing
            Text(
                text = "NARRATELY",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 8.sp,
                    fontSize = 40.sp,
                ),
                color = OnBackground.copy(alpha = titleAlpha),
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Breathing accent line
            Box(
                modifier = Modifier
                    .fillMaxWidth(lineWidth)
                    .height(2.dp)
                    .alpha(titleAlpha)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Primary.copy(alpha = 0.8f),
                                Color.Transparent,
                            )
                        )
                    )
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Tagline
            Text(
                text = "Your documents, narrated.",
                style = MaterialTheme.typography.bodyLarge,
                color = OnSurfaceVariant.copy(alpha = subtitleAlpha * 0.7f),
                textAlign = TextAlign.Center,
            )
        }

        // Credit at bottom
        Text(
            text = "Created by Rahul",
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 2.sp,
            ),
            color = OnSurfaceVariant.copy(alpha = creditAlpha * 0.4f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp),
        )
    }
}
