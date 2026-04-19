package com.example.pdfreader.ui.screens

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.pdfreader.security.SecurityManager
import com.example.pdfreader.ui.theme.*

@Composable
fun LockScreen(
    securityManager: SecurityManager,
    onUnlocked: () -> Unit,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var enteredPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    val maxPinLength = securityManager.pinCode?.length ?: 4

    // Check if biometric is available
    val biometricAvailable = remember {
        val bm = BiometricManager.from(context)
        bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS
    }

    // Auto-trigger biometric on launch
    LaunchedEffect(Unit) {
        if (biometricAvailable && securityManager.useBiometric) {
            showBiometricPrompt(context, onUnlocked)
        }
    }

    // Shake animation on wrong PIN
    val shakeOffset by animateFloatAsState(
        targetValue = if (error) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.3f, stiffness = 2000f),
        label = "shake",
        finishedListener = { error = false },
    )

    // Ambient glow
    val infiniteTransition = rememberInfiniteTransition(label = "lockGlow")
    val glowY by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 0.4f,
        animationSpec = infiniteRepeatable(tween(5000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "glowY",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(listOf(PrimaryGlow, Color.Transparent), center = Offset(size.width * 0.5f, size.height * glowY), radius = size.width * 0.4f),
                    radius = size.width * 0.4f,
                    center = Offset(size.width * 0.5f, size.height * glowY),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.weight(0.3f))

            // Lock icon
            Icon(Icons.Default.Fingerprint, null, tint = Primary, modifier = Modifier.size(56.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Narrately", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = OnBackground, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text("Enter PIN to unlock", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant.copy(alpha = 0.5f))

            Spacer(modifier = Modifier.height(36.dp))

            // PIN dots
            Row(
                modifier = Modifier.offset(x = (shakeOffset * 10).dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                repeat(maxPinLength) { i ->
                    val filled = i < enteredPin.length
                    val dotScale by animateFloatAsState(
                        targetValue = if (filled) 1.2f else 1f,
                        animationSpec = spring(dampingRatio = 0.5f),
                        label = "dot$i",
                    )
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .scale(dotScale)
                            .clip(CircleShape)
                            .background(if (filled) Primary else GlassSurface)
                            .border(1.dp, if (filled) Primary else GlassBorder, CircleShape),
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Numpad
            val buttons = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf(if (biometricAvailable) "bio" else "", "0", "del"),
            )

            for (row in buttons) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    for (key in row) {
                        if (key.isEmpty()) {
                            Spacer(modifier = Modifier.size(72.dp))
                        } else {
                            NumPadKey(
                                key = key,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    when (key) {
                                        "del" -> {
                                            if (enteredPin.isNotEmpty()) enteredPin = enteredPin.dropLast(1)
                                        }
                                        "bio" -> {
                                            showBiometricPrompt(context, onUnlocked)
                                        }
                                        else -> {
                                            enteredPin += key
                                            if (enteredPin.length == maxPinLength) {
                                                if (securityManager.verifyPin(enteredPin)) {
                                                    onUnlocked()
                                                } else {
                                                    error = true
                                                    enteredPin = ""
                                                }
                                            }
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Spacer(modifier = Modifier.weight(0.4f))
        }
    }
}

@Composable
private fun NumPadKey(key: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .clickable { onClick() }
            .background(GlassSurface)
            .border(0.5.dp, GlassBorder, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        when (key) {
            "del" -> Icon(Icons.Default.Backspace, "Delete", tint = OnSurfaceVariant, modifier = Modifier.size(22.dp))
            "bio" -> Icon(Icons.Default.Fingerprint, "Fingerprint", tint = Primary, modifier = Modifier.size(28.dp))
            else -> Text(key, fontSize = 24.sp, color = OnBackground, fontWeight = FontWeight.Medium)
        }
    }
}

private fun showBiometricPrompt(context: android.content.Context, onSuccess: () -> Unit) {
    val activity = context as? FragmentActivity ?: return
    val executor = ContextCompat.getMainExecutor(context)

    val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            onSuccess()
        }
    })

    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Unlock Narrately")
        .setSubtitle("Verify your identity")
        .setNegativeButtonText("Use PIN")
        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
        .build()

    prompt.authenticate(info)
}
