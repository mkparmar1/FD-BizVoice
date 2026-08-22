package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Modern High-End Enterprise VoIP Palette
val ModernPrimary = Color(0xFF2563EB) // Vibrant Electric Cobalt
val ModernPrimaryLight = Color(0xFF60A5FA)
val ModernPrimaryDark = Color(0xFF1D4ED8)
val ModernPrimaryContainer = Color(0xFFEFF6FF) // Soft Ice Blue
val ModernOnPrimaryContainer = Color(0xFF1E3A8A) // Deep Royal Navy

val ModernSecondary = Color(0xFF64748B) // Slate
val ModernSecondaryContainer = Color(0xFFF1F5F9)
val ModernOnSecondaryContainer = Color(0xFF0F172A)

val ModernBackgroundLight = Color(0xFFF8FAFC) // Crisp Snow Slate
val ModernSurfaceLight = Color(0xFFFFFFFF)
val ModernSurfaceVariantLight = Color(0xFFF1F5F9) // Clean elevated card
val ModernOnSurfaceLight = Color(0xFF0F172A) // Rich deep charcoal
val ModernOnSurfaceVariantLight = Color(0xFF475569)
val ModernOutlineLight = Color(0xFFE2E8F0)
val ModernOutlineBorder = Color(0xFFBFDBFE)

// Modern Dark Theme (Obsidian & Titanium Slate)
val ModernBackgroundDark = Color(0xFF0B0F19)
val ModernSurfaceDark = Color(0xFF131B2E)
val ModernSurfaceVariantDark = Color(0xFF1E293B)
val ModernOnSurfaceDark = Color(0xFFF8FAFC)
val ModernOnSurfaceVariantDark = Color(0xFF94A3B8)
val ModernOutlineDark = Color(0xFF334155)

// Status & Action Colors
val CallGreen = Color(0xFF10B981) // Modern Emerald
val CallGreenLight = Color(0xFF34D399)
val CallGreenContainer = Color(0xFFECFDF5)
val CallOnGreenContainer = Color(0xFF064E3B)

val CallRed = Color(0xFFEF4444) // Modern Coral Crimson
val CallRedLight = Color(0xFFF87171)
val CallRedContainer = Color(0xFFFEF2F2)
val CallOnRedContainer = Color(0xFF7F1D1D)

val CallHoldAmber = Color(0xFFF59E0B) // Radiant Amber
val CallHoldAmberContainer = Color(0xFFFFFBEB)

val InfoContainer = Color(0xFFEFF6FF)
val OnInfoContainer = Color(0xFF1E40AF)

val KeypadButtonBgLight = Color(0xFFF8FAFC)
val KeypadButtonBorderLight = Color(0xFFE2E8F0)
val KeypadButtonBgDark = Color(0xFF1E293B)
val KeypadButtonBorderDark = Color(0xFF334155)

// Gradients
val PrimaryGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFF2563EB), Color(0xFF4F46E5))
)

val CallGreenGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF10B981), Color(0xFF059669))
)

val CallRedGradient = Brush.linearGradient(
    colors = listOf(Color(0xFFEF4444), Color(0xFFDC2626))
)

val SurfaceGlassGradientLight = Brush.verticalGradient(
    colors = listOf(Color(0xFFFFFFFF), Color(0xFFF8FAFC))
)



