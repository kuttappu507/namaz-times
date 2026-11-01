package com.namazapp.hubli.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.namazapp.hubli.data.ThemeMode

private val LightColors = lightColorScheme(
    primary = Emerald40,
    onPrimary = Color.White,
    primaryContainer = Emerald90,
    onPrimaryContainer = Emerald10,
    secondary = Gold40,
    onSecondary = Color.White,
    secondaryContainer = Gold90,
    onSecondaryContainer = Gold30,
    tertiary = Teal40,
    onTertiary = Color.White,
    tertiaryContainer = Teal90,
    onTertiaryContainer = Color(0xFF00201C),
    error = Error40,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Mist,
    onBackground = Ink,
    surface = Mist,
    onSurface = Ink,
    surfaceVariant = Mist3,
    onSurfaceVariant = Slate,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFEFF6F2),
    surfaceContainer = Mist2,
    surfaceContainerHigh = Color(0xFFE3EEE8),
    surfaceContainerHighest = Mist3,
    outline = Color(0xFF7A8A82),
    outlineVariant = Color(0xFFC6D3CC),
    inverseSurface = Ink3,
    inverseOnSurface = Mist2,
    inversePrimary = Emerald80,
    surfaceTint = Emerald40,
)

private val DarkColors = darkColorScheme(
    primary = Emerald70,
    onPrimary = Emerald10,
    primaryContainer = Emerald30,
    onPrimaryContainer = Emerald90,
    secondary = Gold80,
    onSecondary = Gold30,
    secondaryContainer = Color(0xFF4A3900),
    onSecondaryContainer = Gold90,
    tertiary = Teal80,
    onTertiary = Color(0xFF12342F),
    tertiaryContainer = Color(0xFF244A44),
    onTertiaryContainer = Teal90,
    error = Error80,
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0B1512),
    onBackground = Color(0xFFE6EFEA),
    surface = Color(0xFF0B1512),
    onSurface = Color(0xFFE6EFEA),
    surfaceVariant = Color(0xFF2A3B34),
    onSurfaceVariant = Color(0xFFB9C7BF),
    surfaceContainerLowest = Color(0xFF070E0B),
    surfaceContainerLow = Color(0xFF131E19),
    surfaceContainer = Color(0xFF182620),
    surfaceContainerHigh = Color(0xFF213129),
    surfaceContainerHighest = Color(0xFF2A3B34),
    outline = SlateLight,
    outlineVariant = Ink5,
    inverseSurface = Mist2,
    inverseOnSurface = Ink3,
    inversePrimary = Emerald40,
    surfaceTint = Emerald70,
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

@Composable
fun NamazAppTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme = if (dark) DarkColors else LightColors
    val extended = if (dark) DarkExtended else LightExtended

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }

    CompositionLocalProvider(LocalExtendedColors provides extended) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
