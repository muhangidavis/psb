package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PsbDarkColorScheme = darkColorScheme(
    primary = PsbOrange,
    onPrimary = Color(0xFF1A0D00),
    primaryContainer = Color(0xFF472200),
    onPrimaryContainer = Color(0xFFFFDBC8),
    secondary = PsbSteel,
    onSecondary = Color(0xFF101419),
    secondaryContainer = PsbSurfaceElevated,
    onSecondaryContainer = PsbSteelLight,
    tertiary = PsbOrangeLight,
    onTertiary = Color(0xFF2B1200),
    background = PsbBgDark,
    onBackground = PsbSteelLight,
    surface = PsbSurface,
    onSurface = PsbSteelLight,
    surfaceVariant = PsbSurfaceCard,
    onSurfaceVariant = PsbSteelMuted,
    outline = PsbBorder,
    outlineVariant = PsbSteelDark,
    error = PsbError,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = PsbDarkColorScheme,
        typography = Typography,
        content = content
    )
}
