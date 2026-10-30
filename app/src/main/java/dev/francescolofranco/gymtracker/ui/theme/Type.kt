package dev.francescolofranco.gymtracker.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Strong editorial headings; body and numeric controls retain familiar, readable proportions.
private fun type(
    size: Int,
    weight: FontWeight,
    tracking: Double = 0.0,
    tabular: Boolean = false,
) = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = weight,
    fontSize = size.sp,
    letterSpacing = tracking.sp,
    fontFeatureSettings = if (tabular) "tnum" else null,
)

val AppTypography = Typography(
    displayLarge = type(52, FontWeight.ExtraBold, -1.6, tabular = true),
    displayMedium = type(44, FontWeight.ExtraBold, -1.2, tabular = true),
    displaySmall = type(36, FontWeight.ExtraBold, -0.8, tabular = true),
    headlineLarge = type(32, FontWeight.Bold, 0.8),
    headlineMedium = type(28, FontWeight.Bold, 0.4),
    headlineSmall = type(24, FontWeight.Bold, 0.4),
    titleLarge = type(20, FontWeight.Bold, 0.4),
    titleMedium = type(16, FontWeight.SemiBold, 0.1),
    titleSmall = type(14, FontWeight.SemiBold, 0.2),
    bodyLarge = type(16, FontWeight.Normal),
    bodyMedium = type(14, FontWeight.Normal),
    bodySmall = type(12, FontWeight.Normal, 0.1),
    labelLarge = type(13, FontWeight.Bold, 0.6, tabular = true),
    labelMedium = type(11, FontWeight.Bold, 0.8, tabular = true),
    labelSmall = type(10, FontWeight.Bold, 0.8, tabular = true),
)
