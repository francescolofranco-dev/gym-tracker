package dev.francescolofranco.gymtracker.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay

/** Distinguishes "not loaded yet" from a real empty or missing database result. */
sealed interface Loadable<out T> {
    data object Loading : Loadable<Nothing>
    data class Ready<T>(val value: T) : Loadable<T>
}

/**
 * Keeps the screen bounds stable while data loads. The spinner is delayed so a normal fast Room
 * emission transitions straight to content instead of flashing a progress indicator for one frame.
 */
@Composable
fun LoadingPane(
    modifier: Modifier = Modifier,
    spinnerDelayMillis: Long = 180L,
) {
    var showSpinner by remember { mutableStateOf(false) }
    LaunchedEffect(spinnerDelayMillis) {
        delay(spinnerDelayMillis)
        showSpinner = true
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (showSpinner) CircularProgressIndicator()
    }
}
