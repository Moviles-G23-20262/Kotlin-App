package com.campusswap.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    secondary = AccentBlue,
    onSecondary = Color.White,
    tertiary = SecondaryBlue,
    onTertiary = DeepNavy,
    background = SurfaceLight,
    onBackground = DeepNavy,
    surface = Color.White,
    onSurface = DeepNavy,
    surfaceVariant = LightBlue.copy(alpha = 0.35f),
    onSurfaceVariant = PrimaryBlue,
    outline = SecondaryBlue,
    error = ErrorRed,
)

private val DarkColors = darkColorScheme(
    primary = SecondaryBlue,
    onPrimary = DeepNavy,
    secondary = AccentBlue,
    onSecondary = Color.White,
    tertiary = LightBlue,
    onTertiary = DeepNavy,
    background = DeepNavy,
    onBackground = Color.White,
    surface = SurfaceDark,
    onSurface = Color.White,
    surfaceVariant = PrimaryBlue.copy(alpha = 0.45f),
    onSurfaceVariant = LightBlue,
    outline = SecondaryBlue,
    error = Color(0xFFFF8A80),
)

@Composable
fun CampusSwapTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = CampusSwapTypography,
        content = content
    )
}
