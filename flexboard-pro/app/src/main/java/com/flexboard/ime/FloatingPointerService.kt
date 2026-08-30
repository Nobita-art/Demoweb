package com.flexboard.ime

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import com.flexboard.utils.SettingsStore

/**
 * Draws a small orange floating dot overlay that the user can drag over the SEND button
 * of any chat app. The position is saved to SharedPreferences. AutoTypeEngine then uses
 * AccessibilityService.dispatchGesture() to "tap" that exact (x,y) after each typed line.
 *
 * - When LOCKED: dot is non-touchable (pointer events pass through to the app underneath)
 *   so it does not interfere with normal use.
 * - When UNLOCKED: dot is draggable, on each ACTION_UP its (x,y) is persisted.
 *
 * Requires SYSTEM_ALERT_WINDOW (android.permission.SYSTEM_ALERT_WINDOW) — must be granted
 * by user via Settings.canDrawOverlays() flow.
 */
class FloatingPointerService : Service() {

    private var wm: WindowManager? = null
    private var dot: View? = null
    private var params: WindowManager.LayoutParams? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        showOverlay()
    }

    override fun onDestroy() {
        hideOverlay()
        if (instance === this) instance = null
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_LOCK -> setLocked(true)
            ACTION_UNLOCK -> setLocked(false)
            ACTION_REFRESH -> applyLockedFlag()
        }
        return START_STICKY
    }

    private fun showOverlay() {
        if (dot != null) return
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val sizePx = dp(28)
        val container = FrameLayout(this)
        val ring = TextView(this).apply {
            text = "●"
            textSize = 22f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#FF8C00"))
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#33000000"))
                setStroke(dp(2), Color.parseColor("#FF8C00"))
            }
            alpha = 0.9f
        }
        container.addView(
            ring,
            FrameLayout.LayoutParams(sizePx, sizePx, Gravity.CENTER)
        )

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val prefs = SettingsStore.prefs(this)
        val storedX = prefs.getInt(SettingsStore.KEY_POINTER_X, -1)
        val storedY = prefs.getInt(SettingsStore.KEY_POINTER_Y, -1)
        val locked = prefs.getBoolean(SettingsStore.KEY_POINTER_LOCKED, false)

        params = WindowManager.LayoutParams(
            sizePx, sizePx,
            type,
            currentFlags(locked),
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = if (storedX >= 0) storedX else 200
            y = if (storedY >= 0) storedY else 800
        }

        // Drag handler (only effective when not locked)
        var downX = 0
        var downY = 0
        var touchX = 0f
        var touchY = 0f
        container.setOnTouchListener { _, ev ->
            val p = params ?: return@setOnTouchListener false
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = p.x; downY = p.y
                    touchX = ev.rawX; touchY = ev.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    p.x = (downX + (ev.rawX - touchX)).toInt()
                    p.y = (downY + (ev.rawY - touchY)).toInt()
                    runCatching { wm?.updateViewLayout(container, p) }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    SettingsStore.prefs(this).edit()
                        .putInt(SettingsStore.KEY_POINTER_X, p.x + dp(14))   // center of dot
                        .putInt(SettingsStore.KEY_POINTER_Y, p.y + dp(14))
                        .apply()
                    true
                }
                else -> false
            }
        }

        try {
            wm?.addView(container, params)
            dot = container
        } catch (_: Throwable) { /* overlay permission missing */ }
    }

    private fun currentFlags(locked: Boolean): Int {
        var f = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        if (locked) f = f or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        return f
    }

    private fun setLocked(locked: Boolean) {
        SettingsStore.prefs(this).edit().putBoolean(SettingsStore.KEY_POINTER_LOCKED, locked).apply()
        applyLockedFlag()
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    @Volatile private var hidingForClick = false

    /**
     * Fully detach the dot from the window stack for [durationMs] and then
     * re-attach it. Called by AutoTypeEngine right before it dispatches its
     * accessibility tap.
     *
     * Why removal (and not just FLAG_NOT_TOUCHABLE):
     *   On Android 9+ the system can still route AccessibilityService
     *   `dispatchGesture()` taps to TYPE_APPLICATION_OVERLAY windows even
     *   when those windows are flagged FLAG_NOT_TOUCHABLE. Result: the tap
     *   lands on (or is consumed by) our floating dot instead of the SEND
     *   button under it, and "auto-type line khatm hone ke baad pointer
     *   click kaam nahi krta" — exactly the bug the user reported.
     *
     *   Removing the view from WindowManager guarantees no overlay is in
     *   the way during the click. We re-add it ~[durationMs] later in the
     *   same configuration (locked/unlocked, position, etc.).
     */
    fun temporarilyHideForClick(durationMs: Long = 450L) {
        if (hidingForClick) return
        val v = dot ?: return
        val p = params ?: return
        hidingForClick = true
        // ===== Threading fix (issue: "send nahi hota") =====
        // WindowManager.removeView()/addView() MUST be called on the same
        // looper that originally added the view (the main thread). The
        // previous code called removeView() directly from the AutoTypeEngine's
        // background coroutine — Android silently threw a CalledFromWrongThread
        // exception which we swallowed in the catch block, so the dot was
        // never actually removed. The accessibility tap then landed on the
        // INVISIBLE overlay dot at (px, py) instead of the chat app's SEND
        // button → the line typed but never sent. Posting both removeView and
        // addView through mainHandler guarantees they run on the main looper.
        mainHandler.post {
            try { wm?.removeView(v) } catch (_: Throwable) {}
        }
        mainHandler.postDelayed({
            try {
                wm?.addView(v, p)
                applyLockedFlag()    // restore visibility/flags from prefs
            } catch (_: Throwable) {}
            hidingForClick = false
        }, durationMs)
    }

    private fun applyLockedFlag() {
        val v = dot ?: return
        val p = params ?: return
        val locked = SettingsStore.prefs(this).getBoolean(SettingsStore.KEY_POINTER_LOCKED, false)
        p.flags = currentFlags(locked)
        // When LOCKED → hide the dot completely (invisible to the user) but the
        // service stays alive AND the saved (x,y) is still used by AutoType's
        // accessibility click. Unlock → show again.
        if (locked) {
            v.visibility = View.INVISIBLE
            v.alpha = 0f
        } else {
            v.visibility = View.VISIBLE
            v.alpha = 0.9f
        }
        runCatching { wm?.updateViewLayout(v, p) }
    }

    private fun hideOverlay() {
        val v = dot ?: return
        runCatching { wm?.removeView(v) }
        dot = null
    }

    private fun dp(v: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics
    ).toInt()

    companion object {
        const val ACTION_LOCK = "com.flexboard.pointer.LOCK"
        const val ACTION_UNLOCK = "com.flexboard.pointer.UNLOCK"
        const val ACTION_REFRESH = "com.flexboard.pointer.REFRESH"

        @Volatile var instance: FloatingPointerService? = null

        fun start(ctx: Context) {
            ctx.startService(Intent(ctx, FloatingPointerService::class.java))
        }
        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, FloatingPointerService::class.java))
        }
        fun lock(ctx: Context) {
            val i = Intent(ctx, FloatingPointerService::class.java).setAction(ACTION_LOCK)
            ctx.startService(i)
        }
        fun unlock(ctx: Context) {
            val i = Intent(ctx, FloatingPointerService::class.java).setAction(ACTION_UNLOCK)
            ctx.startService(i)
        }

        fun isRunning(): Boolean = instance != null
    }
}
