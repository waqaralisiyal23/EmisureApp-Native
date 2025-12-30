package com.emisure.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// ====================================================================
// Dark Color Scheme - Matches the premium dark icon aesthetic
// ====================================================================
private val DarkColorScheme = darkColorScheme(
    primary = PrimaryCyan,
    onPrimary = PrimaryNavy,
    primaryContainer = SecondaryTeal,
    onPrimaryContainer = LightSurface,
    secondary = SecondaryTeal,
    onSecondary = LightSurface,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = DarkOnSurface,
    tertiary = MetallicSilver,
    onTertiary = PrimaryNavy,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    error = StatusRed,
    onError = LightSurface
)

// ====================================================================
// Light Color Scheme - Clean and professional
// ====================================================================
private val LightColorScheme = lightColorScheme(
    primary = PrimaryCyan,
    onPrimary = LightSurface,
    primaryContainer = PrimaryCyanDark,
    onPrimaryContainer = LightSurface,
    secondary = SecondaryTeal,
    onSecondary = LightSurface,
    secondaryContainer = LightSurfaceVariant,
    onSecondaryContainer = LightOnSurface,
    tertiary = MetallicSilverDark,
    onTertiary = LightSurface,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    error = StatusRed,
    onError = LightSurface
)

@Composable
fun EmisureTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    // Disabled by default to maintain brand consistency
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
        typography = Typography,
        content = content
    )
}