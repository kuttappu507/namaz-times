package com.namazapp.hubli.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ---- Brand: deep emerald + warm gold ----------------------------------------------------------
val Emerald05 = Color(0xFF031A14)
val Emerald10 = Color(0xFF06261E)
val Emerald20 = Color(0xFF06483A)
val Emerald30 = Color(0xFF0B5F49)
val Emerald40 = Color(0xFF0B7A5B)
val Emerald50 = Color(0xFF139A72)
val Emerald60 = Color(0xFF19B58A)
val Emerald70 = Color(0xFF4CCBA6)
val Emerald80 = Color(0xFF8FE3C6)
val Emerald90 = Color(0xFFC3F3E2)
val Emerald95 = Color(0xFFE2FAF1)

val Gold30 = Color(0xFF6B4E00)
val Gold40 = Color(0xFF8F6A05)
val Gold50 = Color(0xFFB8891A)
val Gold60 = Color(0xFFD9A62F)
val Gold70 = Color(0xFFE8BC52)
val Gold80 = Color(0xFFF5C451)
val Gold90 = Color(0xFFFFE7A6)
val Gold95 = Color(0xFFFFF3D2)

val Teal40 = Color(0xFF2E6B66)
val Teal80 = Color(0xFF9FD4CC)
val Teal90 = Color(0xFFBEEFE7)

// ---- Neutrals with a slight green tint ----------------------------------------------------------
val Ink = Color(0xFF0A1410)
val Ink2 = Color(0xFF111C17)
val Ink3 = Color(0xFF172420)
val Ink4 = Color(0xFF1F2E28)
val Ink5 = Color(0xFF283A33)
val Mist = Color(0xFFF4FAF6)
val Mist2 = Color(0xFFEAF3EE)
val Mist3 = Color(0xFFDFEBE5)
val Slate = Color(0xFF5C6B64)
val SlateLight = Color(0xFF8E9C95)

val Error40 = Color(0xFFBA1A1A)
val Error80 = Color(0xFFFFB4AB)

// ---- Time-of-day palettes for the hero card ----------------------------------------------------
data class SkyPalette(
    val top: Color,
    val mid: Color,
    val bottom: Color,
    val glow: Color,
    val accent: Color,
)

val SkyNight = SkyPalette(Color(0xFF0B1630), Color(0xFF15264F), Color(0xFF223B6E), Color(0xFF4A6DB0), Gold80)
val SkyDawn = SkyPalette(Color(0xFF2A2E62), Color(0xFF7A4B7C), Color(0xFFE88A64), Color(0xFFFFB27A), Gold90)
val SkyMorning = SkyPalette(Color(0xFF06483A), Color(0xFF0E8A66), Color(0xFF3FBF93), Color(0xFF8CE7C2), Gold80)
val SkyNoon = SkyPalette(Color(0xFF0B5F49), Color(0xFF14A47A), Color(0xFF61CFA6), Color(0xFFC9F5E4), Gold80)
val SkyAfternoon = SkyPalette(Color(0xFF14524A), Color(0xFF2C8A72), Color(0xFFD9A62F), Color(0xFFFFD873), Color.White)
val SkyDusk = SkyPalette(Color(0xFF2E1B4E), Color(0xFF7A3A6D), Color(0xFFE5643F), Color(0xFFFF9C6B), Gold90)
val SkyEvening = SkyPalette(Color(0xFF14183A), Color(0xFF2B2F6B), Color(0xFF4B3F8C), Color(0xFF7C6CC7), Gold80)

/** Palette for a given local hour of day. */
fun skyPaletteFor(hour: Int): SkyPalette = when (hour) {
    in 4..5 -> SkyDawn
    in 6..10 -> SkyMorning
    in 11..14 -> SkyNoon
    in 15..17 -> SkyAfternoon
    in 18..18 -> SkyDusk
    in 19..21 -> SkyEvening
    else -> SkyNight
}

/** Extra semantic colours that Material's scheme does not model. */
data class ExtendedColors(
    val gold: Color,
    val onGold: Color,
    val goldContainer: Color,
    val onGoldContainer: Color,
    val cardGradientStart: Color,
    val cardGradientEnd: Color,
    val shimmer: Color,
    val navGlass: Color,
    val isDark: Boolean,
)

val LightExtended = ExtendedColors(
    gold = Gold50,
    onGold = Color.White,
    goldContainer = Gold95,
    onGoldContainer = Gold30,
    cardGradientStart = Color.White,
    cardGradientEnd = Color(0xFFF0F7F3),
    shimmer = Color.White.copy(alpha = 0.6f),
    navGlass = Color.White.copy(alpha = 0.94f),
    isDark = false,
)

val DarkExtended = ExtendedColors(
    gold = Gold80,
    onGold = Gold30,
    goldContainer = Color(0xFF3F3000),
    onGoldContainer = Gold90,
    cardGradientStart = Color(0xFF1B2A25),
    cardGradientEnd = Color(0xFF14211C),
    shimmer = Color.White.copy(alpha = 0.08f),
    navGlass = Color(0xFF1B2A25).copy(alpha = 0.96f),
    isDark = true,
)

val LocalExtendedColors = staticCompositionLocalOf { LightExtended }

/** Accessor mirroring `MaterialTheme.colorScheme`. */
object AppTheme {
    val extended: ExtendedColors
        @Composable @ReadOnlyComposable get() = LocalExtendedColors.current
}
