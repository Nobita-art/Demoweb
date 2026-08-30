package com.flexboard.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF8C00),
    onPrimary = Color.Black,
    background = Color(0xFF0D0D0D),
    onBackground = Color.White,
    surface = Color(0xFF1F1F1F),
    onSurface = Color.White,
    secondary = Color(0xFFFFB347)
)

@Composable
fun FlexboardTheme(content: @Composable () -> Unit) {
    val colors = DarkColors
    MaterialTheme(colorScheme = colors, content = content)
}
