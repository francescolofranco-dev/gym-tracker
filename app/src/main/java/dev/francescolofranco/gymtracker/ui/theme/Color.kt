package dev.francescolofranco.gymtracker.ui.theme

import androidx.compose.ui.graphics.Color

// Volume traffic-light palette (the 3-10 sets/week rule visualisation). These stay punchy
// across both light and dark schemes — they have to read at a glance on the body diagram.
val VolumeGrey = Color(0xFF6B7280)   // 0 sets — untrained
val VolumeBlue = Color(0xFF3B82F6)   // 1-2 sets — under
val VolumeGreen = Color(0xFF22C55E)  // 3-10 sets — in range
val VolumeRed = Color(0xFFEF4444)    // 11+ sets — over

// Editorial lime is reserved for actions and selection. Volume categories retain their own colors.
val BrandLime = Color(0xFFDBFF51)
val RegressionAmber = Color(0xFFF59E0B)

val DarkPrimary = BrandLime
val DarkOnPrimary = Color(0xFF182107)
val DarkPrimaryContainer = Color(0xFF303B19)
val DarkOnPrimaryContainer = Color(0xFFE0F5AE)
val DarkSecondary = Color(0xFFC2CFAB)
val DarkOnSecondary = Color(0xFF202819)
val DarkSecondaryContainer = Color(0xFF262D20)
val DarkOnSecondaryContainer = Color(0xFFDBE5CD)
val DarkTertiary = Color(0xFFABC5D1)
val DarkOnTertiary = Color(0xFF132B35)
val DarkTertiaryContainer = Color(0xFF253A43)
val DarkOnTertiaryContainer = Color(0xFFD1E7F0)
val DarkBackground = Color(0xFF0C0E0C)
val DarkSurface = Color(0xFF111410)
val DarkSurfaceVariant = Color(0xFF23271F)
val DarkOnSurface = Color(0xFFF2F2EC)
val DarkOnSurfaceVariant = Color(0xFFB0B5A7)
val DarkSurfaceContainer = Color(0xFF171A15)
val DarkSurfaceContainerHigh = Color(0xFF20251C)
val DarkSurfaceContainerHighest = Color(0xFF2A3024)
val DarkOutline = Color(0xFF747D69)
val DarkOutlineVariant = Color(0xFF343C2D)
