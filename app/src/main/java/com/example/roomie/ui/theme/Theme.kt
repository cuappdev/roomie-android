package com.example.roomie.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// Dark mode is out of scope for the MVP (see specs.md); colors are tokens so it is a later swap.
// Dynamic color is off so Android 12+ wallpapers don't override the Roomie palette.
private val RoomieColorScheme = lightColorScheme(
    primary = BrandBrown,
    onPrimary = Surface,
    primaryContainer = PeachPill,
    onPrimaryContainer = TextPrimary,
    secondary = Tan,
    onSecondary = Surface,
    background = Background,
    onBackground = TextPrimary,
    surface = Background,
    onSurface = TextPrimary,
    surfaceVariant = PeachPill,
    onSurfaceVariant = TextSecondary,
    surfaceContainerHigh = Background,
    surfaceContainerHighest = Surface,
    outline = Border,
    outlineVariant = Divider,
    error = Danger,
    scrim = Scrim,
)

@Composable
fun RoomieTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RoomieColorScheme,
        typography = Typography,
        content = content
    )
}
