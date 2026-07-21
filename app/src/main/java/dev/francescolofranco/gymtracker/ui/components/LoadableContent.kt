package dev.francescolofranco.gymtracker.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import dev.francescolofranco.gymtracker.ui.motion.GymMotion

/** Distinguishes "not loaded yet" from a real empty or missing database result. */
sealed interface Loadable<out T> {
    data object Loading : Loadable<Nothing>
    data class Ready<T>(val value: T) : Loadable<T>
    data class Error(val message: String) : Loadable<Nothing>
}

/** Base for Room-backed screens that can re-subscribe after a load failure. */
abstract class RetryableViewModel : ViewModel() {
    private val retryRequests = MutableStateFlow(0)

    fun retry() = retryRequests.update { it + 1 }

    @OptIn(ExperimentalCoroutinesApi::class)
    protected fun <T> Flow<T>.asLoadableState(scope: CoroutineScope): StateFlow<Loadable<T>> =
        retryRequests.flatMapLatest {
            this@asLoadableState
                .map<T, Loadable<T>> { Loadable.Ready(it) }
                .catch { throwable ->
                    emit(Loadable.Error(throwable.message ?: "Something went wrong while loading data."))
                }
        }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), Loadable.Loading)
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
        AnimatedVisibility(
            visible = showSpinner,
            enter = fadeIn(tween(GymMotion.Standard, easing = GymMotion.EmphasizedEasing)) +
                scaleIn(
                    initialScale = 0.86f,
                    animationSpec = tween(GymMotion.Standard, easing = GymMotion.EmphasizedEasing),
                ),
            exit = fadeOut(tween(GymMotion.Quick, easing = GymMotion.ExitEasing)) +
                scaleOut(
                    targetScale = 0.92f,
                    animationSpec = tween(GymMotion.Quick, easing = GymMotion.ExitEasing),
                ),
        ) {
            CircularProgressIndicator()
        }
    }
}

@Composable
fun ErrorPane(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Unable to load this screen", style = MaterialTheme.typography.titleLarge)
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onRetry) { Text("Retry") }
        }
    }
}
