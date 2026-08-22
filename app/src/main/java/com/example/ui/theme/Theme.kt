package com.example.ui.theme

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

private val DarkColorScheme =
  darkColorScheme(
    primary = MinimalistPrimaryLight,
    onPrimary = Color(0xFF002D6C),
    primaryContainer = MinimalistPrimaryDark,
    onPrimaryContainer = MinimalistPrimaryContainer,
    secondary = MinimalistSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF3E4758),
    onSecondaryContainer = Color(0xFFD9E2F9),
    background = MinimalistBackgroundDark,
    onBackground = MinimalistOnSurfaceDark,
    surface = MinimalistSurfaceDark,
    onSurface = MinimalistOnSurfaceDark,
    surfaceVariant = MinimalistSurfaceVariantDark,
    onSurfaceVariant = MinimalistOnSurfaceVariantDark,
    outline = MinimalistOutlineDark,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = MinimalistPrimary,
    onPrimary = Color.White,
    primaryContainer = MinimalistPrimaryContainer,
    onPrimaryContainer = MinimalistOnPrimaryContainer,
    secondary = MinimalistSecondary,
    onSecondary = Color.White,
    secondaryContainer = MinimalistSecondaryContainer,
    onSecondaryContainer = MinimalistOnSecondaryContainer,
    background = MinimalistBackgroundLight,
    onBackground = MinimalistOnSurfaceLight,
    surface = MinimalistSurfaceLight,
    onSurface = MinimalistOnSurfaceLight,
    surfaceVariant = MinimalistSurfaceVariantLight,
    onSurfaceVariant = MinimalistOnSurfaceVariantLight,
    outline = MinimalistOutlineLight,
  )

@Composable
fun BizVoiceTheme(
  themeMode: String = "SYSTEM",
  dynamicColor: Boolean = false, // Use intentional corporate branding by default
  content: @Composable () -> Unit,
) {
  val systemDark = isSystemInDarkTheme()
  val darkTheme = when (themeMode) {
    "DARK" -> true
    "LIGHT" -> false
    else -> systemDark
  }

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

