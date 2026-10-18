package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val IosCameraColorScheme = darkColorScheme(
    primary = IosCameraYellow,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF2C2C2E),
    onPrimaryContainer = IosCameraYellow,
    secondary = Color.White,
    onSecondary = Color.Black,
    background = IosCameraBlack,
    onBackground = Color.White,
    surface = IosCameraDarkSurface,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF1C1C1E),
    onSurfaceVariant = IosCameraGray
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = IosCameraColorScheme,
        typography = Typography,
        content = content
    )
}
