package com.flexboard

import android.app.Application
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FlexboardApp : Application() {
    private var clipboardWatcher: com.flexboard.utils.ClipboardWatcher? = null

    override fun onCreate() {
        super.onCreate()
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try {
                val sw = StringWriter()
                e.printStackTrace(PrintWriter(sw))
                val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                val text = buildString {
                    append("===== FlexBoard Pro Crash =====\n")
                    append("Time: ").append(ts).append("\n")
                    append("Thread: ").append(t.name).append("\n")
                    append("Device: ").append(android.os.Build.MANUFACTURER).append(" ")
                        .append(android.os.Build.MODEL).append(" / Android ")
                        .append(android.os.Build.VERSION.RELEASE).append(" (SDK ")
                        .append(android.os.Build.VERSION.SDK_INT).append(")\n")
                    append("App: 1.0\n\n")
                    append(sw.toString())
                }
                Log.e("FlexBoardCrash", text)
                runCatching {
                    val dir = File(filesDir, "crash").apply { mkdirs() }
                    File(dir, "last_crash.txt").writeText(text)
                    File(dir, "crash_${System.currentTimeMillis()}.txt").writeText(text)
                }
                runCatching {
                    val ext = getExternalFilesDir(null)
                    if (ext != null) {
                        File(ext, "FlexBoard_last_crash.txt").writeText(text)
                    }
                }
            } catch (_: Throwable) {}
            previous?.uncaughtException(t, e)
        }

        // Auto-restart pointer overlay if it was enabled and overlay permission is granted.
        runCatching {
            val prefs = com.flexboard.utils.SettingsStore.prefs(this)
            val enabled = prefs.getBoolean(com.flexboard.utils.SettingsStore.KEY_POINTER_ENABLED, false)
            if (enabled && android.provider.Settings.canDrawOverlays(this)) {
                com.flexboard.ime.FloatingPointerService.start(this)
            }
        }

        // v1.10 — first-run default theme + sizing. Bundled pastel-blue
        // background image + Gboard-style sizing get applied ONCE on first
        // launch (or first launch after upgrading from a build that didn't
        // ship them). Once applied, the flag prevents us from ever
        // overwriting the user's later customisations.
        runCatching { applyDefaultsOnFirstRun() }

        // Always-on clipboard history capture. The Application stays alive while
        // ANY of our components (IME, accessibility service, foreground service,
        // activity) is running, which together keep the listener active much longer
        // than the previous IME-only setup. Items are persisted in Room so they
        // survive power off / power on.
        runCatching {
            clipboardWatcher = com.flexboard.utils.ClipboardWatcher(this).also { it.start() }
        }

        // v1.10 — schedule the background approval re-check. Worker runs
        // every 15 min when the device has internet, calls
        // ApprovalGate.evaluate(force=true) and broadcasts
        // ACTION_APPROVAL_REVOKED if approval was pulled from GitHub. The
        // IME service receives the broadcast and immediately swaps to the
        // lock view so a revoked user can't keep typing for the full 6-h
        // ApprovalGate cache window.
        runCatching {
            com.flexboard.security.ApprovalRefreshWorker.schedule(this)
        }

        // v1.12 — trial is now manually granted via the "trial" array in
        // Users.json (trial_until field). No local auto-start needed.
    }

    /**
     * v1.10 — Curated default keyboard look. Runs ONCE per install (gated by
     * [com.flexboard.utils.SettingsStore.KEY_DEFAULTS_APPLIED]) so it never
     * stomps on settings the user has tuned themselves later.
     *
     * Defaults applied:
     *   • Background image  → bundled pastel-blue PNG (drawable-nodpi)
     *   • Background opacity → 100 %
     *   • Show key boxes over background → ON
     *   • Apply background image to keys → ON
     *   • Border style       → rounded
     *   • Key opacity        → 71 %
     *   • Key text size      → 18 sp
     *   • Key / row height   → 56 dp
     *   • Key spacing        → 1 dp
     *   • 3D key shadow      → ON
     */
    private fun applyDefaultsOnFirstRun() {
        val prefs = com.flexboard.utils.SettingsStore.prefs(this)
        if (prefs.getBoolean(com.flexboard.utils.SettingsStore.KEY_DEFAULTS_APPLIED, false)) {
            return
        }
        // android.resource:// URIs are openable through ContentResolver, so
        // applyKeyboardBackground() in KeyboardView already handles them via
        // the same code path as user-picked images. No special branch needed.
        val bgUri = "android.resource://" + packageName +
                "/" + com.flexboard.R.drawable.default_keyboard_bg
        prefs.edit()
            // v1.10 — default theme is "Rose Gold" (id matches the entry in
            // ThemeManager.builtInThemes). Earlier the implicit fallback was
            // "dark" which made the keyboard look black on a fresh install,
            // even though the pastel-blue background image was set — the
            // dark theme's key colours hid most of the wallpaper. Rose Gold
            // pairs cleanly with the pastel-blue bg the user picked.
            .putString(com.flexboard.utils.SettingsStore.KEY_THEME, "rose_gold")
            .putString(com.flexboard.utils.SettingsStore.KEY_BG_IMAGE_URI, bgUri)
            .putInt(com.flexboard.utils.SettingsStore.KEY_BG_IMAGE_OPACITY, 100)
            .putBoolean(com.flexboard.utils.SettingsStore.KEY_BG_SHOW_BORDERS, true)
            .putBoolean(com.flexboard.utils.SettingsStore.KEY_BG_IMAGE_ON_KEYS, true)
            .putString(com.flexboard.utils.SettingsStore.KEY_BORDER_STYLE, "rounded")
            .putInt(com.flexboard.utils.SettingsStore.KEY_KEY_OPACITY, 71)
            .putInt(com.flexboard.utils.SettingsStore.KEY_KEY_TEXT_SIZE, 18)
            .putInt(com.flexboard.utils.SettingsStore.KEY_KEY_HEIGHT_DP, 56)
            .putInt(com.flexboard.utils.SettingsStore.KEY_KEY_MARGIN_DP, 1)
            .putBoolean(com.flexboard.utils.SettingsStore.KEY_KEY_3D_SHADOW, true)
            .putBoolean(com.flexboard.utils.SettingsStore.KEY_DEFAULTS_APPLIED, true)
            .apply()
    }
}
