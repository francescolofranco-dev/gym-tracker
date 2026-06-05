package dev.francescolofranco.gymtracker.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/*
 * Brand corner radii from the Gym Tracker design system (colors_and_type.css -> --radius-*).
 *
 * Most surfaces clip their own corners inline (cards 16dp, timer pill 20dp, set-row chips 14dp,
 * numpad keys 16dp), so the only radii pinned here are the two places where Material 3's
 * component defaults disagree with the brand:
 *   - Filled/outlined buttons default to a full stadium; the brand uses 16dp (--radius-lg).
 *   - AlertDialogs default to 28dp (extraLarge); the brand uses 16dp (--radius-lg).
 *
 * Modal bottom sheets are intentionally left at the Material default 28dp, which already matches
 * the design (.sheet { border-radius: 28px 28px 0 0 }).
 */

/** 16dp brand radius for buttons (design token --radius-lg). */
val ButtonShape = RoundedCornerShape(16.dp)

/** 16dp brand radius for dialogs (design token --radius-lg). */
val DialogShape = RoundedCornerShape(16.dp)
