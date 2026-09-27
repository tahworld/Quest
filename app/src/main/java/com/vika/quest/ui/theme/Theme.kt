package com.vika.quest.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Color(0xFF356B63),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCF3E9),
    onPrimaryContainer = Color(0xFF123E37),
    secondary = Color(0xFF936451),
    background = Color(0xFFFFFAF5),
    surface = Color(0xFFFFFAF5),
    surfaceVariant = Color(0xFFF3EBE4),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA6D9C7),
    onPrimary = Color(0xFF113C34),
    primaryContainer = Color(0xFF245449),
    onPrimaryContainer = Color(0xFFE2F5EB),
    secondary = Color(0xFFE7B59E),
    background = Color(0xFF151B1A),
    surface = Color(0xFF151B1A),
    surfaceVariant = Color(0xFF34413C),
)

private val SoftShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp),
)

@Composable
fun QuestTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        shapes = SoftShapes,
        content = content,
    )
}
