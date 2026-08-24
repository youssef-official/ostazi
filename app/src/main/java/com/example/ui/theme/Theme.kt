package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = SkyPrimary,
    onPrimary = SkyOnPrimary,
    primaryContainer = SkyPrimaryContainer,
    onPrimaryContainer = SkyOnPrimaryContainer,
    secondary = LimePrimary,
    onSecondary = Color.White,
    secondaryContainer = LimeContainer,
    onSecondaryContainer = LimeOnContainer,
    background = SkyBackground,
    surface = SkySurface,
    surfaceVariant = SkySurfaceVariant,
    onBackground = SkyOnSurface,
    onSurface = SkyOnSurface,
    onSurfaceVariant = SkyOnSurfaceVariant,
    outline = SkyOutline,
    outlineVariant = SkyOutlineVariant,
    tertiary = Color(0xFF177A68),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD9F1EB),
    onTertiaryContainer = Color(0xFF0B5145)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFA8C5F4), onPrimary = Color(0xFF102B55),
    primaryContainer = Color(0xFF203D68), onPrimaryContainer = Color(0xFFDCE9FF),
    secondary = Color(0xFFE4BE5C), onSecondary = Color(0xFF3B2D00),
    secondaryContainer = Color(0xFF56440F), onSecondaryContainer = Color(0xFFFFEAB1),
    tertiary = Color(0xFF73D5BF), onTertiary = Color(0xFF00382F),
    tertiaryContainer = Color(0xFF125446), onTertiaryContainer = Color(0xFFADF2DF),
    background = Color(0xFF0B1423), surface = Color(0xFF111D2F),
    surfaceVariant = Color(0xFF19283C), onBackground = Color(0xFFF2F5FA),
    onSurface = Color(0xFFF2F5FA), onSurfaceVariant = Color(0xFFBBC6D6),
    outline = Color(0xFF58677C), outlineVariant = Color(0xFF2A394E)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes(
            extraSmall = RoundedCornerShape(10.dp),
            small = RoundedCornerShape(14.dp),
            medium = RoundedCornerShape(18.dp),
            large = RoundedCornerShape(22.dp),
            extraLarge = RoundedCornerShape(28.dp)
        ),
        content = content
    )
}
