package com.flexboard.security

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Manages activation-key-based access for FlexBoard Pro.
 *
 * Flow:
 *   1. User enters a key on the PlansScreen.
 *   2. [saveKey] persists it and clears the cache.
 *   3. [checkKey] hits /key-check?key=XYZ on the keyboard website and
 *      caches the result.
 *   4. [cachedState] is read synchronously (no network) whenever the
 *      gate needs to decide quickly.
 *   5. Background re-check happens every [CHECK_INTERVAL_MS] (1 h).
 *      If the server returns invalid/expired, the keyboard locks.
 */
object KeyManager {

    private const val PREFS           = "flexboard_key"
    private const val K_SAVED_KEY     = "activation_key"
    private const val K_LAST_CHECK    = "key_last_check"
    private const val K_KEY_VALID     = "key_valid"
    private const val K_KEY_EXPIRES   = "key_expires_ms"
    private const val CHECK_INTERVAL_MS = 60L * 60L * 1000L     // 1 hour
    private const val MAX_OFFLINE_MS    = 24L * 60L * 60L * 1000L // 1 day offline tolerance

    private const val BASE_URL = "https://keyboard.kraza.qzz.io"

    sealed class KeyState {
        object NoKey      : KeyState()
        object KeyActive  : KeyState()
        object KeyInvalid : KeyState()
        object KeyExpired : KeyState()
        data class KeyOffline(val previouslyValid: Boolean) : KeyState()
    }

    @Volatile private var http: OkHttpClient? = null
    private fun http(): OkHttpClient {
        http?.let { return it }
        val c = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
        http = c
        return c
    }

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getSavedKey(ctx: Context): String =
        prefs(ctx).getString(K_SAVED_KEY, "").orEmpty().trim()

    fun saveKey(ctx: Context, key: String) {
        prefs(ctx).edit()
            .putString(K_SAVED_KEY, key.trim())
            .putLong(K_LAST_CHECK, 0L)
            .putBoolean(K_KEY_VALID, false)
            .putLong(K_KEY_EXPIRES, 0L)
            .apply()
    }

    fun clearKey(ctx: Context) {
        prefs(ctx).edit().clear().apply()
    }

    fun isCacheStale(ctx: Context): Boolean {
        val key = getSavedKey(ctx)
        if (key.isBlank()) return false
        val last = prefs(ctx).getLong(K_LAST_CHECK, 0L)
        return System.currentTimeMillis() - last >= CHECK_INTERVAL_MS
    }

    fun cachedState(ctx: Context): KeyState {
        val key = getSavedKey(ctx)
        if (key.isBlank()) return KeyState.NoKey

        val p         = prefs(ctx)
        val valid     = p.getBoolean(K_KEY_VALID, false)
        val lastCheck = p.getLong(K_LAST_CHECK, 0L)
        val expiresMs = p.getLong(K_KEY_EXPIRES, 0L)
        val now       = System.currentTimeMillis()

        if (lastCheck == 0L) return KeyState.NoKey  // never been checked yet

        if (expiresMs > 0L && now > expiresMs) return KeyState.KeyExpired
        return if (valid) KeyState.KeyActive else KeyState.KeyInvalid
    }

    suspend fun checkKey(
        ctx: Context,
        key: String = getSavedKey(ctx),
        force: Boolean = false
    ): KeyState = withContext(Dispatchers.IO) {
        if (key.isBlank()) return@withContext KeyState.NoKey

        val p         = prefs(ctx)
        val now       = System.currentTimeMillis()
        val lastCheck = p.getLong(K_LAST_CHECK, 0L)
        val wasValid  = p.getBoolean(K_KEY_VALID, false)

        if (!force && lastCheck > 0L && now - lastCheck < CHECK_INTERVAL_MS) {
            return@withContext cachedState(ctx)
        }

        try {
            val encoded = java.net.URLEncoder.encode(key, "UTF-8")
            val url     = "$BASE_URL/key-check?key=$encoded"
            val req = Request.Builder().url(url)
                .header("Cache-Control", "no-cache")
                .header("Pragma", "no-cache")
                .header("User-Agent", "FlexBoardPro")
                .build()

            http().newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    if (wasValid && now - lastCheck < MAX_OFFLINE_MS) return@withContext KeyState.KeyActive
                    return@withContext KeyState.KeyOffline(wasValid)
                }

                val body = resp.body?.string()
                    ?: return@withContext KeyState.KeyOffline(wasValid)
                val json      = JSONObject(body)
                val valid     = json.optBoolean("valid", false)
                val expiresAt = json.optString("expires_at", "")
                val expiresMs = if (expiresAt.isNotBlank()) parseIso8601(expiresAt) else 0L

                p.edit()
                    .putString(K_SAVED_KEY, key)
                    .putBoolean(K_KEY_VALID, valid)
                    .putLong(K_LAST_CHECK, now)
                    .putLong(K_KEY_EXPIRES, expiresMs)
                    .apply()

                return@withContext when {
                    !valid                              -> KeyState.KeyInvalid
                    expiresMs > 0L && now > expiresMs  -> KeyState.KeyExpired
                    else                               -> KeyState.KeyActive
                }
            }
        } catch (_: Exception) {
            if (wasValid && now - lastCheck < MAX_OFFLINE_MS) return@withContext KeyState.KeyActive
            return@withContext KeyState.KeyOffline(wasValid)
        }
    }

    private fun parseIso8601(s: String): Long {
        val trimmed = s.trim()
        if (trimmed.isEmpty()) return 0L
        val candidates = listOf(
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd"
        )
        for (pattern in candidates) {
            try {
                val sdf = java.text.SimpleDateFormat(pattern, java.util.Locale.US).apply {
                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                }
                return sdf.parse(trimmed)?.time ?: continue
            } catch (_: Throwable) {}
        }
        return 0L
    }
}
