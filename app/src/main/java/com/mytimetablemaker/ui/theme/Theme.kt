package com.mytimetablemaker.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// Material3 color schemes: Primary #3700B3, Accent #03DAC5

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    secondary = Accent,
    tertiary = Accent
)

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    secondary = Accent,
    tertiary = Accent
)

// Apply Material3 theme with dynamic color support (Android 12+)
@Composable
fun MyTransitMakers_JetpackComposeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
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
        content = content
    )
}