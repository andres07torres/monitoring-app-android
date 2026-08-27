package com.example.inframonitor.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Enterprise Dashboard Theme (M3 implementation)
 * Uses high-contrast neutral backgrounds with sharp primary accents.
 */
private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = Surface,
    primaryContainer = PrimarySoft,
    onPrimaryContainer = Primary,
    secondary = Accent,
    onSecondary = Surface,
    secondaryContainer = InfoSoft,
    onSecondaryContainer = Info,
    background = Background,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceAlt,
    onSurfaceVariant = TextSecondary,
    outline = Outline,
    error = Error,
    onError = Surface,
    errorContainer = ErrorSoft,
    onErrorContainer = Error
)

@Composable
fun InfraMonitorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // For monitoring dashboards, a clean Light theme is usually preferred for readability
    val colorScheme = LightColorScheme 

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
