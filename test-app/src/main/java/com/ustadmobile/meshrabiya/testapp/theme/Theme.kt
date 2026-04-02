package com.ustadmobile.meshrabiya.testapp.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val MeshrabiyaColorScheme = darkColorScheme(
    primary              = MeshAmber,
    onPrimary            = MeshOnAmber,
    primaryContainer     = Color(0xFF3D2F00),
    onPrimaryContainer   = MeshAmber,

    secondary            = MeshTeal,
    onSecondary          = Color(0xFF001A17),
    secondaryContainer   = Color(0xFF1A3330),
    onSecondaryContainer = MeshTeal,

    tertiary             = MeshWood,
    onTertiary           = MeshText,

    background           = MeshBackground,
    onBackground         = MeshText,

    surface              = MeshSurface,
    onSurface            = MeshText,
    surfaceVariant       = MeshSurfaceVariant,
    onSurfaceVariant     = MeshTextSecondary,

    outline              = MeshOutline,
    outlineVariant       = Color(0xFF302A26),

    error                = MeshError,
    onError              = Color(0xFF1A000A),
)

@Composable
fun HttpOverBluetoothTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = MeshBackground.toArgb()
            window.navigationBarColor = MeshBackground.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = MeshrabiyaColorScheme,
        typography = Typography,
        content = content
    )
}
