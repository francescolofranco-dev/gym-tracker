package dev.francescolofranco.gymtracker.ui.theme

import androidx.compose.ui.graphics.Color

// Volume traffic-light palette (the 3-10 sets/week rule visualisation). These stay punchy
// across both light and dark schemes — they have to read at a glance on the body diagram.
val VolumeGrey = Color(0xFF6B7280)   // 0 sets — untrained
val VolumeBlue = Color(0xFF3B82F6)   // 1-2 sets — under
val VolumeGreen = Color(0xFF22C55E)  // 3-10 sets — in range
val VolumeRed = Color(0xFFEF4444)    // 11+ sets — over

// Editorial lime is reserved for actions and selection. Volume categories retain their own colors.
val BrandLime = Color(0xFFC8F323)
val RegressionAmber = Color(0xFFF59E0B)
val DestructiveCoral = Color(0xFFFFB4AB)

// Tactical dark surfaces & borders from Figma design system
val SurfaceDeep = Color(0xFF0D0E10)
val SurfaceBase = Color(0xFF121315)
val SurfaceCard = Color(0xFF1B1C1E)
val SurfaceElevated = Color(0xFF1F2022)
val SurfaceControl = Color(0xFF292A2C)
val SurfaceChip = Color(0xFF343537)

val EditorialBorder = Color(0xFF444934)
val SubtleBorder = Color(0xFF292A2C)
val MutedOlive = Color(0xFF8F937A)
val OliveSubtext = Color(0xFFC5C9AD)
val SlateSubtext = Color(0xFFC0C6D7)

val DarkPrimary = BrandLime
val DarkOnPrimary = Color(0xFF171E00)
val DarkPrimaryContainer = Color(0xFF263000)
val DarkOnPrimaryContainer = Color(0xFFC8F323)
val DarkSecondary = Color(0xFFC0C6D7)
val DarkOnSecondary = Color(0xFF1B1C1E)
val DarkSecondaryContainer = Color(0xFF292A2C)
val DarkOnSecondaryContainer = Color(0xFFE3E2E5)
val DarkTertiary = Color(0xFFABC5D1)
val DarkOnTertiary = Color(0xFF132B35)
val DarkTertiaryContainer = Color(0xFF253A43)
val DarkOnTertiaryContainer = Color(0xFFD1E7F0)
val DarkBackground = SurfaceDeep
val DarkSurface = SurfaceBase
val DarkSurfaceVariant = SurfaceChip
val DarkOnSurface = Color(0xFFF4F5F7)
val DarkOnSurfaceVariant = OliveSubtext
val DarkSurfaceContainer = SurfaceCard
val DarkSurfaceContainerHigh = SurfaceElevated
val DarkSurfaceContainerHighest = SurfaceControl
val DarkOutline = MutedOlive
val DarkOutlineVariant = EditorialBorder
