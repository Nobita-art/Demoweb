package com.flexboard.ime

/**
 * v1.10 — Live typing-speed (WPM) tracker shown on the suggestion strip,
 * right next to the ≡ menu button.
 *
 * Algorithm:
 *   • Each productive keystroke (letter, digit, symbol, space, punctuation)
 *     records a timestamp into a rolling 10-second window.
 *   • Backspace is intentionally NOT recorded — corrections shouldn't
 *     inflate the speed reading and "delete spam" would otherwise look
 *     like 200+ WPM.
 *   • WPM = (chars in window / 5) / (window seconds / 60). Standard WPM
 *     definition uses a 5-character "word" (matches Gboard / TypingClub /
 *     monkeytype.com), so a 60-char-per-minute typist reads 12 WPM.
 *   • After [IDLE_MS] of silence the tracker reports 0 so the strip badge
 *     fades back to "—" instead of holding a stale number.
 *
 * The tracker is a singleton because it lives across keyboard show/hide
 * cycles — closing & reopening the IME shouldn't reset the speed of an
 * ongoing typing session in the same chat field.
 */
object WpmTracker {

    /** Length of the sliding window. 10 s gives a stable, responsive
     *  reading — short enough to feel "live", long enough that one fast
     *  burst of 3 letters doesn't spike the gauge to 300 WPM. */
    private const val WINDOW_MS = 10_000L

    /** After this much idle time the tracker reports 0. The on-screen
     *  badge then renders a neutral placeholder so users don't see a
     *  stale "85 WPM" while they're staring at the screen. */
    private const val IDLE_MS = 3_000L

    /** Standard WPM definition: 5 characters = 1 word. */
    private const val CHARS_PER_WORD = 5

    private val timestamps: ArrayDeque<Long> = ArrayDeque()

    /** Call from the IME's CHAR / SPACE / PERIOD / COMMA branches. */
    @Synchronized
    fun recordChar() {
        val now = System.currentTimeMillis()
        timestamps.addLast(now)
        pruneOld(now)
    }

    /** Returns the current rolling-window WPM (0 if idle / not enough data). */
    @Synchronized
    fun currentWpm(): Int {
        val now = System.currentTimeMillis()
        pruneOld(now)
        // Need at least 2 samples to define a span; one keypress alone
        // would divide-by-tiny-number and report nonsense.
        if (timestamps.size < 2) return 0
        val first = timestamps.first()
        val spanMs = (now - first).coerceAtLeast(1L)
        // (count / CHARS_PER_WORD) / (spanMs / 60_000)
        // = count * 60_000 / (CHARS_PER_WORD * spanMs)
        // = count * 12_000 / spanMs
        return (timestamps.size.toLong() * 60_000L / (CHARS_PER_WORD * spanMs)).toInt()
    }

    /** True when the last keystroke is older than [IDLE_MS] (or there
     *  have never been any). UI uses this to switch the badge to a
     *  placeholder so a stale number isn't shown. */
    @Synchronized
    fun isIdle(): Boolean {
        val now = System.currentTimeMillis()
        return timestamps.isEmpty() || now - timestamps.last() > IDLE_MS
    }

    /** Wipe the window — called on IME finish so a brand-new field
     *  starts measuring from zero. */
    @Synchronized
    fun reset() {
        timestamps.clear()
    }

    private fun pruneOld(now: Long) {
        while (timestamps.isNotEmpty() && now - timestamps.first() > WINDOW_MS) {
            timestamps.removeFirst()
        }
    }
}
