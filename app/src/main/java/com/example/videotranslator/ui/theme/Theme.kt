package com.example.videotranslator.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val StudioDarkColorScheme = darkColorScheme(
    primary = ElectricIndigo,
    onPrimary = TextPrimary,
    primaryContainer = ElectricIndigoContainer,
    onPrimaryContainer = ElectricIndigoLight,
    secondary = LuminousCyan,
    onSecondary = StudioObsidian,
    secondaryContainer = LuminousCyanContainer,
    onSecondaryContainer = LuminousCyan,
    tertiary = EmeraldSuccess,
    onTertiary = StudioObsidian,
    tertiaryContainer = EmeraldContainer,
    onTertiaryContainer = EmeraldSuccess,
    background = StudioObsidian,
    onBackground = TextPrimary,
    surface = StudioSurface,
    onSurface = TextPrimary,
    surfaceVariant = StudioSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    surfaceContainerHigh = StudioSurfaceElevated,
    surfaceContainerHighest = StudioSurfaceHighlight,
    outline = StudioOutline,
    outlineVariant = StudioSurfaceHighlight,
    error = CoralError,
    onError = TextPrimary,
    errorContainer = CoralErrorContainer,
    onErrorContainer = CoralError
)

private val StudioShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun VideoTranslatorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = StudioDarkColorScheme,
        typography = AppTypography,
        shapes = StudioShapes,
        content = content
    )
}
