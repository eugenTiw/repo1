package com.eugentiw.fernziel.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Ruhige, bewusst kleine Farbpalette: neutrale Flächen und genau eine Akzentfarbe. */
@Immutable
data class AppColors(
    val background: Color,
    val surface: Color,
    val fill: Color,
    val fillSelected: Color,
    val text: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val separator: Color,
    val accent: Color,
    val accentSoft: Color,
    val onAccent: Color,
    val danger: Color,
)

private val LightColors = AppColors(
    background = Color(0xFFF4F4F1),
    surface = Color(0xFFFFFFFF),
    fill = Color(0xFFEDEDE9),
    fillSelected = Color(0xFFFFFFFF),
    text = Color(0xFF1B1B1A),
    textSecondary = Color(0xFF6E6E6A),
    textTertiary = Color(0xFFB4B4AF),
    separator = Color(0xFFE9E9E5),
    accent = Color(0xFF2E6B5E),
    accentSoft = Color(0xFFE3EEEA),
    onAccent = Color(0xFFFFFFFF),
    danger = Color(0xFFC0392B),
)

private val DarkColors = AppColors(
    background = Color(0xFF0F0F10),
    surface = Color(0xFF1B1B1D),
    fill = Color(0xFF2A2A2D),
    fillSelected = Color(0xFF3D3D41),
    text = Color(0xFFF2F2EF),
    textSecondary = Color(0xFF9A9A96),
    textTertiary = Color(0xFF5E5E5B),
    separator = Color(0xFF2A2A2C),
    accent = Color(0xFF7CC7B3),
    accentSoft = Color(0xFF1F3530),
    onAccent = Color(0xFF0F1A17),
    danger = Color(0xFFFF6B5E),
)

val LocalAppColors = staticCompositionLocalOf { LightColors }

/** Wenige, klar abgestufte Schriftgrößen in der Systemschrift. */
object AppType {
    val largeTitle = TextStyle(fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp)
    val headline = TextStyle(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold)
    val body = TextStyle(fontSize = 17.sp, lineHeight = 22.sp)
    val subhead = TextStyle(fontSize = 15.sp, lineHeight = 20.sp)
    val caption = TextStyle(fontSize = 13.sp, lineHeight = 18.sp)
    val overline = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.8.sp)
    val tab = TextStyle(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium)
}

@Composable
fun FernzielTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val c = if (dark) DarkColors else LightColors

    // Das Material-Farbschema wird nur für Systemdialoge (Datumsauswahl, Rückfragen) gebraucht.
    val scheme = if (dark) {
        darkColorScheme(
            primary = c.accent,
            onPrimary = c.onAccent,
            primaryContainer = c.accentSoft,
            onPrimaryContainer = c.text,
            secondary = c.accent,
            onSecondary = c.onAccent,
            secondaryContainer = c.accentSoft,
            onSecondaryContainer = c.text,
            tertiary = c.accent,
            onTertiary = c.onAccent,
            background = c.background,
            onBackground = c.text,
            surface = c.surface,
            onSurface = c.text,
            surfaceVariant = c.fill,
            onSurfaceVariant = c.textSecondary,
            surfaceTint = c.surface,
            outline = c.textTertiary,
            outlineVariant = c.separator,
            error = c.danger,
            surfaceContainerLowest = c.background,
            surfaceContainerLow = c.surface,
            surfaceContainer = c.surface,
            surfaceContainerHigh = c.surface,
            surfaceContainerHighest = c.fill,
        )
    } else {
        lightColorScheme(
            primary = c.accent,
            onPrimary = c.onAccent,
            primaryContainer = c.accentSoft,
            onPrimaryContainer = c.text,
            secondary = c.accent,
            onSecondary = c.onAccent,
            secondaryContainer = c.accentSoft,
            onSecondaryContainer = c.text,
            tertiary = c.accent,
            onTertiary = c.onAccent,
            background = c.background,
            onBackground = c.text,
            surface = c.surface,
            onSurface = c.text,
            surfaceVariant = c.fill,
            onSurfaceVariant = c.textSecondary,
            surfaceTint = c.surface,
            outline = c.textTertiary,
            outlineVariant = c.separator,
            error = c.danger,
            surfaceContainerLowest = c.surface,
            surfaceContainerLow = c.surface,
            surfaceContainer = c.surface,
            surfaceContainerHigh = c.surface,
            surfaceContainerHighest = c.fill,
        )
    }

    MaterialTheme(colorScheme = scheme) {
        CompositionLocalProvider(LocalAppColors provides c) {
            content()
        }
    }
}
