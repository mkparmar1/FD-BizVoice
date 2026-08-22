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
    primary = ModernPrimaryLight,
    onPrimary = Color(0xFF0B192C),
    primaryContainer = ModernPrimaryDark,
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = ModernSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF1E293B),
    onSecondaryContainer = Color(0xFFF1F5F9),
    background = ModernBackgroundDark,
    onBackground = ModernOnSurfaceDark,
    surface = ModernSurfaceDark,
    onSurface = ModernOnSurfaceDark,
    surfaceVariant = ModernSurfaceVariantDark,
    onSurfaceVariant = ModernOnSurfaceVariantDark,
    outline = ModernOutlineDark,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = ModernPrimary,
    onPrimary = Color.White,
    primaryContainer = ModernPrimaryContainer,
    onPrimaryContainer = ModernOnPrimaryContainer,
    secondary = ModernSecondary,
    onSecondary = Color.White,
    secondaryContainer = ModernSecondaryContainer,
    onSecondaryContainer = ModernOnSecondaryContainer,
    background = ModernBackgroundLight,
    onBackground = ModernOnSurfaceLight,
    surface = ModernSurfaceLight,
    onSurface = ModernOnSurfaceLight,
    surfaceVariant = ModernSurfaceVariantLight,
    onSurfaceVariant = ModernOnSurfaceVariantLight,
    outline = ModernOutlineLight,
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

