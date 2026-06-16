package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = BentoPurpleDark,
    onPrimary = BentoBgDark,
    primaryContainer = BentoPurpleContainerDark,
    onPrimaryContainer = BentoOnPurpleContainerDark,
    secondary = BentoSecondaryContainerDark,
    onSecondary = BentoOnSecondaryContainerDark,
    tertiary = BentoTertiaryContainerDark,
    onTertiary = BentoOnTertiaryContainerDark,
    background = BentoBgDark,
    onBackground = BentoTextDark,
    surface = BentoTertiaryContainerDark,
    onSurface = BentoTextDark,
    surfaceVariant = BentoSecondaryContainerDark,
    onSurfaceVariant = BentoTextSecondaryDark,
    outline = BentoBorderDark
)

private val LightColorScheme = lightColorScheme(
    primary = BentoPurple,
    onPrimary = Color.White,
    primaryContainer = BentoPurpleContainer,
    onPrimaryContainer = BentoOnPurpleContainer,
    secondary = BentoSecondaryContainer,
    onSecondary = BentoOnSecondaryContainer,
    tertiary = BentoTertiaryContainer,
    onTertiary = BentoOnTertiaryContainer,
    background = BentoBgLight,
    onBackground = BentoTextLight,
    surface = Color.White,
    onSurface = BentoTextLight,
    surfaceVariant = BentoTertiaryContainer,
    onSurfaceVariant = BentoTextSecondaryLight,
    outline = BentoBorderLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // We enforce our premium handcrafted palette for a completely custom visual experience
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
