package org.freedomsuite.core.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FreedomGreen = Color(0xFF2E7D52)
private val FreedomGreenDark = Color(0xFF1B5E3A)

private val LightColors = lightColorScheme(
    primary = FreedomGreen,
    onPrimary = Color.White,
    secondary = FreedomGreenDark,
    surface = Color(0xFFFAFAFA),
    background = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF6BCF8E),
    onPrimary = Color(0xFF0D2818),
    secondary = FreedomGreen,
    surface = Color(0xFF121212),
    background = Color(0xFF0A0A0A),
)

@Composable
fun FreedomTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
