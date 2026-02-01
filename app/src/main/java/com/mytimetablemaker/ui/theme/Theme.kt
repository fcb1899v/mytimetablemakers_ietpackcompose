package com.mytimetablemaker.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// MARK: - Material Theme Color Scheme
// Theme colors (primary and accent)
// Primary: #3700B3 (Indigo)
// Accent: #03DAC5 (Cyan)

private val LightColorScheme = lightColorScheme(
    primary = Primary,      // #3700B3
    secondary = Accent,     // #03DAC5
    tertiary = Accent       // #03DAC5
)

private val DarkColorScheme = darkColorScheme(
    primary = Primary,      // #3700B3
    secondary = Accent,     // #03DAC5
    tertiary = Accent       // #03DAC5
)

@Composable
fun MyTransitMakers_JetpackComposeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}