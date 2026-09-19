package com.campusswap.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Core CampusSwap palette (Section 2.1), still referenced directly by the screens that
// predate the token system: Login, Chat, Sell and Meeting point.
val PrimaryBlue = Color(0xFF202A6A)
val AccentBlue = Color(0xFF3B4DC4)
val SecondaryBlue = Color(0xFF8B94D0)

// Functional colors, used only for status/feedback states.
val SuccessGreen = Color(0xFF24BC5C)
val ErrorRed = Color(0xFFDE2E21)
val WarningAmber = Color(0xFFF59F0A)

// ---- Figma prototype scales (index.css) ----
private val Primary50 = Color(0xFFF8F8F9)
private val Primary100 = Color(0xFFEEEEF2)
private val Primary200 = Color(0xFFD7D9E5)
private val Primary300 = Color(0xFFAFB4D5)
private val Primary400 = Color(0xFF7883C9)
private val Primary500 = Color(0xFF3B4DC4)
private val Primary600 = Color(0xFF2E3DA4)
private val Primary700 = Color(0xFF222F81)
private val Primary800 = Color(0xFF18215D)
private val Primary900 = Color(0xFF11163C)
private val Primary950 = Color(0xFF0A0D1F)

private val Neutral50 = Color(0xFFF9F9F9)
private val Neutral100 = Color(0xFFF2F2F2)
private val Neutral300 = Color(0xFFD3D3D5)
private val Neutral500 = Color(0xFF76777E)
private val Neutral700 = Color(0xFF404045)
private val Neutral800 = Color(0xFF2A2A2D)
private val Neutral900 = Color(0xFF18191B)
private val Neutral950 = Color(0xFF0F0F10)

/**
 * Semantic tokens that mirror the CSS variables of the Figma prototype
 * (--bg, --bg-surface, --bd, --tx, --accent ...). Screens should read colors from
 * [CampusSwapTheme.colors] so light/dark switch exactly like the prototype.
 */
@Immutable
data class CampusColors(
    val isDark: Boolean,
    val bg: Color,
    val surface: Color,
    val elevated: Color,
    val border: Color,
    val borderSubtle: Color,
    val text: Color,
    val text2: Color,
    val textMuted: Color,
    val accent: Color,
    val accentHi: Color,
    val accentLo: Color,
    val accentText: Color,
    val tagBg: Color,
    val tagText: Color,
    val tagHiBg: Color,
    val tagHiText: Color,
    val chipBg: Color,
    val chipText: Color,
    val heroFrom: Color,
    val heroTo: Color,
    val shadowCard: Color,
    val shadowAccent: Color,
    val outerBg: Color,
    val success: Color,
    val warning: Color,
    val error: Color,
)

val DarkCampusColors = CampusColors(
    isDark = true,
    bg = Neutral950,
    surface = Neutral900,
    elevated = Neutral800,
    border = Neutral800,
    borderSubtle = Neutral700,
    text = Neutral50,
    text2 = Neutral300,
    textMuted = Neutral500,
    accent = Primary500,
    accentHi = Primary400,
    accentLo = Primary700,
    accentText = Neutral50,
    tagBg = Primary900,
    tagText = Primary300,
    tagHiBg = Primary500,
    tagHiText = Neutral50,
    chipBg = Neutral800,
    chipText = Neutral300,
    heroFrom = Primary900,
    heroTo = Primary700,
    shadowCard = Color(0x73000000),
    shadowAccent = Color(0x8C222F81),
    outerBg = Primary950,
    success = Color(0xFF6DD493),
    warning = Color(0xFFE6B35B),
    error = Color(0xFFD97069),
)

val LightCampusColors = CampusColors(
    isDark = false,
    bg = Primary50,
    surface = Color.White,
    elevated = Primary100,
    border = Primary200,
    borderSubtle = Primary300,
    text = Primary900,
    text2 = Primary800,
    textMuted = Primary400,
    accent = Primary500,
    accentHi = Primary600,
    accentLo = Primary700,
    accentText = Color.White,
    tagBg = Primary100,
    tagText = Primary700,
    tagHiBg = Primary500,
    tagHiText = Color.White,
    chipBg = Neutral100,
    chipText = Neutral700,
    heroFrom = Primary600,
    heroTo = Primary500,
    shadowCard = Color(0x1A3B4DC4),
    shadowAccent = Color(0x2E222F81),
    outerBg = Primary200,
    success = Color(0xFF1A9D4B),
    warning = Color(0xFFCE8403),
    error = Color(0xFFBA2217),
)

val LocalCampusColors = staticCompositionLocalOf { DarkCampusColors }
