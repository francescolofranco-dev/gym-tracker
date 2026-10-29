package dev.francescolofranco.gymtracker.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Strong editorial headings; body and numeric controls retain familiar, readable proportions.
private fun type(size: Int, weight: FontWeight, tracking: Double = 0.0) = TextStyle(
    fontFamily = FontFamily.SansSerif, fontWeight = weight,
    fontSize = size.sp, letterSpacing = tracking.sp,
)
val AppTypography = Typography(
    displayLarge = type(52, FontWeight.ExtraBold, -2.0),
    displayMedium = type(44, FontWeight.ExtraBold, -1.5),
    displaySmall = type(36, FontWeight.ExtraBold, -1.0),
    headlineLarge = type(32, FontWeight.ExtraBold, -1.0),
    headlineMedium = type(28, FontWeight.Bold, -0.7),
    headlineSmall = type(24, FontWeight.Bold, -0.5),
    titleLarge = type(22, FontWeight.Bold, -0.4),
    titleMedium = type(16, FontWeight.SemiBold, -0.2),
    titleSmall = type(14, FontWeight.SemiBold),
    bodyLarge = type(16, FontWeight.Normal),
    bodyMedium = type(14, FontWeight.Normal),
    bodySmall = type(12, FontWeight.Normal),
    labelLarge = type(14, FontWeight.SemiBold, 0.2),
    labelMedium = type(12, FontWeight.Medium, 0.8),
    labelSmall = type(11, FontWeight.Medium, 0.6),
)
