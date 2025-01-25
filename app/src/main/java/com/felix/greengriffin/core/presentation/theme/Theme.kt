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
    error = Pink40

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
    error = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
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