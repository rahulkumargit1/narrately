package com.example.pdfreader.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pdfreader.security.SecurityManager
import com.example.pdfreader.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    securityManager: SecurityManager,
    onBack: () -> Unit,
) {
    var showPinSetup by remember { mutableStateOf(false) }
    var showPinRemove by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "settingsGlow")
    val glowX by infiniteTransition.animateFloat(0.3f, 0.7f, infiniteRepeatable(tween(6000, easing = EaseInOutSine), RepeatMode.Reverse), label = "gx")

    // PIN Setup Dialog
    if (showPinSetup) {
        PinSetupDialog(
            onSetup = { pin, bio ->
                securityManager.setupLock(pin, bio)
                showPinSetup = false
            },
            onDismiss = { showPinSetup = false },
        )
    }
    if (showPinRemove) {
        AlertDialog(
            onDismissRequest = { showPinRemove = false },
            title = { Text("Remove App Lock", fontWeight = FontWeight.Bold) },
            text = { Text("Your documents will no longer be protected by PIN or fingerprint.") },
            confirmButton = {
                TextButton(
                    onClick = { securityManager.removeLock(); showPinRemove = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = Error),
                ) { Text("Remove", fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = { TextButton(onClick = { showPinRemove = false }) { Text("Cancel") } },
            containerColor = SurfaceContainerHigh,
            titleContentColor = OnBackground,
            textContentColor = OnSurfaceVariant,
            shape = RoundedCornerShape(20.dp),
        )
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Background).drawBehind {
            drawCircle(
                brush = Brush.radialGradient(listOf(PrimaryGlow, Color.Transparent), center = Offset(size.width * glowX, size.height * 0.12f), radius = size.width * 0.45f),
                radius = size.width * 0.45f, center = Offset(size.width * glowX, size.height * 0.12f),
            )
        },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = OnSurfaceVariant)
                }
                Text("Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = OnBackground, modifier = Modifier.padding(start = 4.dp))
            }

            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
                Spacer(modifier = Modifier.height(8.dp))

                // ─── Security Section ───
                SectionLabel("Security")
                GlassSettingsItem(
                    icon = Icons.Default.Fingerprint,
                    title = "App Lock",
                    subtitle = if (securityManager.isAppLockEnabled) "Protected with PIN & Fingerprint" else "Off — Tap to enable",
                    iconTint = if (securityManager.isAppLockEnabled) Primary else OnSurfaceVariant,
                    onClick = {
                        if (securityManager.isAppLockEnabled) showPinRemove = true
                        else showPinSetup = true
                    },
                )

                if (securityManager.isAppLockEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    GlassSettingsItem(
                        icon = Icons.Outlined.Timer,
                        title = "Auto-Lock Timeout",
                        subtitle = "${securityManager.autoLockTimeoutSeconds}s after leaving app",
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                listOf(10, 30, 60).forEach { t ->
                                    val selected = securityManager.autoLockTimeoutSeconds == t
                                    Box(
                                        modifier = Modifier.padding(start = 4.dp).clip(RoundedCornerShape(8.dp))
                                            .clickable { securityManager.autoLockTimeoutSeconds = t }
                                            .background(if (selected) Primary.copy(alpha = 0.15f) else Color.Transparent)
                                            .border(0.5.dp, if (selected) Primary.copy(0.4f) else GlassBorder, RoundedCornerShape(8.dp))
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text("${t}s", style = MaterialTheme.typography.labelSmall, color = if (selected) Primary else OnSurfaceVariant, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                                    }
                                }
                            }
                        },
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ─── Playback Section ───
                SectionLabel("Playback Defaults")
                var speed by remember { mutableStateOf(securityManager.defaultSpeed) }
                GlassSettingsItem(
                    icon = Icons.Outlined.Speed,
                    title = "Default Speed",
                    subtitle = "${speed}x",
                    trailingContent = {
                        Slider(
                            value = speed, onValueChange = { speed = it; securityManager.defaultSpeed = it },
                            valueRange = 0.5f..3f, steps = 9,
                            modifier = Modifier.width(140.dp),
                            colors = SliderDefaults.colors(thumbColor = Primary, activeTrackColor = Primary),
                        )
                    },
                )

                Spacer(modifier = Modifier.height(8.dp))
                var pitch by remember { mutableStateOf(securityManager.defaultPitch) }
                GlassSettingsItem(
                    icon = Icons.Outlined.Tune,
                    title = "Default Pitch",
                    subtitle = if (pitch == 1.0f) "Normal" else "${"%.1f".format(pitch)}",
                    trailingContent = {
                        Slider(
                            value = pitch, onValueChange = { pitch = it; securityManager.defaultPitch = it },
                            valueRange = 0.5f..2f, steps = 5,
                            modifier = Modifier.width(140.dp),
                            colors = SliderDefaults.colors(thumbColor = Secondary, activeTrackColor = Secondary),
                        )
                    },
                )

                Spacer(modifier = Modifier.height(24.dp))

                // ─── Display Section ───
                SectionLabel("Display")
                var fontSize by remember { mutableStateOf(securityManager.fontSize) }
                GlassSettingsItem(
                    icon = Icons.Outlined.FormatSize,
                    title = "Font Size",
                    subtitle = "${"%.0f".format(fontSize)}sp",
                    trailingContent = {
                        Slider(
                            value = fontSize, onValueChange = { fontSize = it; securityManager.fontSize = it },
                            valueRange = 12f..24f, steps = 5,
                            modifier = Modifier.width(140.dp),
                            colors = SliderDefaults.colors(thumbColor = AccentTeal, activeTrackColor = AccentTeal),
                        )
                    },
                )

                Spacer(modifier = Modifier.height(24.dp))

                // ─── About Section ───
                SectionLabel("About")
                GlassSettingsItem(
                    icon = Icons.Outlined.Info,
                    title = "Narrately",
                    subtitle = "Version 4.0 — Built with ❤️",
                )

                Spacer(modifier = Modifier.height(120.dp))
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = Primary, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 10.dp))
}

