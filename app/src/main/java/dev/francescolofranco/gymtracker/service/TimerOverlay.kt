package dev.francescolofranco.gymtracker.service

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.Button
import android.widget.Chronometer
import android.widget.LinearLayout
import dev.francescolofranco.gymtracker.R
import kotlin.math.abs
import kotlin.math.roundToInt

/** A small service-owned window; the platform Chronometer ticks without a polling coroutine. */
internal class TimerOverlay(private val context: Context) {
    private val windowManager = context.getSystemService(WindowManager::class.java)
    private var view: LinearLayout? = null
    private var chronometer: Chronometer? = null
    private val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.LEFT
        x = dp(16)
        y = dp(120)
        title = context.getString(R.string.timer_title)
    }

    fun show(state: TimerState.Running) {
        if (!Settings.canDrawOverlays(context)) {
            hide()
            return
        }
        if (view == null) {
            val timer = Chronometer(context).apply {
                textSize = 28f
                setTextColor(Color.WHITE)
                typeface = Typeface.MONOSPACE
                gravity = Gravity.CENTER
                setPadding(dp(16), dp(8), dp(16), dp(8))
                contentDescription = context.getString(R.string.timer_overlay_drag)
            }
            val panel = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(4), dp(4), dp(4), dp(4))
                background = GradientDrawable().apply {
                    setColor(Color.rgb(30, 33, 39))
                    cornerRadius = dp(20).toFloat()
                    setStroke(dp(1), Color.rgb(104, 114, 130))
                }
                addView(timer)
                addView(LinearLayout(context).apply {
                    addView(action(R.string.timer_reset, TimerService.ACTION_RESET))
                    addView(action(R.string.timer_stop, TimerService.ACTION_STOP))
                })
            }
            enableDragging(timer)
            try {
                windowManager.addView(panel, params)
                view = panel
                chronometer = timer
                panel.post { reposition() }
            } catch (_: SecurityException) {
                // Permission can be revoked between the check and addView. The notification
                // and in-app timer remain usable without the optional floating window.
                return
            }
        }
        chronometer?.apply {
            base = state.baseElapsedRealtime
            start()
        }
    }

    fun hide() {
        chronometer?.stop()
        view?.let { windowManager.removeView(it) }
        view = null
        chronometer = null
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

    private fun action(label: Int, action: String) = Button(context).apply {
        setText(label)
        isAllCaps = false
        setTextColor(Color.WHITE)
        backgroundTintList = android.content.res.ColorStateList.valueOf(Color.rgb(56, 63, 76))
        minWidth = dp(64)
        minimumWidth = dp(64)
        setOnClickListener { TimerService.send(context, action) }
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
                        params.x = startX + dx.roundToInt()
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
