package com.naze.vault.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Naze Vault ships dark-mode only by design (matches the "vault" concept).
private val NazeColorScheme = darkColorScheme(
    primary = NazeBlue,
    onPrimary = NazeTextPrimary,
    secondary = NazePurple,
    onSecondary = NazeTextPrimary,
    background = NazeBlack,
    onBackground = NazeTextPrimary,
    surface = NazeSurface,
    onSurface = NazeTextPrimary,
    surfaceVariant = NazeSurfaceElevated,
    onSurfaceVariant = NazeTextSecondary,
    error = NazeDanger,
    outline = NazeDivider
)

@Composable
fun NazeVaultTheme(
    // Parameter kept for API compatibility; Naze Vault intentionally always
    // renders dark to stay true to its "secure vault" identity.
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity ?: return@SideEffect
            activity.window.statusBarColor = NazeBlack.toArgb()
            activity.window.navigationBarColor = NazeBlack.toArgb()
            WindowCompat.getInsetsController(activity.window, view).isAppearanceLightStatusBars = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowCompat.getInsetsController(activity.window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = NazeColorScheme,
        typography = NazeTypography,
        content = content
    )
}
