@file:OptIn(ExperimentalTextApi::class)

package com.campusswap.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.campusswap.app.R

// Fraunces (display serif) — headings and prominent figures.
// The web prototype loads Fraunces with `font-optical-sizing: auto`, so the browser feeds the
// optical-size axis the font size in px. Android has no automatic optical sizing, so we build one
// family per step of the scale and pick it by size. SOFT/WONK are pinned to 0, which is what
// Google Fonts serves; the bundled file defaults to WONK = 1 (the wonky alternates).
private fun fraunces(opticalSize: Float): FontFamily = FontFamily(
    listOf(
        400 to FontWeight.Normal,
        500 to FontWeight.Medium,
        600 to FontWeight.SemiBold,
        700 to FontWeight.Bold,
    ).map { (weight, fontWeight) ->
        Font(
            R.font.fraunces_variable,
            weight = fontWeight,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(weight),
                FontVariation.Setting("opsz", opticalSize),
                FontVariation.Setting("SOFT", 0f),
                FontVariation.Setting("WONK", 0f),
            ),
        )
    }
)

val Fraunces12 = fraunces(12f)
val Fraunces16 = fraunces(16f)
val Fraunces21 = fraunces(21f)
val Fraunces28 = fraunces(28f)
val Fraunces38 = fraunces(38f)

/** Picks the Fraunces instance whose optical size matches the text size. */
fun frauncesFor(sizeSp: Float): FontFamily = when {
    sizeSp <= 13f -> Fraunces12
    sizeSp <= 18f -> Fraunces16
    sizeSp <= 24f -> Fraunces21
    sizeSp <= 32f -> Fraunces28
    else -> Fraunces38
}

/** Default instance for call sites that don't know their size. */
val FrauncesFamily = Fraunces21

// Outfit (geometric sans) — body copy and interface text.
val OutfitFamily = FontFamily(
    Font(R.font.outfit_variable, weight = FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.outfit_variable, weight = FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.outfit_variable, weight = FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.outfit_variable, weight = FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
)

// JetBrains Mono — technical identifiers, course codes, price figures where alignment matters.
val JetBrainsMonoFamily = FontFamily(
    Font(R.font.jetbrains_mono_variable, weight = FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.jetbrains_mono_variable, weight = FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.jetbrains_mono_variable, weight = FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
)

val CampusSwapTypography = Typography(
    displayLarge = TextStyle(fontFamily = Fraunces38, fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 46.sp),
    displayMedium = TextStyle(fontFamily = Fraunces28, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 38.sp),
    headlineLarge = TextStyle(fontFamily = Fraunces28, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 34.sp),
    headlineMedium = TextStyle(fontFamily = Fraunces21, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 30.sp),
    headlineSmall = TextStyle(fontFamily = Fraunces21, fontWeight = FontWeight.Medium, fontSize = 20.sp, lineHeight = 26.sp),
    titleLarge = TextStyle(fontFamily = Fraunces16, fontWeight = FontWeight.Medium, fontSize = 18.sp, lineHeight = 24.sp),
    titleMedium = TextStyle(fontFamily = OutfitFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = OutfitFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = OutfitFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = OutfitFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = OutfitFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = OutfitFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(fontFamily = OutfitFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp),
)

object CampusType {
    val size2xs = 9.sp
    val sizeXs = 12.sp
    val sizeSm = 16.sp
    val sizeMd = 21.3.sp
    val sizeLg = 28.4.sp
    val sizeXl = 37.9.sp

    val heading = TextStyle(fontFamily = Fraunces21, fontWeight = FontWeight.SemiBold)
    val body = TextStyle(fontFamily = OutfitFamily, fontWeight = FontWeight.Normal)
    val mono = TextStyle(fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Normal)

    val sectionTitle = TextStyle(
        fontFamily = Fraunces21,
        fontWeight = FontWeight.SemiBold,
        fontSize = sizeMd,
        letterSpacing = (-0.015).em,
    )
}
