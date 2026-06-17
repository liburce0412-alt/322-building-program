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
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = GeometricPrimary,
    secondary = GeometricSecondary,
    tertiary = GeometricTertiary,
    background = GeometricBackground,
    surface = GeometricSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = GeometricOnTertiary,
    onBackground = GeometricOnBackground,
    onSurface = GeometricOnBackground,
    primaryContainer = GeometricPrimaryContainer,
    onPrimaryContainer = GeometricOnPrimaryContainer,
    surfaceVariant = GeometricSurfaceVariant,
    onSurfaceVariant = GeometricOnSurfaceVariant,
    outline = GeometricOutline
  )

private val LightColorScheme =
  lightColorScheme(
    primary = GeometricPrimary,
    secondary = GeometricSecondary,
    tertiary = GeometricTertiary,
    background = GeometricBackground,
    surface = GeometricSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = GeometricOnTertiary,
    onBackground = GeometricOnBackground,
    onSurface = GeometricOnBackground,
    primaryContainer = GeometricPrimaryContainer,
    onPrimaryContainer = GeometricOnPrimaryContainer,
    surfaceVariant = GeometricSurfaceVariant,
    onSurfaceVariant = GeometricOnSurfaceVariant,
    outline = GeometricOutline
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Disabling dynamic colors to enforce the geometric theme
  dynamicColor: Boolean = true,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
