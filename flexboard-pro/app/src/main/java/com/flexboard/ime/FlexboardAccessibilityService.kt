package com.flexboard.ime

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class FlexboardAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance === this) instance = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) { /* not needed */ }
    override fun onInterrupt() {}

    fun typeIntoFocused(text: String): Boolean {
        val node = findFocusedEditable() ?: return false
        val existing = node.text?.toString() ?: ""
        val args = Bundle().apply {
            putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                existing + text
            )
        }
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    fun pasteIntoFocused(): Boolean {
        val node = findFocusedEditable() ?: return false
        return node.performAction(AccessibilityNodeInfo.ACTION_PASTE)
    }

    /** Dispatch a tap at the given screen coordinates. Returns true if the gesture was queued. */
    fun clickAt(x: Float, y: Float): Boolean {
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 60))
            .build()
        return dispatchGesture(gesture, null, null)
    }

    /**
     * Reads the package name of the app whose window is currently in focus.
     * Used by AutoTypeEngine to decide between IME-Enter and accessibility
     * send-button click — chat apps (WhatsApp/Telegram/etc.) get the button
     * click, browsers/search boxes get a real Enter.
     */
    fun currentForegroundPackage(): String? =
        try { rootInActiveWindow?.packageName?.toString() } catch (_: Throwable) { null }

    /** True when the package is a chat app whose send button we know how to click. */
    fun isKnownChatApp(pkg: String?): Boolean = pkg != null && pkg in KNOWN_CHAT_APPS

    /** Try to click the visible "Send" button. First per-app id list, then generic scan. */
    fun pressSend(): Boolean {
        val root = rootInActiveWindow ?: return false
        val pkg = root.packageName?.toString() ?: ""
        // 1) Try per-app exact resource-id matches first
        val targetIds = perAppSendIds[pkg].orEmpty()
        for (id in targetIds) {
            val nodes = root.findAccessibilityNodeInfosByViewId(id) ?: continue
            for (n in nodes) {
                if (n.isVisibleToUser && (n.isClickable || n.isEnabled)) {
                    val target = ascendToClickable(n)
                    if (target.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
                }
            }
        }
        // 2) Per-app SMART FALLBACK for Messenger / Messenger Lite.
        //    Facebook ships a new Messenger build every couple of weeks
        //    and the send-button resource id keeps changing (currently
        //    a hashed name like `_id_xyz123`). Instead of chasing the
        //    id we scan for "send_button" / "composer_send_button" by
        //    SUBSTRING in the resource-id of every visible node and
        //    pick the right-most one in the bottom half of the screen
        //    (the chat composer is always at the bottom-right).
        if (pkg == "com.facebook.orca" || pkg == "com.facebook.mlite") {
            val node = findMessengerSendNode(root)
            if (node != null && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
        }
        // 3) Generic scan by content-description / text keywords.
        //    Restricted to known chat apps so we don't accidentally tap a
        //    "send" link on a random webpage in the browser.
        if (isKnownChatApp(pkg)) {
            val node = findSendNode(root) ?: return false
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }
        return false
    }

    /**
     * Messenger-specific scan. Walks every visible node, scoring each one
     * by:
     *   • resource-id contains "send" / "composer_send" → +5
     *   • content-description matches "send" keyword     → +3
     *   • node is in the bottom 35% of the screen        → +2
     *   • node is in the right 30% of the screen         → +2
     *   • node is clickable                              → +2
     * The highest-scoring node wins. Picks Messenger's small blue paper-
     * plane in the bottom-right corner reliably even after FB ships a new
     * build that breaks our hard-coded resource-id list.
     */
    private fun findMessengerSendNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val displayMetrics = resources.displayMetrics
        val screenW = displayMetrics.widthPixels
        val screenH = displayMetrics.heightPixels
        val rect = android.graphics.Rect()
        var best: AccessibilityNodeInfo? = null
        var bestScore = 0
        fun walk(n: AccessibilityNodeInfo?) {
            if (n == null) return
            try {
                if (n.isVisibleToUser) {
                    var score = 0
                    val rid = n.viewIdResourceName?.lowercase() ?: ""
                    val cd  = n.contentDescription?.toString()?.lowercase() ?: ""
                    // The mic button on Messenger sits LEFT of the send
                    // button and its content-desc usually says "Voice
                    // message" / "Hold to record" — we explicitly skip
                    // anything that mentions the mic so we never tap it.
                    val isMic = "mic" in cd || "voice" in cd || "record" in cd ||
                                "mic" in rid || "voice" in rid || "record" in rid
                    if (!isMic) {
                        if ("composer_send" in rid || rid.endsWith(":id/send_button") ||
                            rid.endsWith(":id/send") || "send_progress" in rid) score += 5
                        if (cd == "send" || cd.startsWith("send ") || cd.endsWith(" send") ||
                            "send message" in cd || "paper plane" in cd) score += 3
                        n.getBoundsInScreen(rect)
                        if (rect.top > screenH * 0.55f) score += 2
                        if (rect.left > screenW * 0.65f) score += 2
                        if (n.isClickable) score += 2
                        // Tiny round buttons (~48dp) score better than huge
                        // containers — Messenger's send button is a small
                        // circle, not the entire composer view.
                        val w = rect.width()
                        val h = rect.height()
                        val px48 = (48 * displayMetrics.density).toInt()
                        if (w in 1..(px48 * 2) && h in 1..(px48 * 2)) score += 2
                        if (score > bestScore) {
                            bestScore = score
                            best = n
                        }
                    }
                }
                for (i in 0 until n.childCount) walk(n.getChild(i))
            } catch (_: Throwable) { /* swallow opaque accessibility errors */ }
        }
        walk(root)
        // Need at least a meaningful match (resource-id OR content-desc)
        // to avoid clicking some random tile in the chat list.
        return if (bestScore >= 5) best?.let { ascendToClickable(it) } else null
    }

    private val perAppSendIds: Map<String, List<String>> = mapOf(
        "com.whatsapp" to listOf(
            "com.whatsapp:id/send",
            "com.whatsapp:id/send_container"
        ),
        "com.whatsapp.w4b" to listOf(
            "com.whatsapp.w4b:id/send",
            "com.whatsapp.w4b:id/send_container"
        ),
        "com.facebook.orca" to listOf(
            "com.facebook.orca:id/send_button",
            "com.facebook.orca:id/composer_send_button"
        ),
        "com.facebook.mlite" to listOf(
            "com.facebook.mlite:id/composer_send_button"
        ),
        "com.instagram.android" to listOf(
            "com.instagram.android:id/row_thread_composer_button_send",
            "com.instagram.android:id/composer_send_button"
        ),
        "org.telegram.messenger" to listOf(
            "org.telegram.messenger:id/send_button"
        ),
        "org.thunderdog.challegram" to listOf(
            "org.thunderdog.challegram:id/msg_send"
        ),
        "com.google.android.apps.messaging" to listOf(
            "com.google.android.apps.messaging:id/send_message_button_icon",
            "com.google.android.apps.messaging:id/send_message_button"
        ),
        "com.android.mms" to listOf(
            "com.android.mms:id/send_button"
        ),
        "com.discord" to listOf(
            "com.discord:id/chat_input_send"
        ),
        "com.viber.voip" to listOf(
            "com.viber.voip:id/btn_send"
        ),
        // Snapchat intentionally NOT listed here — see KNOWN_CHAT_APPS
        // comment for why. Snapchat's chat composer is single-line and
        // an Enter key press fires its send action reliably; the previous
        // accessibility-click path didn't work because Snapchat's send
        // button is rendered inside a Compose surface with no resource id.
    )

    private val sendKeywords = listOf(
        "send", "send message", "submit", "post",
        "بھیجیں", "ارسال", "إرسال", "ارسل", "भेजें", "전송", "送信", "发送"
    )

    private fun findSendNode(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        val cd = node.contentDescription?.toString()?.lowercase()?.trim() ?: ""
        val tx = node.text?.toString()?.lowercase()?.trim() ?: ""
        val matches = sendKeywords.any { kw ->
            val k = kw.lowercase()
            cd == k || cd.startsWith("$k ") || cd.endsWith(" $k") || tx == k
        }
        if (matches && node.isVisibleToUser && (node.isClickable || node.isEnabled)) {
            return ascendToClickable(node)
        }
        for (i in 0 until node.childCount) {
            val r = findSendNode(node.getChild(i))
            if (r != null) return r
        }
        return null
    }

    private fun ascendToClickable(start: AccessibilityNodeInfo): AccessibilityNodeInfo {
        var n: AccessibilityNodeInfo? = start
        while (n != null && !n.isClickable) n = n.parent
        return n ?: start
    }

    private fun findFocusedEditable(): AccessibilityNodeInfo? {
        val root = rootInActiveWindow ?: return null
        return root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            ?: searchEditable(root)
    }

    private fun searchEditable(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isEditable) return node
        for (i in 0 until node.childCount) {
            val r = searchEditable(node.getChild(i))
            if (r != null) return r
        }
        return null
    }

    companion object {
        @Volatile var instance: FlexboardAccessibilityService? = null

        /**
         * Apps where the message field is multi-line (Enter = newline) AND
         * there's a separate visible Send / arrow / paper-plane button. For
         * these we tap the button via accessibility instead of pressing
         * Enter, which would just insert a newline. Mirrors the keys of
         * [perAppSendIds] so adding a new app there auto-enrolls it here.
         */
        val KNOWN_CHAT_APPS: Set<String> = setOf(
            "com.whatsapp",
            "com.whatsapp.w4b",
            "com.facebook.orca",
            "com.facebook.mlite",
            "com.instagram.android",
            "org.telegram.messenger",
            "org.thunderdog.challegram",
            "com.google.android.apps.messaging",
            "com.android.mms",
            "com.discord",
            "com.viber.voip"
            // Snapchat intentionally excluded — its chat field treats Enter
            // as the send action, so we route Snapchat through the IME-Enter
            // branch in AutoTypeEngine.trySend instead of the accessibility
            // send-button click (which doesn't work because the Compose
            // composer has no resource id).
        )
    }
}
