package com.example.busschedule.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

// --- Define the Color Schemes using colors from Color.kt ---

private val DarkColorScheme = darkColorScheme(
    primary = CyanAccent,               // Your Teal
    onPrimary = DarkOnPrimary,          // Text on Teal
    secondary = CyanAccent.copy(alpha = 0.7f), // Example secondary - adjust if needed
    onSecondary = DarkOnPrimary,        // Example - adjust if needed
    background = DarkBackground,        // Dark base background for scheme elements
    onBackground = DarkOnSurface,       // Text on Dark Background
    surface = DarkSurface,              // Color for Cards, Drawers, etc.
    onSurface = DarkOnSurface,          // Text on Surfaces
    error = ErrorRed,
    onError = White,
    surfaceVariant = Color(0xFF212121), // Slightly different surface variant
    onSurfaceVariant = GreyText         // Text on surfaceVariant (e.g., outlines)
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,             // Blue accent for light theme
    onPrimary = LightOnPrimary,         // Text on Blue
    secondary = LightPrimary.copy(alpha = 0.7f), // Example secondary - adjust if needed
    onSecondary = LightOnPrimary,       // Example - adjust if needed
    background = CreamyWhiteBackground, // Your Creamy background for scheme elements
    onBackground = LightOnSurface,      // Text on Creamy Background
    surface = LightSurface,             // White surface for Cards, Drawers etc.
    onSurface = LightOnSurface,         // Text on White Surfaces
    error = ErrorRed,
    onError = White,
    surfaceVariant = Color(0xFFE0E0E0), // Light grey surface variant
    onSurfaceVariant = GreyText.copy(alpha = 0.8f) // Text on light surfaceVariant
)

// --- BusScheduleTheme Composable ---

@Composable
fun BusScheduleTheme(
    // Accept Boolean and FontColor VALUES now
    darkTheme: Boolean,
    fontColor: FontColor,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            // Use the passed-in 'darkTheme' value
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        // Use the passed-in 'darkTheme' and 'fontColor' values
        darkTheme -> DarkColorScheme.copy(primary = fontColor.color)
        else -> LightColorScheme.copy(primary = fontColor.color)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}