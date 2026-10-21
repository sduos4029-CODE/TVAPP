package com.flowtv.launcher.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.darkColorScheme

val Navy = Color(0xFF0A0E1A)
val Slate = Color(0xFF141C2E)
val Mint = Color(0xFF3EE6B0)
val Lavender = Color(0xFFB497FF)
val Muted = Color(0xFF94A3B8)

@Composable
fun FlowTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(primary = Mint, onPrimary = Navy,
        background = Navy, onBackground = Color(0xFFF4F7FF), surface = Slate,
        onSurface = Color(0xFFF4F7FF))) {
        CompositionLocalProvider(LocalContentColor provides Color(0xFFF4F7FF), content = content)
    }
}
