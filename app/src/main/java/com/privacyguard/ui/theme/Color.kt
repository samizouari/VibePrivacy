package com.privacyguard.ui.theme

import androidx.compose.ui.graphics.Color

// Palette sobre et moderne selon PROJECT_CONTEXT.md
// Noir, blanc, gris + accent bleu pour privacy

// Couleurs principales - Light Theme
val PrimaryBlue = Color(0xFF2196F3)  // Bleu pour privacy
val PrimaryBlueVariant = Color(0xFF1976D2)
val SecondaryGray = Color(0xFF757575)
val BackgroundLight = Color(0xFFFFFFFF)
val SurfaceLight = Color(0xFFF5F5F5)
val OnPrimaryLight = Color(0xFFFFFFFF)
val OnBackgroundLight = Color(0xFF000000)
val OnSurfaceLight = Color(0xFF1C1B1F)

// Couleurs principales - Dark Theme
val PrimaryBlueDark = Color(0xFF90CAF9)  // Bleu plus clair pour dark mode
val PrimaryBlueDarkVariant = Color(0xFF42A5F5)
val SecondaryGrayDark = Color(0xFFBDBDBD)
val BackgroundDark = Color(0xFF121212)
val SurfaceDark = Color(0xFF1E1E1E)
val OnPrimaryDark = Color(0xFF000000)
val OnBackgroundDark = Color(0xFFE1E1E1)
val OnSurfaceDark = Color(0xFFE1E1E1)

// Couleurs d'état (pour indicateur privacy)
val SafeGreen = Color(0xFF4CAF50)
val WarningYellow = Color(0xFFFFC107)
val DangerRed = Color(0xFFF44336)

// Couleurs de menace (pour dashboard)
val ThreatLow = Color(0xFF81C784)
val ThreatMedium = Color(0xFFFFB74D)
val ThreatHigh = Color(0xFFE57373)
val ThreatCritical = Color(0xFFF44336)
