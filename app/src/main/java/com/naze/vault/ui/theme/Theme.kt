package com.naze.vault.ui.theme

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/** User-selectable app theme. LIGHT is the default. */
enum class NazeThemeMode { LIGHT, DARK, SYSTEM }

val NazeThemeMode.label: String
    get() = when (this) {
        NazeThemeMode.LIGHT -> "Terang"
        NazeThemeMode.DARK -> "Gelap"
        NazeThemeMode.SYSTEM -> "Ikuti sistem"
    }

/**
 * Persisted theme selection. Reads/writes SharedPreferences and exposes the
 * current mode as Compose state so the whole UI recomposes on change.
 */
object NazeThemePrefs {
    private const val PREFS_NAME = "naze_theme_prefs"
    private const val KEY_MODE = "mode"

    private lateinit var prefs: SharedPreferences

    var mode by mutableStateOf(NazeThemeMode.LIGHT)
        private set

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val stored = prefs.getString(KEY_MODE, null)
        mode = stored?.let { name -> runCatching { NazeThemeMode.valueOf(name) }.getOrNull() } ?: NazeThemeMode.LIGHT
    }

    fun setMode(newMode: NazeThemeMode) {
        prefs.edit().putString(KEY_MODE, newMode.name).apply()
        mode = newMode
    }
}

private val NazeDarkScheme = darkColorScheme(
    primary = NazeBlue,
    onPrimary = NazeTextPrimary,
    secondary = NazePurple,
    onSecondary = NazeTextPrimary,
    tertiary = NazeWarning,
    onTertiary = NazeBlack,
    background = NazeBlack,
    onBackground = NazeTextPrimary,
    surface = NazeSurface,
    onSurface = NazeTextPrimary,
    surfaceVariant = NazeSurfaceElevated,
    onSurfaceVariant = NazeTextSecondary,
    error = NazeDanger,
    outline = NazeDivider
)

private val NazeLightScheme = lightColorScheme(
    primary = NazeLightPrimary,
    onPrimary = NazeLightOnPrimary,
    secondary = NazeLightSecondary,
    onSecondary = NazeLightOnPrimary,
    tertiary = NazeLightWarning,
    onTertiary = NazeLightOnPrimary,
    background = NazeLightBackground,
    onBackground = NazeLightTextPrimary,
    surface = NazeLightSurface,
    onSurface = NazeLightTextPrimary,
    surfaceVariant = NazeLightSurfaceVariant,
    onSurfaceVariant = NazeLightTextSecondary,
    error = NazeLightDanger,
    outline = NazeLightDivider
)

@Composable
fun NazeVaultTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val useDarkTheme = when (NazeThemePrefs.mode) {
        NazeThemeMode.LIGHT -> false
        NazeThemeMode.DARK -> true
        NazeThemeMode.SYSTEM -> darkTheme
    }
    val colorScheme = if (useDarkTheme) NazeDarkScheme else NazeLightScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity ?: return@SideEffect
            val background = colorScheme.background.toArgb()
            activity.window.statusBarColor = background
            activity.window.navigationBarColor = background
            WindowCompat.getInsetsController(activity.window, view).isAppearanceLightStatusBars = !useDarkTheme
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowCompat.getInsetsController(activity.window, view).isAppearanceLightNavigationBars = !useDarkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = NazeTypography,
        content = content
    )
}
