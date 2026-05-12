package dev.francescolofranco.gymtracker.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import dev.francescolofranco.gymtracker.MainActivity
import dev.francescolofranco.gymtracker.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Foreground service that holds the workout timer state. State is process-wide static so the
 * QS tile, in-app pill, and notification all observe the same flow without service binding.
 *
 * Reset: from any state, snaps to RUNNING(now). Stop: snaps to STOPPED but the service stays
 * alive (the spec wants the notification visible at 00:00 rather than disappearing).
 */
class TimerService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_RESET -> _state.value = TimerState.runningNow()
            ACTION_STOP -> _state.value = TimerState.Stopped
            // Any other action (or null) just ensures we are running with current state.
        }
        startInForeground(_state.value)
        return START_STICKY
    }

    private fun startInForeground(state: TimerState) {
        val notification = buildNotification(state)
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
        )
    }

    private fun buildNotification(state: TimerState): Notification {
        ensureChannel(this)

        val openAppPi = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val resetPi = pendingIntent(this, ACTION_RESET, REQ_RESET)
        val stopPi = pendingIntent(this, ACTION_STOP, REQ_STOP)

        val customView = buildTimerView(state)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.timer_title))
            .setContentIntent(openAppPi)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setSilent(true)
            .setCustomContentView(customView)
            .setCustomBigContentView(customView)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .addAction(0, getString(R.string.timer_reset), resetPi)
            .addAction(0, getString(R.string.timer_stop), stopPi)
        return builder.build()
    }

    /**
     * Builds the custom RemoteViews layout that renders the timer prominently inside the
     * notification body. The Chronometer ticks autonomously once shown, so we don't have to
     * re-post the notification every second.
     */
    private fun buildTimerView(state: TimerState): RemoteViews {
        val rv = RemoteViews(packageName, R.layout.notification_timer)
        when (state) {
            is TimerState.Running -> {
                rv.setChronometer(R.id.timerChronometer, state.baseElapsedRealtime, null, true)
                rv.setViewVisibility(R.id.timerChronometer, View.VISIBLE)
                rv.setViewVisibility(R.id.timerStopped, View.GONE)
                rv.setTextViewText(R.id.timerStateText, getString(R.string.timer_running))
            }
            TimerState.Stopped -> {
                // Freeze the chronometer at 00:00 by setting started=false and hiding it.
                rv.setChronometer(R.id.timerChronometer, SystemClock.elapsedRealtime(), null, false)
                rv.setViewVisibility(R.id.timerChronometer, View.GONE)
                rv.setViewVisibility(R.id.timerStopped, View.VISIBLE)
                rv.setTextViewText(R.id.timerStateText, getString(R.string.timer_stopped))
            }
        }
        return rv
    }

    companion object {
        const val ACTION_RESET = "dev.francescolofranco.gymtracker.timer.RESET"
        const val ACTION_STOP = "dev.francescolofranco.gymtracker.timer.STOP"
        const val ACTION_ENSURE = "dev.francescolofranco.gymtracker.timer.ENSURE"

        const val CHANNEL_ID = "gym-timer"
        const val NOTIFICATION_ID = 1001
        private const val REQ_RESET = 11
        private const val REQ_STOP = 12

        private val _state = MutableStateFlow<TimerState>(TimerState.Stopped)
        val state: StateFlow<TimerState> = _state.asStateFlow()

        fun send(ctx: Context, action: String) {
            val intent = Intent(ctx, TimerService::class.java).setAction(action)
            ContextCompat.startForegroundService(ctx, intent)
        }

        private fun pendingIntent(ctx: Context, action: String, reqCode: Int): PendingIntent {
            val intent = Intent(ctx, TimerService::class.java).setAction(action)
            return PendingIntent.getForegroundService(
                ctx,
                reqCode,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }

        fun ensureChannel(ctx: Context) {
            val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    ctx.getString(R.string.timer_channel_name),
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = ctx.getString(R.string.timer_channel_desc)
                    setShowBadge(false)
                }
                nm.createNotificationChannel(channel)
            }
        }
    }
}
