package com.reggiesoft.citrusremote.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = sk_light_primary,
    onPrimary = sk_light_onPrimary,
    primaryContainer = sk_light_primaryContainer,
    onPrimaryContainer = sk_light_onPrimaryContainer,
    secondary = sk_light_secondary,
    onSecondary = sk_light_onSecondary,
    secondaryContainer = sk_light_secondaryContainer,
    onSecondaryContainer = sk_light_onSecondaryContainer,
    tertiary = sk_light_tertiary,
    onTertiary = sk_light_onTertiary,
    tertiaryContainer = sk_light_tertiaryContainer,
    onTertiaryContainer = sk_light_onTertiaryContainer,
    background = sk_light_background,
    onBackground = sk_light_onBackground,
    surface = sk_light_surface,
    onSurface = sk_light_onSurface,
    surfaceVariant = sk_light_surfaceVariant,
    onSurfaceVariant = sk_light_onSurfaceVariant,
    outline = sk_light_outline,
    inverseSurface = sk_light_inverseSurface,
    inverseOnSurface = sk_light_inverseOnSurface,
    inversePrimary = sk_light_inversePrimary,
    error = sk_light_error,
    onError = sk_light_onError,
    errorContainer = sk_light_errorContainer,
    onErrorContainer = sk_light_onErrorContainer
)

private val DarkColorScheme = darkColorScheme(
    primary = sk_dark_primary,
    onPrimary = sk_dark_onPrimary,
    primaryContainer = sk_dark_primaryContainer,
    onPrimaryContainer = sk_dark_onPrimaryContainer,
    secondary = sk_dark_secondary,
    onSecondary = sk_dark_onSecondary,
    secondaryContainer = sk_dark_secondaryContainer,
    onSecondaryContainer = sk_dark_onSecondaryContainer,
    tertiary = sk_dark_tertiary,
    onTertiary = sk_dark_onTertiary,
    tertiaryContainer = sk_dark_tertiaryContainer,
    onTertiaryContainer = sk_dark_onTertiaryContainer,
    background = sk_dark_background,
    onBackground = sk_dark_onBackground,
    surface = sk_dark_surface,
    onSurface = sk_dark_onSurface,
    surfaceVariant = sk_dark_surfaceVariant,
    onSurfaceVariant = sk_dark_onSurfaceVariant,
    outline = sk_dark_outline,
    inverseSurface = sk_dark_inverseSurface,
    inverseOnSurface = sk_dark_inverseOnSurface,
    inversePrimary = sk_dark_inversePrimary,
    error = sk_dark_error,
    onError = sk_dark_onError,
    errorContainer = sk_dark_errorContainer,
    onErrorContainer = sk_dark_onErrorContainer
)

@Composable
fun CitrusRemoteTheme(
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
        typography = Typography,
        content = content
    )
}
