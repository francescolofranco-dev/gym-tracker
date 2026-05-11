package dev.francescolofranco.gymtracker.ui.theme

import androidx.compose.ui.graphics.Color

// Volume traffic-light palette (the 3-10 sets/week rule visualisation). These stay punchy
// across both light and dark schemes — they have to read at a glance on the body diagram.
val VolumeGrey = Color(0xFF6B7280)   // 0 sets — untrained
val VolumeBlue = Color(0xFF3B82F6)   // 1-2 sets — under
val VolumeGreen = Color(0xFF22C55E)  // 3-10 sets — in range
val VolumeRed = Color(0xFFEF4444)    // 11+ sets — over

// ─── Brand accent ─────────────────────────────────────────────────────────────────────────
// Vibrant orange — energetic, distinct from the volume traffic-light so the primary action
// (Start session, Backup now, ✓) never clashes with the muscle heatmap.
val BrandOrange = Color(0xFFFF6B35)
val BrandOrangeDeep = Color(0xFFC2410C)

// ─── Dark scheme ──────────────────────────────────────────────────────────────────────────
val DarkPrimary = BrandOrange
val DarkOnPrimary = Color(0xFF2C0900)
val DarkPrimaryContainer = Color(0xFF5D2300)
val DarkOnPrimaryContainer = Color(0xFFFFDBC9)

val DarkSecondary = Color(0xFF6EE7B7)
val DarkOnSecondary = Color(0xFF003827)
val DarkSecondaryContainer = Color(0xFF005238)
val DarkOnSecondaryContainer = Color(0xFFB5F1D3)

val DarkTertiary = Color(0xFF8AB4F8)
val DarkOnTertiary = Color(0xFF0A2347)
val DarkTertiaryContainer = Color(0xFF1F3A66)
val DarkOnTertiaryContainer = Color(0xFFD6E3FF)

val DarkBackground = Color(0xFF0E1014)
val DarkSurface = Color(0xFF15181C)
val DarkSurfaceVariant = Color(0xFF2A2F36)
val DarkOnSurface = Color(0xFFE7E9ED)
val DarkOnSurfaceVariant = Color(0xFFBFC4CC)
val DarkSurfaceContainer = Color(0xFF1B1F24)
val DarkSurfaceContainerHigh = Color(0xFF22272D)
val DarkSurfaceContainerHighest = Color(0xFF2A2F36)
val DarkOutline = Color(0xFF565C66)
val DarkOutlineVariant = Color(0xFF3A4049)

// ─── Light scheme ─────────────────────────────────────────────────────────────────────────
val LightPrimary = BrandOrangeDeep
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFFFDBC9)
val LightOnPrimaryContainer = Color(0xFF380C00)

val LightSecondary = Color(0xFF047857)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFB5F1D3)
val LightOnSecondaryContainer = Color(0xFF002218)

val LightTertiary = Color(0xFF1E3A8A)
val LightOnTertiary = Color(0xFFFFFFFF)
val LightTertiaryContainer = Color(0xFFD6E3FF)
val LightOnTertiaryContainer = Color(0xFF0A2347)

val LightBackground = Color(0xFFFAFAFA)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF0F0F2)
val LightOnSurface = Color(0xFF15181C)
val LightOnSurfaceVariant = Color(0xFF44474C)
val LightSurfaceContainer = Color(0xFFF4F4F6)
val LightSurfaceContainerHigh = Color(0xFFECECEF)
val LightSurfaceContainerHighest = Color(0xFFE3E4E8)
val LightOutline = Color(0xFF74787F)
val LightOutlineVariant = Color(0xFFC7C9CE)
