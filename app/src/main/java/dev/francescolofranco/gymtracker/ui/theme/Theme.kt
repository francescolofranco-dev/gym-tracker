package dev.francescolofranco.gymtracker.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat
import kotlin.math.roundToInt

const val UI_ZOOM_SCALE = 1.05f

internal fun scaledDensity(baseDensity: Float, scale: Float = UI_ZOOM_SCALE): Float =
    baseDensity * scale

internal fun scaledDensityDpi(baseDensityDpi: Int, scale: Float = UI_ZOOM_SCALE): Int =
    (baseDensityDpi * scale).roundToInt()

private class ZoomedContext(
    base: Context,
    val unscaledDensity: Float,
    val unscaledDensityDpi: Int,
) : ContextWrapper(base)

private tailrec fun Context.findZoomedContext(): ZoomedContext? = when (this) {
    is ZoomedContext -> this
    is ContextWrapper -> baseContext?.findZoomedContext()
    else -> null
}

fun Context.withUiZoom(): Context {
    val existing = findZoomedContext()
    val unscaledDensity = existing?.unscaledDensity ?: resources.displayMetrics.density
    val unscaledDensityDpi = existing?.unscaledDensityDpi ?: resources.configuration.densityDpi
    val config = Configuration(resources.configuration).apply {
        densityDpi = scaledDensityDpi(unscaledDensityDpi)
    }
    return ZoomedContext(createConfigurationContext(config), unscaledDensity, unscaledDensityDpi)
}

private val DarkColors = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceContainerHighest,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
)

/** Fixed dark editorial identity, independent of wallpaper and system appearance. */
@Composable
fun OgmaTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            (view.context as? Activity)?.window?.let { window ->
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = false
                    isAppearanceLightNavigationBars = false
                }
            }
        }
    }
    val baseDensity = LocalDensity.current
    val unscaledDensity = view.context.findZoomedContext()?.unscaledDensity ?: baseDensity.density
    val zoomedDensity = remember(unscaledDensity, baseDensity.fontScale) {
        Density(
            density = scaledDensity(unscaledDensity),
            fontScale = baseDensity.fontScale,
        )
    }
    CompositionLocalProvider(LocalDensity provides zoomedDensity) {
        MaterialTheme(
            colorScheme = DarkColors,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
