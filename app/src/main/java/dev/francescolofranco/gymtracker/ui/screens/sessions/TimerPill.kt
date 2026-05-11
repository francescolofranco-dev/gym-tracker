package dev.francescolofranco.gymtracker.ui.screens.sessions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.francescolofranco.gymtracker.service.TimerState
import kotlinx.coroutines.delay
import java.util.Locale

/**
 * Prominent timer header for the active session screen. Tap to reset, mirrors the foreground
 * service's state, big enough to read across the room while you're mid-set.
 */
@Composable
fun TimerPill(viewModel: TimerPillViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val running = state is TimerState.Running
    val elapsedMs = rememberElapsed(state)

    val (bg, fg) = if (running) {
        MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh to MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .clickable { viewModel.tap() }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Timer,
            contentDescription = null,
            tint = fg,
            modifier = Modifier.size(32.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = formatElapsed(elapsedMs),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 40.sp,
                ),
                color = fg,
            )
            Text(
                text = if (running) "Running · tap to reset" else "Stopped · tap to start",
                style = MaterialTheme.typography.labelMedium,
                color = fg.copy(alpha = 0.75f),
            )
        }
        Icon(
            imageVector = Icons.Filled.Refresh,
            contentDescription = "Reset timer",
            tint = fg,
            modifier = Modifier.size(28.dp),
        )
    }
}

@Composable
private fun rememberElapsed(state: TimerState): Long {
    var now by remember { mutableLongStateOf(android.os.SystemClock.elapsedRealtime()) }
    LaunchedEffect(state) {
        while (state is TimerState.Running) {
            now = android.os.SystemClock.elapsedRealtime()
            delay(1_000)
        }
        // Bounce once when stopped so the pill snaps to 00:00.
        now = android.os.SystemClock.elapsedRealtime()
    }
    return state.elapsedMillis(now)
}

private fun formatElapsed(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
    else String.format(Locale.US, "%02d:%02d", m, s)
}
