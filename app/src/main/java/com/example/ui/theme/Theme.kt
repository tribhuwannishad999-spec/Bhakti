package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val BhaktiLightColorScheme = lightColorScheme(
    primary = RoyalSaffron,
    onPrimary = Color.White,
    primaryContainer = SoftGoldTint,
    onPrimaryContainer = DeepMaroon,
    secondary = DeepMaroon,
    onSecondary = TempleCream,
    secondaryContainer = SandstoneSurface,
    onSecondaryContainer = DeepMaroon,
    tertiary = SpiritualPurple,
    onTertiary = Color.White,
    tertiaryContainer = SoftPurpleTint,
    onTertiaryContainer = SpiritualPurple,
    background = TempleCream,
    onBackground = DeepInk,
    surface = WarmIvory,
    onSurface = DeepInk,
    surfaceVariant = SandstoneSurface,
    onSurfaceVariant = MutedInk,
    outline = SacredGold
)

private val BhaktiDarkColorScheme = darkColorScheme(
    primary = BrightSaffron,
    onPrimary = SanctumNight,
    primaryContainer = RichMaroon,
    onPrimaryContainer = LuminousGold,
    secondary = LuminousGold,
    onSecondary = SanctumNight,
    secondaryContainer = DarkCardSurface,
    onSecondaryContainer = TempleCream,
    tertiary = Color(0xFFD0A3F0),
    onTertiary = SanctumPurpleGlow,
    tertiaryContainer = SanctumPurpleGlow,
    onTertiaryContainer = SoftPurpleTint,
    background = SanctumNight,
    onBackground = TempleCream,
    surface = DarkCardSurface,
    onSurface = TempleCream,
    surfaceVariant = SanctumMaroonDark,
    onSurfaceVariant = Color(0xFFE7D3C5),
    outline = DarkCardBorder
)

val BhaktiShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun BhaktiDarshanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) BhaktiDarkColorScheme else BhaktiLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = BhaktiShapes,
        content = content
    )
}
