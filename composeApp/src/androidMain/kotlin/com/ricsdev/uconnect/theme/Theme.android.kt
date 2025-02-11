package com.ricsdev.uconnect.theme


import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat


@Composable
actual fun UConnectTheme(content: @Composable () -> Unit) {

    val darkTheme = isSystemInDarkTheme()
    val context = LocalContext.current
    val view = LocalView.current
    val activity = LocalActivity.current

    val colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (darkTheme) DarkColorScheme else LightColorScheme
    }

    SideEffect {
        val window = activity?.window
        if (window != null) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }
        val windowInsetsController = window?.let { WindowInsetsControllerCompat(it, view) }

        // Set the status bar color
        if (window != null) {
            window.statusBarColor = colorScheme.background.toArgb()
        }
        // Set the navigation bar color
        if (window != null) {
            window.navigationBarColor = colorScheme.background.toArgb()
        }

        if (windowInsetsController != null) {
            windowInsetsController.isAppearanceLightStatusBars = !darkTheme
        }
        if (windowInsetsController != null) {
            windowInsetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}