@Composable
private fun GlassSettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconTint: Color = OnSurfaceVariant,
    trailingContent: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .background(GlassSurface)
            .border(0.5.dp, GlassBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = iconTint, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = OnBackground)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant.copy(alpha = 0.5f))
        }
        trailingContent?.invoke()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PinSetupDialog(
    onSetup: (String, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var step by remember { mutableIntStateOf(1) }
    var useBio by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (step == 1) "Set PIN" else "Confirm PIN", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                if (error != null) {
                    Text(error!!, color = Error, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                OutlinedTextField(
                    value = if (step == 1) pin else confirmPin,
                    onValueChange = { v ->
                        if (v.length <= 6 && v.all { it.isDigit() }) {
                            if (step == 1) pin = v else confirmPin = v
                            error = null
                        }
                    },
                    label = { Text(if (step == 1) "Enter 4-6 digit PIN" else "Re-enter PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary, cursorColor = Primary,
                        focusedTextColor = OnBackground, unfocusedTextColor = OnBackground,
                    ),
                )
                if (step == 1) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = useBio,
                            onCheckedChange = { useBio = it },
                            colors = CheckboxDefaults.colors(checkedColor = Primary),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Also enable fingerprint", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (step == 1) {
                    if (pin.length < 4) { error = "PIN must be at least 4 digits"; return@TextButton }
                    step = 2
                } else {
                    if (confirmPin != pin) { error = "PINs don't match"; confirmPin = ""; return@TextButton }
                    onSetup(pin, useBio)
                }
            }) { Text(if (step == 1) "Next" else "Enable Lock", fontWeight = FontWeight.Bold, color = Primary) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        containerColor = SurfaceContainerHigh,
        titleContentColor = OnBackground,
        textContentColor = OnSurfaceVariant,
        shape = RoundedCornerShape(20.dp),
    )
}
