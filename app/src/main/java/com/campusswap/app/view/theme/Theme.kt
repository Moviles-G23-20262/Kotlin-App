package com.campusswap.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.unit.dp


private fun CampusColors.toColorScheme(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = accent,
        onPrimary = accentText,
        primaryContainer = tagBg,
        onPrimaryContainer = tagText,
        inversePrimary = accentHi,
        secondary = accentHi,
        onSecondary = accentText,
        secondaryContainer = elevated,
        onSecondaryContainer = text,
        tertiary = accentHi,
        onTertiary = accentText,
        background = bg,
        onBackground = text,
        surface = surface,
        onSurface = text,
        surfaceVariant = elevated,
        onSurfaceVariant = text2,
        surfaceTint = surface,
        inverseSurface = text,
        inverseOnSurface = bg,
        error = error,
        onError = accentText,
        outline = borderSubtle,
        outlineVariant = border,
        scrim = shadowCard,
        surfaceBright = elevated,
        surfaceDim = bg,
        surfaceContainerLowest = bg,
        surfaceContainerLow = surface,
        surfaceContainer = surface,
        surfaceContainerHigh = elevated,
        surfaceContainerHighest = elevated,
    )
}

private val CampusShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp),
)

@Composable
fun CampusSwapTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkCampusColors else LightCampusColors
    CompositionLocalProvider(LocalCampusColors provides colors) {
        MaterialTheme(
            colorScheme = colors.toColorScheme(),
            typography = CampusSwapTypography,
            shapes = CampusShapes,
            content = content,
        )
    }
}
object CampusSwapTheme {
    val colors: CampusColors
        @Composable
        @ReadOnlyComposable
        get() = LocalCampusColors.current
}
