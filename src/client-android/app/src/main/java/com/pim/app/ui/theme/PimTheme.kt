package com.pim.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PimLightColors = lightColorScheme(
    primary = Color(0xFF1D63D8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE8FF),
    onPrimaryContainer = Color(0xFF09306F),
    secondary = Color(0xFF00897B),
    onSecondary = Color.White,
    tertiary = Color(0xFFFFB300),
    error = Color(0xFFC62828),
    background = Color(0xFFF7F9FC),
    surface = Color.White,
    surfaceVariant = Color(0xFFE8EEF6),
    outlineVariant = Color(0xFFD4DCE8)
)

@Composable
fun PimTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PimLightColors,
        typography = MaterialTheme.typography,
        content = content
    )
}
