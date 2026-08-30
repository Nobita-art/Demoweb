package com.flexboard.utils

import android.content.Context
import android.graphics.Typeface

/**
 * Built-in fonts. Two flavours:
 *   • Asset-backed entries — real .ttf files in `assets/fonts/`. Loaded with
 *     `Typeface.createFromAsset` so rendering is byte-identical on every
 *     OEM ROM (no silent fallback to system Roboto on Xiaomi / Vivo / Oppo
 *     / Samsung that used to make every "different" built-in font look the
 *     same).
 *   • System-family entries — pseudo-path of the form `system:<family>` —
 *     loaded with `Typeface.create(family, style)` and apply on top of the
 *     device's installed system fonts. Used to fan out the built-in catalog
 *     to 25+ entries without needing to bundle dozens more .ttf files.
 *
 * Each entry's [path] starts with `asset:` (real .ttf), `system:` (system
 * family), or is empty (default Roboto with [style] applied). FontManager
 * recognizes all three forms.
 *
 * The [path] ALSO encodes the entry's intrinsic [style] flag (`|0` / `|1` /
 * `|2` / `|3` for NORMAL / BOLD / ITALIC / BOLD_ITALIC). Without this suffix,
 * two entries that share the same .ttf file (e.g. "Caveat · Casual" +
 * "Caveat · Bold", "Monospace · Bold" + "Monospace · Italic", every System
 * Default variant) would collapse to the same path string. Result: the
 * Theme picker would highlight multiple radio buttons at once and
 * `byPath()` would return the FIRST match — so picking the Bold variant
 * silently rendered as Normal. With the style baked into the path, every
 * one of the 39 entries is uniquely identifiable.
 */
data class BuiltInFont(
    val name: String,
    val assetPath: String,           // "fonts/<file>" | "system:<family>" | ""
    val style: Int = Typeface.NORMAL // applied on top of the loaded face
) {
    val path: String get() {
        val prefix = when {
            assetPath.isEmpty() -> "asset:"
            assetPath.startsWith("system:") -> assetPath
            else -> "asset:$assetPath"
        }
        return "$prefix|$style"
    }
}

object BuiltInFonts {

    /** All bundled fonts. Order = order in the on-keyboard picker. */
    val ALL: List<BuiltInFont> = listOf(
        // ===== System defaults (always available, no .ttf required) =====
        BuiltInFont("System  ·  Default",          "",                                     Typeface.NORMAL),
        BuiltInFont("System  ·  Bold",             "",                                     Typeface.BOLD),
        BuiltInFont("System  ·  Italic",           "",                                     Typeface.ITALIC),
        BuiltInFont("System  ·  Bold Italic",      "",                                     Typeface.BOLD_ITALIC),

        // ===== System families (use device's built-in alternates) =====
        BuiltInFont("Sans-Serif Light",            "system:sans-serif-light",              Typeface.NORMAL),
        BuiltInFont("Sans-Serif Medium",           "system:sans-serif-medium",             Typeface.NORMAL),
        BuiltInFont("Sans-Serif Black",            "system:sans-serif-black",              Typeface.BOLD),
        BuiltInFont("Sans-Serif Condensed",        "system:sans-serif-condensed",          Typeface.NORMAL),
        BuiltInFont("Sans-Serif Condensed Bold",   "system:sans-serif-condensed",          Typeface.BOLD),
        BuiltInFont("Serif",                       "system:serif",                         Typeface.NORMAL),
        BuiltInFont("Serif Italic",                "system:serif",                         Typeface.ITALIC),
        BuiltInFont("Cursive",                     "system:cursive",                       Typeface.NORMAL),

        // ===== Monospace =====
        BuiltInFont("Monospace",                   "fonts/SourceCodePro.ttf",              Typeface.NORMAL),
        BuiltInFont("Monospace  ·  Bold",          "fonts/SourceCodePro.ttf",              Typeface.BOLD),
        BuiltInFont("Monospace  ·  Italic",        "fonts/SourceCodePro.ttf",              Typeface.ITALIC),

        // ===== Serif family (asset-backed) =====
        BuiltInFont("Playfair Display",            "fonts/PlayfairDisplay.ttf",            Typeface.NORMAL),
        BuiltInFont("Playfair Display  ·  Bold",   "fonts/PlayfairDisplay.ttf",            Typeface.BOLD),
        BuiltInFont("Playfair Display Italic",     "fonts/PlayfairDisplay-Italic.ttf",     Typeface.NORMAL),
        BuiltInFont("Roboto Slab",                 "fonts/RobotoSlab.ttf",                 Typeface.NORMAL),
        BuiltInFont("Roboto Slab  ·  Bold",        "fonts/RobotoSlab.ttf",                 Typeface.BOLD),
        BuiltInFont("Merriweather",                "fonts/Merriweather.ttf",               Typeface.NORMAL),
        BuiltInFont("Merriweather  ·  Bold",       "fonts/Merriweather.ttf",               Typeface.BOLD),
        BuiltInFont("Merriweather Italic",         "fonts/Merriweather-Italic.ttf",        Typeface.ITALIC),

        // ===== Display / condensed (asset-backed) =====
        BuiltInFont("Anton  ·  Tall",              "fonts/Anton-Regular.ttf",              Typeface.NORMAL),
        BuiltInFont("Bebas Neue  ·  Caps",         "fonts/BebasNeue-Regular.ttf",          Typeface.NORMAL),
        BuiltInFont("Bebas Neue  ·  Bold",         "fonts/BebasNeue-Regular.ttf",          Typeface.BOLD),
        BuiltInFont("Orbitron  ·  Techy",          "fonts/Orbitron.ttf",                   Typeface.NORMAL),
        BuiltInFont("Orbitron  ·  Bold Techy",     "fonts/Orbitron.ttf",                   Typeface.BOLD),
        BuiltInFont("Shade Blue  ·  Decorative",   "fonts/ShadeBlue.ttf",                  Typeface.NORMAL),

        // ===== Script / handwriting (asset-backed) =====
        BuiltInFont("Lobster  ·  Script",          "fonts/Lobster-Regular.ttf",            Typeface.NORMAL),
        BuiltInFont("Pacifico  ·  Brush",          "fonts/Pacifico-Regular.ttf",           Typeface.NORMAL),
        BuiltInFont("Dancing Script",              "fonts/DancingScript.ttf",              Typeface.NORMAL),
        BuiltInFont("Dancing Script  ·  Bold",     "fonts/DancingScript.ttf",              Typeface.BOLD),
        BuiltInFont("Caveat  ·  Casual",           "fonts/Caveat.ttf",                     Typeface.NORMAL),
        BuiltInFont("Caveat  ·  Bold",             "fonts/Caveat.ttf",                     Typeface.BOLD),
        BuiltInFont("Indie Flower",                "fonts/IndieFlower-Regular.ttf",        Typeface.NORMAL),
        BuiltInFont("Permanent Marker",            "fonts/PermanentMarker-Regular.ttf",    Typeface.NORMAL),
        BuiltInFont("Shadows Into Light",          "fonts/ShadowsIntoLight-Regular.ttf",   Typeface.NORMAL),

        // ===== Urdu / Nastaliq =====
        // Real Nastaliq face for Urdu typing. Renders Urdu glyphs with
        // proper joining + kasheeda; English glyphs fall through to the
        // font's Latin set (or the system font on devices that don't
        // ship Latin coverage in this face).
        BuiltInFont("Jameel Noori Nastaleeq · Urdu", "fonts/JameelNooriNastaleeq.ttf",    Typeface.NORMAL)
    )

