package com.xemniz.langcoach.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Ink = Color(0xFF17211B)
val Forest = Color(0xFF185C3D)
val ForestDark = Color(0xFF0C3F29)
val Mint = Color(0xFFD9F2E3)
val Cream = Color(0xFFF8F6EF)
val WarmWhite = Color(0xFFFFFCF5)
val Gold = Color(0xFFF2B84B)
val MutedInk = Color(0xFF536159)
val Outline = Color(0xFFD5DDD7)

private val LangCoachColors = lightColorScheme(
    primary = Forest,
    onPrimary = Color.White,
    primaryContainer = Mint,
    onPrimaryContainer = ForestDark,
    secondary = Gold,
    onSecondary = Ink,
    background = Cream,
    onBackground = Ink,
    surface = WarmWhite,
    onSurface = Ink,
    surfaceVariant = Color(0xFFEAF0EB),
    onSurfaceVariant = MutedInk,
    outline = Outline,
)

@Composable
fun LangCoachTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LangCoachColors,
        content = content,
    )
}
