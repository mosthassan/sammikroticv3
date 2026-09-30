package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = MikroTikCyan,
    onPrimary = CyberDarkCanvas,
    primaryContainer = CyberDarkCardElevated,
    onPrimaryContainer = Color.White,
    secondary = MikroTikTeal,
    onSecondary = CyberDarkCanvas,
    tertiary = StatusOnline,
    background = CyberDarkCanvas,
    surface = CyberDarkSurface,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = CyberDarkCardElevated,
    onSurfaceVariant = TextSecondaryDark,
    outline = CyberBorder,
    outlineVariant = CyberBorderGlow
)

private val LightColorScheme = lightColorScheme(
    primary = MikroTikPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD0E8FF),
    onPrimaryContainer = MikroTikNavy,
    secondary = Color(0xFF0096C7),
    onSecondary = Color.White,
    tertiary = ReceiptGreen,
    background = MikroTikLightBg,
    surface = MikroTikLightSurface,
    onBackground = Color(0xFF1E293B),
    onSurface = Color(0xFF1E293B),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Network utilities like MikroTik Winbox/UniFi are dedicated dark UI for optimal visibility and battery efficiency
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    // We enforce the sleek Cyber Dark ColorScheme across the app for complete visual harmony
    val colorScheme = DarkColorScheme

    MaterialTheme(colorScheme = colorScheme, typography = AppTypography, content = content)
}
