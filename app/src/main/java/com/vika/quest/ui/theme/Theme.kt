package com.vika.quest.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF3E56B6),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDE1FF),
    onPrimaryContainer = Color(0xFF001452),
    background = Color(0xFFFAF8FF),
    surface = Color(0xFFFAF8FF),
    surfaceVariant = Color(0xFFE3E2EC),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB8C3FF),
    onPrimary = Color(0xFF09266F),
    primaryContainer = Color(0xFF253D9C),
    onPrimaryContainer = Color(0xFFDDE1FF),
    background = Color(0xFF121318),
    surface = Color(0xFF121318),
    surfaceVariant = Color(0xFF45464F),
)

@Composable
fun QuestTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
