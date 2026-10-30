package dev.francescolofranco.gymtracker.ui.screens.sessions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.francescolofranco.gymtracker.service.TimerState
import dev.francescolofranco.gymtracker.ui.theme.BrandLime
import dev.francescolofranco.gymtracker.ui.theme.EditorialBorder
import dev.francescolofranco.gymtracker.ui.theme.MutedOlive
import dev.francescolofranco.gymtracker.ui.theme.SurfaceCard
import kotlinx.coroutines.delay
import java.util.Locale

/**
 * Prominent timer header / metric tile for the active session screen. Tap to reset, mirrors the
 * foreground service's state, big enough to read across the room while you're mid-set.
 */
@Composable
fun TimerPill(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    viewModel: TimerPillViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val running = state is TimerState.Running
    val elapsedMs = rememberElapsed(state)

    val borderColor = if (running) BrandLime else EditorialBorder
    val valueColor = if (running) BrandLime else MaterialTheme.colorScheme.onSurface
    val clickModifier = if (running) Modifier.clickable { viewModel.reset() } else Modifier

    if (compact) {
        Column(
            modifier = modifier
                .clip(RoundedCornerShape(2.dp))
                .background(SurfaceCard)
                .border(1.dp, borderColor, RoundedCornerShape(2.dp))
                .then(clickModifier)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (running) BrandLime else MutedOlive),
                )
                Text(
                    text = if (running) "ELAPSED" else "REST TIMER",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        letterSpacing = 0.72.sp,
                    ),
                    color = MutedOlive,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = formatElapsed(elapsedMs),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = valueColor,
                )
                if (running) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Reset timer",
                        tint = BrandLime,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
        return
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(SurfaceCard)
            .border(1.dp, borderColor, RoundedCornerShape(4.dp))
            .then(clickModifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Timer,
            contentDescription = null,
            tint = valueColor,
            modifier = Modifier.size(28.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = formatElapsed(elapsedMs),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                ),
                color = valueColor,
            )
            Text(
                text = if (running) "RUNNING · TAP TO RESET"
                else "STARTS ON YOUR FIRST ✓",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    letterSpacing = 0.8.sp,
                ),
                color = MutedOlive,
            )
        }
        if (running) {
            Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = "Reset timer",
                tint = valueColor,
                modifier = Modifier.size(24.dp),
            )
        }
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
