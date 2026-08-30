package com.flexboard.security

import android.content.Context
import android.content.SharedPreferences
import com.flexboard.utils.SettingsStore
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Checks https://keyboard.kraza.qzz.io/update.json for a newer version.
 *
 * When the server reports a version higher than the currently installed
 * [BuildConfig.VERSION_NAME], GatedApp shows a non-dismissable dialog
 * until the user taps "Update Now" and installs the new APK.
 *
 * Cache: result is cached for 30 minutes so every app open doesn't hit the
 * network. If the network is unavailable, the last known result is returned.
 */
object UpdateChecker {

    private const val UPDATE_URL = "https://keyboard.kraza.qzz.io/update.json"
    private const val PREF_NAME = "flexboard_update"
    private const val KEY_LATEST_VERSION = "latest_version"
    private const val KEY_DOWNLOAD_URL = "download_url"
    private const val KEY_RELEASE_NOTES = "release_notes"
    private const val KEY_LAST_CHECK = "last_check_ms"
    private const val CACHE_MS = 30L * 60 * 1000

    data class UpdateInfo(
        val latestVersion: String,
        val downloadUrl: String,
        val releaseNotes: String,
        val updateAvailable: Boolean
    )

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .header("Cache-Control", "no-cache")
                    .header("Pragma", "no-cache")
                    .build()
                chain.proceed(req)
            }
            .build()
    }

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    private fun versionGreater(server: String, installed: String): Boolean {
        val sv = server.trim().removePrefix("v").split(".").mapNotNull { it.toIntOrNull() }
        val iv = installed.trim().removePrefix("v").split(".").mapNotNull { it.toIntOrNull() }
        for (i in 0 until maxOf(sv.size, iv.size)) {
            val s = sv.getOrElse(i) { 0 }
            val inst = iv.getOrElse(i) { 0 }
            if (s > inst) return true
            if (s < inst) return false
        }
        return false
    }

    fun cachedResult(ctx: Context): UpdateInfo? {
        val p = prefs(ctx)
        val latestVersion = p.getString(KEY_LATEST_VERSION, null) ?: return null
        val downloadUrl = p.getString(KEY_DOWNLOAD_URL, null) ?: return null
        val releaseNotes = p.getString(KEY_RELEASE_NOTES, null) ?: ""
        val installed = com.flexboard.BuildConfig.VERSION_NAME
        return UpdateInfo(
            latestVersion = latestVersion,
            downloadUrl = downloadUrl,
            releaseNotes = releaseNotes,
            updateAvailable = versionGreater(latestVersion, installed)
        )
    }

    fun isCacheStale(ctx: Context): Boolean {
        val lastCheck = prefs(ctx).getLong(KEY_LAST_CHECK, 0L)
        return System.currentTimeMillis() - lastCheck > CACHE_MS
    }

    suspend fun check(ctx: Context, force: Boolean = false): UpdateInfo? {
        if (!force && !isCacheStale(ctx)) {
            return cachedResult(ctx)
        }
        return try {
            val req = Request.Builder().url(UPDATE_URL).build()
            val body = httpClient.newCall(req).execute().use { it.body?.string() } ?: return cachedResult(ctx)
            val json = JSONObject(body)
            val latestVersion = json.optString("latest_version", "")
            val downloadUrl = json.optString("download_url", "")
            val releaseNotes = json.optString("release_notes", "New update available.")
            if (latestVersion.isBlank()) return cachedResult(ctx)
            prefs(ctx).edit()
                .putString(KEY_LATEST_VERSION, latestVersion)
                .putString(KEY_DOWNLOAD_URL, downloadUrl)
                .putString(KEY_RELEASE_NOTES, releaseNotes)
                .putLong(KEY_LAST_CHECK, System.currentTimeMillis())
                .apply()
            val installed = com.flexboard.BuildConfig.VERSION_NAME
            UpdateInfo(
                latestVersion = latestVersion,
                downloadUrl = downloadUrl,
                releaseNotes = releaseNotes,
                updateAvailable = versionGreater(latestVersion, installed)
            )
        } catch (_: Throwable) {
            cachedResult(ctx)
        }
    }
}
