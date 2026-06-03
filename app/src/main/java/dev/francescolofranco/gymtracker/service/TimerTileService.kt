package dev.francescolofranco.gymtracker.service

import android.graphics.drawable.Icon
import android.os.SystemClock
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import dev.francescolofranco.gymtracker.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Quick-Settings tile for the gym timer. A single tap resets the timer (== starts a fresh
 * count) regardless of whether anything was previously running, matching the spec.
 *
 * The label updates while the tile sheet is visible so the user gets a live count.
 */
class TimerTileService : TileService() {

    private var scope: CoroutineScope? = null
    private var tickerJob: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        val newScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        scope = newScope
        newScope.launch {
            TimerService.state.collectLatest { state ->
                refresh(state)
                tickerJob?.cancel()
                tickerJob = if (state is TimerState.Running) {
                    launch {
                        while (true) {
                            delay(1_000)
                            refresh(state)
                        }
                    }
                } else null
            }
        }
    }

    override fun onStopListening() {
        scope?.cancel()
        scope = null
        tickerJob = null
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        TimerService.send(applicationContext, TimerService.ACTION_RESET)
    }

    private fun refresh(state: TimerState) {
        val tile = qsTile ?: return
        tile.icon = Icon.createWithResource(applicationContext, R.drawable.ic_launcher_monochrome)
        tile.label = getString(R.string.tile_timer_label)
        tile.contentDescription = tile.label
        tile.state = if (state.isRunning) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.subtitle = formatElapsed(state.elapsedMillis(SystemClock.elapsedRealtime()))
        tile.updateTile()
    }

    private fun formatElapsed(ms: Long): String {
        val totalSeconds = ms / 1000
        val m = totalSeconds / 60
        val s = totalSeconds % 60
        return "%02d:%02d".format(m, s)
    }
}
