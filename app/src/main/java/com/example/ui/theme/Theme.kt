package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val MetronomeColorScheme = lightColorScheme(
    primary = MetronomeBordeaux,
    onPrimary = MetronomeCream,
    primaryContainer = MetronomeCreamDarker,
    onPrimaryContainer = MetronomeBordeaux,
    secondary = MetronomeGreen,
    onSecondary = MetronomeCream,
    tertiary = MetronomeOrange,
    onTertiary = MetronomeCream,
    background = MetronomeCream,
    onBackground = MetronomeBordeaux,
    surface = MetronomeCream,
    onSurface = MetronomeBordeaux,
    surfaceVariant = MetronomeCreamDarker,
    onSurfaceVariant = MetronomeBordeaux,
    outline = MetronomeBordeaux
)

@Composable
fun MetronomeTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MetronomeColorScheme,
        typography = Typography,
        content = content
    )
}
