package dev.francescolofranco.gymtracker.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.francescolofranco.gymtracker.R

@OptIn(ExperimentalTextApi::class)
private fun variableFontFamily(resId: Int, weights: List<FontWeight>): FontFamily = FontFamily(
    weights.map { weight ->
        Font(
            resId = resId,
            weight = weight,
            variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
        )
    },
)

val OswaldFontFamily: FontFamily = variableFontFamily(
    resId = R.font.oswald,
    weights = listOf(FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold),
)

val SpaceGroteskFontFamily: FontFamily = variableFontFamily(
    resId = R.font.space_grotesk,
    weights = listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold),
)

// Strong editorial headings (Oswald); technical metrics, labels, and body (Space Grotesk).
private fun type(
    size: Int,
    weight: FontWeight,
    tracking: Double = 0.0,
    tabular: Boolean = false,
    family: FontFamily = SpaceGroteskFontFamily,
) = TextStyle(
    fontFamily = family,
    fontWeight = weight,
    fontSize = size.sp,
    letterSpacing = tracking.sp,
    fontFeatureSettings = if (tabular) "tnum" else null,
)

val AppTypography = Typography(
    displayLarge = type(52, FontWeight.Bold, -1.6, tabular = true, family = SpaceGroteskFontFamily),
    displayMedium = type(44, FontWeight.Bold, -1.2, tabular = true, family = SpaceGroteskFontFamily),
    displaySmall = type(36, FontWeight.Bold, -0.8, tabular = true, family = SpaceGroteskFontFamily),
    headlineLarge = type(32, FontWeight.Bold, 0.8, family = OswaldFontFamily),
    headlineMedium = type(28, FontWeight.SemiBold, 0.4, family = OswaldFontFamily),
    headlineSmall = type(24, FontWeight.SemiBold, 0.4, family = OswaldFontFamily),
    titleLarge = type(20, FontWeight.SemiBold, 0.4, family = OswaldFontFamily),
    titleMedium = type(16, FontWeight.SemiBold, 0.1, family = SpaceGroteskFontFamily),
    titleSmall = type(14, FontWeight.SemiBold, 0.2, family = SpaceGroteskFontFamily),
    bodyLarge = type(16, FontWeight.Normal),
    bodyMedium = type(14, FontWeight.Normal),
    bodySmall = type(12, FontWeight.Normal, 0.1),
    labelLarge = type(13, FontWeight.Bold, 0.6, tabular = true, family = SpaceGroteskFontFamily),
    labelMedium = type(11, FontWeight.Bold, 0.8, tabular = true, family = SpaceGroteskFontFamily),
    labelSmall = type(10, FontWeight.Bold, 0.8, tabular = true, family = SpaceGroteskFontFamily),
)
