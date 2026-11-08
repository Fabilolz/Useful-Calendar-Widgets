package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryExpressive,
    onPrimary = OnPrimaryExpressive,
    primaryContainer = PrimaryContainerExpressive,
    onPrimaryContainer = OnPrimaryContainerExpressive,
    secondary = SecondaryExpressive,
    onSecondary = OnSecondaryExpressive,
    secondaryContainer = SecondaryContainerExpressive,
    onSecondaryContainer = OnSecondaryContainerExpressive,
    tertiary = TertiaryExpressive,
    onTertiary = OnTertiaryExpressive,
    tertiaryContainer = TertiaryContainerExpressive,
    onTertiaryContainer = OnTertiaryContainerExpressive,
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onBackground = OnSurfaceDark,
    onSurface = OnSurfaceDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryExpressive,
    onPrimary = OnPrimaryExpressive,
    primaryContainer = PrimaryContainerExpressive,
    onPrimaryContainer = OnPrimaryContainerExpressive,
    secondary = SecondaryExpressive,
    onSecondary = OnSecondaryExpressive,
    secondaryContainer = SecondaryContainerExpressive,
    onSecondaryContainer = OnSecondaryContainerExpressive,
    tertiary = TertiaryExpressive,
    onTertiary = OnTertiaryExpressive,
    tertiaryContainer = TertiaryContainerExpressive,
    onTertiaryContainer = OnTertiaryContainerExpressive,
    background = BackgroundLight,
    surface = SurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onBackground = OnSurfaceLight,
    onSurface = OnSurfaceLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
