package com.appspy.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary          = Primary,
    onPrimary        = androidx.compose.ui.graphics.Color.White,
    primaryContainer = PrimarySoft,
    secondary        = Coral,
    onSecondary      = androidx.compose.ui.graphics.Color.White,
    background       = Background,
    surface          = Surface,
    onBackground     = Ink,
    onSurface        = Ink,
    outline          = Divider
)

private val DarkColors = darkColorScheme(
    primary          = Primary,
    onPrimary        = androidx.compose.ui.graphics.Color.White,
    primaryContainer = Primary.copy(alpha = 0.2f),
    secondary        = Coral,
    onSecondary      = androidx.compose.ui.graphics.Color.White,
    background       = androidx.compose.ui.graphics.Color(0xFF12131C),
    surface          = androidx.compose.ui.graphics.Color(0xFF1E1F2E),
    onBackground     = androidx.compose.ui.graphics.Color.White,
    onSurface        = androidx.compose.ui.graphics.Color.White,
    outline          = androidx.compose.ui.graphics.Color(0xFF2E2F3E)
)

@Composable
fun AppUsageTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography  = AppTypography,
        content     = content
    )
}
