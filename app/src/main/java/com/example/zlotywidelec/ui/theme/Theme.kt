package com.example.zlotywidelec.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = LightBeigeText,
    secondary = MutedBrownAccent,
    tertiary = BeigeAccent,
    background = DeepBrownBackground,
    surface = DeepBrownBackground,
    onPrimary = DeepBrownBackground,
    onSecondary = LightBeigeText,
    onTertiary = DeepBrownBackground,
    onBackground = LightBeigeText,
    onSurface = LightBeigeText,
    surfaceVariant = MutedBrownAccent,
    onSurfaceVariant = LightBeigeText,
    primaryContainer = MutedBrownAccent,
    onPrimaryContainer = LightBeigeText,
    secondaryContainer = MutedBrownAccent,
    onSecondaryContainer = LightBeigeText,
    tertiaryContainer = MutedBrownAccent,
    onTertiaryContainer = LightBeigeText,
    surfaceContainer = DeepBrownBackground,
    surfaceContainerHigh = MutedBrownAccent,
    surfaceContainerHighest = MutedBrownAccent,
    surfaceTint = Color.Transparent
)

private val LightColorScheme = lightColorScheme(
    primary = DarkText,
    secondary = BeigeAccent,
    tertiary = GrayText,
    background = BeigeBackground,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = DarkText,
    onTertiary = Color.White,
    onBackground = DarkText,
    onSurface = DarkText,
    surfaceVariant = BeigeAccent,
    onSurfaceVariant = DarkText,
    primaryContainer = BeigeAccent,
    onPrimaryContainer = DarkText,
    secondaryContainer = BeigeAccent,
    onSecondaryContainer = DarkText,
    tertiaryContainer = BeigeAccent,
    onTertiaryContainer = DarkText,
    surfaceContainer = Color.White,
    surfaceContainerHigh = BeigeAccent,
    surfaceContainerHighest = BeigeAccent,
    surfaceTint = Color.Transparent
)

@Composable
fun ZlotyWidelecTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
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