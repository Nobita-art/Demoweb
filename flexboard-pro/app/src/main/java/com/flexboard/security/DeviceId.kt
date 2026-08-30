package com.flexboard.security

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import java.security.MessageDigest

/**
 * Stable device fingerprint used by the approval system. Combines
 * Settings.Secure.ANDROID_ID with hardware identifiers, hashes them,
 * and returns the first 16 hex chars.
 *
 *  - Short enough that the user can dictate it over WhatsApp.
 *  - Long enough (64 bits) that collisions across the user base are
 *    practically zero.
 *  - Resets on factory reset (because ANDROID_ID resets) — that's the
 *    desired behavior. We don't want one approval to cover a
 *    user's-friend's-cousin's-phone if they share the APK file.
 */
object DeviceId {

    @SuppressLint("HardwareIds")
    fun get(ctx: Context): String {
        val androidId = try {
            Settings.Secure.getString(
                ctx.contentResolver,
                Settings.Secure.ANDROID_ID
            ) ?: ""
        } catch (_: Throwable) {
            ""
        }
        val hwSig = "${Build.MANUFACTURER}|${Build.MODEL}|${Build.DEVICE}|${Build.BRAND}"
        val seed = "flexboard_devid_v1::$androidId::$hwSig".toByteArray(Charsets.UTF_8)
        val hash = MessageDigest.getInstance("SHA-256").digest(seed)
        return hash.joinToString("") { "%02x".format(it) }.substring(0, 16)
    }
}
