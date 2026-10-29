package dev.francescolofranco.gymtracker.service

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.Chronometer
import dev.francescolofranco.gymtracker.R
import kotlin.math.abs
import kotlin.math.roundToInt

/** A small service-owned window; the platform Chronometer ticks without a polling coroutine. */
internal class TimerOverlay(private val context: Context) {
    private val windowManager = context.getSystemService(WindowManager::class.java)
    private var view: Chronometer? = null
    // Physical right edge, regardless of the language direction.
    @SuppressLint("RtlHardcoded")
    private val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.RIGHT
        x = dp(12)
        y = dp(8)
        title = context.getString(R.string.timer_title)
    }

    fun show(state: TimerState.Running) {
        if (!Settings.canDrawOverlays(context)) {
            hide()
            return
        }
        if (view == null) {
            val timer = Chronometer(context).apply {
                textSize = 18f
                setTextColor(Color.WHITE)
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                includeFontPadding = false
                setShadowLayer(dp(2).toFloat(), 0f, 0f, Color.BLACK)
                gravity = Gravity.CENTER
                setPadding(dp(6), dp(4), dp(6), dp(4))
                contentDescription = context.getString(R.string.timer_overlay_drag)
            }
            enableDragging(timer)
            try {
                windowManager.addView(timer, params)
                view = timer
                timer.post { reposition() }
            } catch (_: SecurityException) {
                // Permission can be revoked between the check and addView. The notification
                // and in-app timer remain usable without the optional floating window.
                return
            }
        }
        view?.apply {
            base = state.baseElapsedRealtime
            start()
        }
    }

    fun hide() {
        view?.stop()
        view?.let { windowManager.removeView(it) }
        view = null
    }

    fun reposition() {
        val panel = view ?: return
        val bounds = windowManager.currentWindowMetrics.bounds
        params.x = params.x.coerceIn(0, (bounds.width() - panel.width).coerceAtLeast(0))
        params.y = params.y.coerceIn(0, (bounds.height() - panel.height).coerceAtLeast(0))
        try {
            windowManager.updateViewLayout(panel, params)
        } catch (_: SecurityException) {
            hide()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun enableDragging(timer: Chronometer) {
        var downX = 0f
        var downY = 0f
        var startX = 0
        var startY = 0
        var dragging = false
        val slop = ViewConfiguration.get(context).scaledTouchSlop
        timer.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    startX = params.x
                    startY = params.y
                    dragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downX
                    val dy = event.rawY - downY
                    if (abs(dx) > slop || abs(dy) > slop) dragging = true
                    if (dragging) {
                        // Right-anchored windows use the opposite sign for horizontal movement.
                        params.x = startX - dx.roundToInt()
                        params.y = startY + dy.roundToInt()
                        reposition()
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> true
                else -> false
            }
        }
    }

    private fun dp(value: Int) = (value * context.resources.displayMetrics.density).roundToInt()
}
