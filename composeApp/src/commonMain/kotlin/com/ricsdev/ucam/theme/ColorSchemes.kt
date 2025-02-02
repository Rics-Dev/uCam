package com.ricsdev.ucam.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color




val LightColorScheme = lightColorScheme(
    primary = Color(0xFF000000), // Black
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E0E0), // Lighter gray container
    onPrimaryContainer = Color(0xFF212121),
    secondary = Color(0xFF616161),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF0F0F0), // Even lighter gray container
    onSecondaryContainer = Color(0xFF424242),
    tertiary = Color(0xFF757575),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF5F5F5), // Very light gray container
    onTertiaryContainer = Color(0xFF616161),
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFF0F0F0),
    onSurfaceVariant = Color(0xFF424242),
    error = Color(0xFFB00020),
    onError = Color.White,
    errorContainer = Color(0xFFFCDAD7),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFFBDBDBD)
)

val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFE0E0E0), // Light gray instead of white
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF303030), // Lighter dark gray container
    onPrimaryContainer = Color(0xFFE0E0E0),
    secondary = Color(0xFFBDBDBD),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF424242), // Medium gray container
    onSecondaryContainer = Color(0xFFE0E0E0),
    tertiary = Color(0xFF9E9E9E),
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF303030), // Lighter dark gray container
    onTertiaryContainer = Color(0xFFE0E0E0),
    background = Color(0xFF1E1E1E), // Slightly lighter than almost black
    onBackground = Color.White,
    surface = Color(0xFF1E1E1E),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF303030),
    onSurfaceVariant = Color(0xFFBDBDBD),
    error = Color(0xFFCF6679),
    onError = Color.Black,
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF757575) // Slightly lighter gray outline
)
