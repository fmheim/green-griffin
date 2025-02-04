package com.felix.greengriffin.core.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = ScrabbleStoneBorder,
    secondary = ScrabbleStoneBackground,
    tertiary = ScrabbleStoneText,
    surface = ScrabbleBoardGreen,
    onSurface = ScrabbleBoardGridGreen,
    onSurfaceVariant = ScrabbleBoardBorderSuccess,
    background = ScrabbleBackground,
    surfaceVariant = ScrabbleStoneBackground,
    error = Pink40,

    onPrimary = ScrabbleStoneText,
    onSecondary = ScrabbleStoneText,
    onBackground = ScrabbleStoneText,
    outline = ScrabbleStoneBorder,
    outlineVariant = ScrabbleBoardGridGreen,
    primaryContainer = ScrabbleStoneBorder,
    onPrimaryContainer = ScrabbleStoneText,
)

private val LightColorScheme = lightColorScheme(
    primary = ScrabbleStoneBorder,
    secondary = ScrabbleStoneBackground,
    tertiary = ScrabbleStoneText,
    surface = ScrabbleBoardGreen,
    onSurface = ScrabbleBoardGridGreen,
    onSurfaceVariant = ScrabbleBoardBorderSuccess,
    background = ScrabbleBackground,
    surfaceVariant = ScrabbleStoneBackground,
    error = Pink40,

    onPrimary = ScrabbleStoneText,
    onSecondary = ScrabbleStoneText,
    onBackground = ScrabbleStoneText,
    outline = ScrabbleStoneBorder,
    outlineVariant = ScrabbleBoardGridGreen,
    primaryContainer = ScrabbleStoneBorder,
    onPrimaryContainer = ScrabbleStoneText,


    )

@Composable
fun GreenGriffinTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}