    fun byPath(path: String?): BuiltInFont? {
        if (path == null) return null
        if (!path.startsWith("asset:") && !path.startsWith("system:") && !path.startsWith("builtin:")) return null
        // Legacy "builtin:" paths from older app versions → fall back to the
        // first entry so the picker doesn't crash on old saved prefs.
        if (path.startsWith("builtin:")) return ALL.firstOrNull()
        // First try an exact match against the unique style-aware path
        // (e.g. "asset:fonts/Caveat.ttf|2"). This is what every entry
        // produces today via [BuiltInFont.path].
        ALL.firstOrNull { it.path == path }?.let { return it }
        // Backwards-compat: paths saved by earlier versions of the app
        // didn't include the "|<style>" suffix. Strip it from the lookup
        // key (or accept a bare prefix) and fall back to the FIRST entry
        // that shares the same asset/system file. Bold/Italic variants
        // saved before this fix will resolve to their Normal sibling —
        // user can re-pick to get the variant they actually want.
        val bare = path.substringBefore('|')
        return ALL.firstOrNull {
            it.path == bare || it.path.substringBefore('|') == bare || it.assetPath == bare
        }
    }

    fun isBuiltIn(path: String?): Boolean = path != null && (
        path.startsWith("asset:") || path.startsWith("system:") || path.startsWith("builtin:")
    )

    /**
     * Cache key includes the style flag so a "Merriweather Bold" entry doesn't
     * return the same Typeface as "Merriweather" — this used to make multiple
     * built-in entries render identically in the picker.
     */
    private val cache = mutableMapOf<String, Typeface>()
    private fun cacheKey(font: BuiltInFont): String = "${font.assetPath}|${font.style}"

    /**
     * Load a built-in font's typeface with its intrinsic style applied. The
     * user-controlled bold/italic toggles in Theme & Appearance are layered
     * on top of this by FontManager.loadKeyTypeface, so users can still bold
     * an italic entry etc.
     */
    fun typefaceFor(ctx: Context, font: BuiltInFont): Typeface {
        cache[cacheKey(font)]?.let { return it }
        val base: Typeface = try {
            when {
                font.assetPath.isEmpty() -> Typeface.DEFAULT
                font.assetPath.startsWith("system:") -> {
                    val family = font.assetPath.removePrefix("system:")
                    Typeface.create(family, Typeface.NORMAL)
                }
                else -> Typeface.createFromAsset(ctx.assets, font.assetPath)
            }
        } catch (t: Throwable) {
            android.util.Log.e("BuiltInFonts", "load failed: ${font.assetPath}", t)
            Typeface.DEFAULT
        }
        val styled: Typeface = try {
            if (font.style == Typeface.NORMAL) base else Typeface.create(base, font.style)
        } catch (_: Throwable) { base }
        cache[cacheKey(font)] = styled
        return styled
    }
}
