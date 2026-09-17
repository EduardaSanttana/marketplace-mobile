package edu.ifsp.marketplace.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = InfraBlue80,
    onPrimary = InfraBlue20,
    primaryContainer = InfraBlue30,
    onPrimaryContainer = InfraBlue90,
    secondary = InfraCyan80,
    onSecondary = InfraNavy10,
    secondaryContainer = InfraBlue20,
    onSecondaryContainer = InfraCyan90,
    tertiary = InfraCyan80,
    background = InfraNavy10,
    onBackground = InfraGray90,
    surface = InfraNavy20,
    onSurface = InfraGray90,
    surfaceVariant = Color(0xFF2A3142),
    onSurfaceVariant = InfraGray90,
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A)
)

private val LightColorScheme = lightColorScheme(
    primary = InfraBlue40,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = InfraBlue90,
    onPrimaryContainer = InfraBlue10,
    secondary = InfraCyan40,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = InfraCyan90,
    onSecondaryContainer = InfraBlue10,
    tertiary = InfraCyan40,
    background = InfraGray95,
    onBackground = InfraGray10,
    surface = InfraGray99,
    onSurface = InfraGray10,
    surfaceVariant = InfraGray90,
    onSurfaceVariant = Color(0xFF44474F),
    error = InfraError,
    errorContainer = InfraErrorContainer
)

@Composable
fun MarketplaceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
