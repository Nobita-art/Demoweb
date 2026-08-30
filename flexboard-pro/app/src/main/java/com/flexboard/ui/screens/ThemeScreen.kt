package com.flexboard.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.flexboard.utils.BuiltInFonts
import com.flexboard.utils.FontManager
import com.flexboard.utils.KeyboardTheme
import com.flexboard.utils.SettingsStore
import com.flexboard.utils.ThemeManager
import com.flexboard.security.Obf
import com.flexboard.security.ObfConstants

@Composable
fun ThemeScreen(
    onPickBackground: () -> Unit
) {
    val ctx = LocalContext.current
    val prefs = SettingsStore.prefs(ctx)
    // `themeTick` bumps every time we change the theme so the live preview
    // recomputes ThemeManager.current(ctx) and recolors itself in real time
    // without needing to open the actual IME.
    var themeTick by remember { mutableStateOf(0) }
    var selectedTheme by remember { mutableStateOf(prefs.getString(SettingsStore.KEY_THEME, "dark") ?: "dark") }
    var keyOpacity by remember { mutableStateOf(prefs.getInt(SettingsStore.KEY_KEY_OPACITY, 100)) }
    var border by remember { mutableStateOf(prefs.getString(SettingsStore.KEY_BORDER_STYLE, "rounded") ?: "rounded") }
    var keyTextSize by remember { mutableStateOf(prefs.getInt(SettingsStore.KEY_KEY_TEXT_SIZE, 16)) }
    var keyHeight by remember { mutableStateOf(prefs.getInt(SettingsStore.KEY_KEY_HEIGHT_DP, 58)) }
    // v1.7 — per-key spacing + 3D shadow controls. Default 3 dp spacing
    // mirrors the new tight Gboard-class look.
    var keyMargin by remember { mutableStateOf(prefs.getInt(SettingsStore.KEY_KEY_MARGIN_DP, 3)) }
    var key3dShadow by remember { mutableStateOf(prefs.getBoolean(SettingsStore.KEY_KEY_3D_SHADOW, true)) }
    var bgOpacity by remember { mutableStateOf(prefs.getInt(SettingsStore.KEY_BG_IMAGE_OPACITY, 60)) }
    var showBordersOverBg by remember { mutableStateOf(prefs.getBoolean(SettingsStore.KEY_BG_SHOW_BORDERS, false)) }
    var bgImageOnKeys by remember { mutableStateOf(prefs.getBoolean(SettingsStore.KEY_BG_IMAGE_ON_KEYS, false)) }
    var haptic by remember { mutableStateOf(prefs.getBoolean(SettingsStore.KEY_HAPTIC, true)) }
    var sound by remember { mutableStateOf(prefs.getBoolean(SettingsStore.KEY_SOUND, false)) }
    var soundVolume by remember { mutableStateOf(prefs.getInt(SettingsStore.KEY_SOUND_VOLUME, 35)) }
    var bold by remember { mutableStateOf(prefs.getBoolean(SettingsStore.KEY_FONT_BOLD, false)) }
    var italic by remember { mutableStateOf(prefs.getBoolean(SettingsStore.KEY_FONT_ITALIC, false)) }
    val defaultSpaceLabel = remember {
        Obf.decode(ctx, ObfConstants.SPACE_LABEL).ifBlank { "space" }
    }
    var spaceLabel by remember {
        mutableStateOf(prefs.getString(SettingsStore.KEY_SPACE_LABEL, "") ?: "")
    }
    var fontsTick by remember { mutableStateOf(0) }
    // Built-in catalog only — the "Add custom font" flow was removed in v1.4.2
    // and replaced with a single curated list of 25+ bundled fonts.
    val builtInFonts = remember { BuiltInFonts.ALL }
    val activePath = remember(fontsTick) {
        SettingsStore.prefs(ctx).getString(SettingsStore.KEY_FONT_PATH, "") ?: ""
    }
    val bgUri = remember(fontsTick) { prefs.getString(SettingsStore.KEY_BG_IMAGE_URI, "") ?: "" }

    // Live preview text — tapping it opens the active IME (FlexBoard if set as default)
    var previewText by remember { mutableStateOf("") }

    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Theme & Appearance", color = Color(0xFFFF8C00), fontWeight = FontWeight.Bold)

        // === LIVE MINI-KEYBOARD PREVIEW ===
        // Renders a fake mini keyboard using the EXACT colors that the IME
        // will render with — recomputed every time `themeTick` bumps so any
        // theme/color slider change shows up here instantly. The user no
        // longer needs to open a chat to verify their pick.
        val livePreview = remember(themeTick) { ThemeManager.current(ctx) }
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F))) {
            Column(Modifier.padding(16.dp)) {
                Text("Live Preview", color = Color(0xFFFF8C00), fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                MiniKeyboardPreview(
                    theme = livePreview,
                    spaceLabel = spaceLabel.ifBlank { defaultSpaceLabel }
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "The preview reflects theme changes in real time. Tap the field below " +
                            "to view the same configuration on the live keyboard.",
                    color = Color.LightGray
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = previewText,
                    onValueChange = { previewText = it },
                    label = { Text("Type here to preview the real keyboard…") },
                    minLines = 1, maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(4.dp))
                OutlinedButton(onClick = { previewText = "" }) { Text("Clear") }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F))) {
            Column(Modifier.padding(16.dp)) {
                Text("Built-in themes", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                ThemeManager.BUILT_IN.forEach { t ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(36.dp).clip(RoundedCornerShape(6.dp)).background(Color(t.keyboardBg)))
                        Spacer(Modifier.width(8.dp))
                        Box(Modifier.size(36.dp).clip(RoundedCornerShape(6.dp)).background(Color(t.keyBg)))
                        Spacer(Modifier.width(12.dp))
                        Text(t.name, color = Color.White, modifier = Modifier.weight(1f))
                        RadioButton(selected = selectedTheme == t.id, onClick = {
                            selectedTheme = t.id
                            ThemeManager.setTheme(ctx, t.id)
                            // Bump tick so the live mini-keyboard preview
                            // above instantly recolors with the picked theme.
                            themeTick++
                        })
                    }
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F))) {
            Column(Modifier.padding(16.dp)) {
                Text("Custom Background Image", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(if (bgUri.isBlank()) "No image set" else "Image: ${bgUri.takeLast(40)}", color = Color.LightGray)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onPickBackground(); fontsTick++ },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8C00), contentColor = Color.Black)) {
                        Text("Pick Image")
                    }
                    OutlinedButton(onClick = {
                        prefs.edit().remove(SettingsStore.KEY_BG_IMAGE_URI).apply(); fontsTick++
                    }) { Text("Remove") }
                }
                Spacer(Modifier.height(8.dp))
                Text("Background opacity: $bgOpacity%", color = Color.White)
                Slider(
                    value = bgOpacity.toFloat(), valueRange = 0f..100f,
                    onValueChange = {
                        bgOpacity = it.toInt(); prefs.edit().putInt(SettingsStore.KEY_BG_IMAGE_OPACITY, bgOpacity).apply()
                    }
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = showBordersOverBg, onCheckedChange = {
                        showBordersOverBg = it
                        prefs.edit().putBoolean(SettingsStore.KEY_BG_SHOW_BORDERS, it).apply()
                    })
                    Spacer(Modifier.width(8.dp))
                    Text("Show key boxes over background", color = Color.White)
                }
                // ===== Per-key bg image (issue #2) =====
                // When ON, the same wallpaper paints on every key
                // (clipped to the key's rounded shape). Defaults OFF
                // so existing users see no change unless they enable it.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = bgImageOnKeys, onCheckedChange = {
                        bgImageOnKeys = it
                        prefs.edit().putBoolean(SettingsStore.KEY_BG_IMAGE_ON_KEYS, it).apply()
                    })
                    Spacer(Modifier.width(8.dp))
                    Text("Apply background image to keys", color = Color.White)
                }
                Text(
                    when {
                        bgUri.isNotBlank() && bgImageOnKeys ->
                            "Each key now shows your wallpaper image clipped to its shape."
                        bgUri.isNotBlank() && !showBordersOverBg ->
                            "Keys are transparent — only the labels are rendered over the background."
                        bgUri.isNotBlank() ->
                            "Key surfaces are rendered over the background image."
                        else ->
                            "Select a background image to enable these options."
                    },
                    color = Color.LightGray
                )
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F))) {
            Column(Modifier.padding(16.dp)) {
                Text("Keys & Sizing", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Border style", color = Color.White)
                Row {
                    listOf("none","thin","thick","rounded").forEach { s ->
                        FilterChip(selected = border == s, onClick = {
                            border = s; prefs.edit().putString(SettingsStore.KEY_BORDER_STYLE, s).apply()
                        }, label = { Text(s) }, modifier = Modifier.padding(end = 6.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Key opacity: $keyOpacity%", color = Color.White)
                Slider(value = keyOpacity.toFloat(), valueRange = 20f..100f, onValueChange = {
                    keyOpacity = it.toInt(); prefs.edit().putInt(SettingsStore.KEY_KEY_OPACITY, keyOpacity).apply()
                })
                Text("Key text size: $keyTextSize sp", color = Color.White)
                Slider(value = keyTextSize.toFloat(), valueRange = 10f..28f, onValueChange = {
                    keyTextSize = it.toInt(); prefs.edit().putInt(SettingsStore.KEY_KEY_TEXT_SIZE, keyTextSize).apply()
                })
                Text("Key / row height: $keyHeight dp", color = Color.White)
                // Range widened 36..80 → 44..90 dp to match the new
                // 58 dp default (issue #3 — bigger keys out of the box).
                Slider(value = keyHeight.toFloat(), valueRange = 44f..90f, onValueChange = {
                    keyHeight = it.toInt(); prefs.edit().putInt(SettingsStore.KEY_KEY_HEIGHT_DP, keyHeight).apply()
                })
                // v1.7 — Key spacing (was hard-coded at 6 dp in v1.6 and
                // made keys look small because every key lost ~12 dp of
                // width to gaps). 1 dp = packed Gboard look, 8 dp = airy.
                Text("Key spacing: $keyMargin dp", color = Color.White)
                Slider(value = keyMargin.toFloat(), valueRange = 1f..8f, steps = 6, onValueChange = {
                    keyMargin = it.toInt(); prefs.edit().putInt(SettingsStore.KEY_KEY_MARGIN_DP, keyMargin).apply()
                })
                // v1.7 — 3D shadow toggle. ON = soft drop-shadow under
                // each key + press-into-surface animation (Gboard look).
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = key3dShadow, onCheckedChange = {
                        key3dShadow = it
                        prefs.edit().putBoolean(SettingsStore.KEY_KEY_3D_SHADOW, it).apply()
                    })
                    Spacer(Modifier.width(8.dp))
                    Text("3D key shadow (Gboard-style)", color = Color.White)
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F))) {
            Column(Modifier.padding(16.dp)) {
                Text("Space Button", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Choose the text shown on the wide space button. This setting is managed here in Theme & Appearance.",
                    color = Color.LightGray
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = spaceLabel,
                    onValueChange = {
                        spaceLabel = it.take(24)
                        prefs.edit().putString(SettingsStore.KEY_SPACE_LABEL, spaceLabel).apply()
                    },
                    label = { Text("Space button text") },
                    placeholder = { Text(defaultSpaceLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (spaceLabel.isBlank()) "Using default: $defaultSpaceLabel"
                        else "Preview: ${spaceLabel.trim()}",
                        color = Color.LightGray
                    )
                    OutlinedButton(onClick = {
                        spaceLabel = ""
                        prefs.edit().remove(SettingsStore.KEY_SPACE_LABEL).apply()
                    }) {
                        Text("Reset")
                    }
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F))) {
            Column(Modifier.padding(16.dp)) {
                Text("Built-in Fonts", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(
                    "${builtInFonts.size} bundled typefaces. The selected font is applied " +
                            "across keys, suggestions, the emoji panel and the toolbar.",
                    color = Color.LightGray
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = {
                    // Route through FontManager so the typeface cache is
                    // invalidated atomically with the pref write — without
                    // this the next keyboard reload reads the new pref but
                    // serves the OLD cached typeface and "system default"
                    // never actually shows up.
                    FontManager.setActive(ctx, null)
                    fontsTick++
                }) {
                    Text("Use System Default")
                }
                Spacer(Modifier.height(8.dp))
                // Custom RadioButton colors so the unselected ring is
                // visible against the dark Color(0xFF1F1F1F) card surface.
                // Material3 defaults render a near-black ring on a near-
                // black background which the user reported as "radio
                // button show nahi ho rha". Bright orange selected dot
                // matches the rest of the app's accent.
                val fontRadioColors = RadioButtonDefaults.colors(
                    selectedColor = Color(0xFFFF8C00),
                    unselectedColor = Color.LightGray,
                    disabledSelectedColor = Color.Gray,
                    disabledUnselectedColor = Color.DarkGray
                )
                builtInFonts.forEach { f ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                    ) {
                        Text(f.name, color = Color.White, modifier = Modifier.weight(1f))
                        RadioButton(
                            selected = activePath == f.path,
                            onClick = {
                                // Same fix as the "Use System Default"
                                // button — go through FontManager so the
                                // cache flips atomically with the pref
                                // write. Tapping a radio button used to
                                // write the pref directly, leaving the
                                // cached typeface stale, which caused the
                                // "37 built-in fonts apply nahi ho rhe"
                                // bug the user reported.
                                FontManager.setActive(ctx, f.path)
                                fontsTick++
                            },
                            colors = fontRadioColors
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = bold, onCheckedChange = {
                        bold = it; prefs.edit().putBoolean(SettingsStore.KEY_FONT_BOLD, it).apply()
                    })
                    Spacer(Modifier.width(6.dp)); Text("Bold", color = Color.White)
                    Spacer(Modifier.width(16.dp))
                    Switch(checked = italic, onCheckedChange = {
                        italic = it; prefs.edit().putBoolean(SettingsStore.KEY_FONT_ITALIC, it).apply()
                    })
                    Spacer(Modifier.width(6.dp)); Text("Italic", color = Color.White)
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F))) {
            Column(Modifier.padding(16.dp)) {
                Text("Feedback", color = Color.White, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = haptic, onCheckedChange = {
                        haptic = it; prefs.edit().putBoolean(SettingsStore.KEY_HAPTIC, it).apply()
                    })
                    Spacer(Modifier.width(8.dp)); Text("Haptic vibration", color = Color.White)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = sound, onCheckedChange = {
                        sound = it; prefs.edit().putBoolean(SettingsStore.KEY_SOUND, it).apply()
                    })
                    Spacer(Modifier.width(8.dp)); Text("Click sound", color = Color.White)
                }
                // Volume slider for the click sound (issue #4 — give the
                // user a real on/off feature with a tunable level so they
                // can match Gboard / SwiftKey loudness).
                if (sound) {
                    Spacer(Modifier.height(4.dp))
                    Text("Click volume: $soundVolume%", color = Color.White)
                    Slider(
                        value = soundVolume.toFloat(),
                        valueRange = 5f..100f,
                        onValueChange = {
                            soundVolume = it.toInt()
                            prefs.edit().putInt(SettingsStore.KEY_SOUND_VOLUME, soundVolume).apply()
                        }
                    )
                }
            }
        }

        Text("Tip: Use the Live preview box above and adjust sliders — keyboard updates live.",
            color = Color.LightGray)
    }
}

/**
 * Tiny non-interactive mock of the real keyboard rendered with the actual
 * [theme] colors. Two rows of letter keys + a thin suggestion strip on top
 * + a colored "send" key — enough to read what the picked theme will look
 * like. Pure Compose (no Android View) so it always renders inside the
 * settings screen, even before the IME has been opened.
 */
@Composable
private fun MiniKeyboardPreview(theme: KeyboardTheme, spaceLabel: String) {
    val kbBg = Color(theme.keyboardBg)
    val keyBg = Color(theme.keyBg)
    val keyText = Color(theme.keyText)
    val suggestionBg = Color(theme.suggestionBg)
    val accent = Color(theme.accent)
    val pressed = Color(theme.pressedKey)

    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(kbBg).padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Suggestion strip
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(suggestionBg).padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("hello", color = accent, fontWeight = FontWeight.Bold)
            Text("world", color = keyText)
            Text("flex", color = keyText)
        }
        // Two key rows
        @Composable
        fun keyRow(letters: List<String>, highlightIdx: Int = -1) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                letters.forEachIndexed { idx, ch ->
                    val bg = if (idx == highlightIdx) pressed else keyBg
                    Box(
                        modifier = Modifier.weight(1f).height(28.dp).clip(RoundedCornerShape(4.dp)).background(bg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(ch, color = keyText, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
        keyRow(listOf("q","w","e","r","t","y","u","i","o","p"))
        keyRow(listOf("a","s","d","f","g","h","j","k","l"), highlightIdx = 4)
        // Bottom row with a coloured "send" tile to show the accent colour
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.weight(2f).height(28.dp).clip(RoundedCornerShape(4.dp)).background(keyBg),
                contentAlignment = Alignment.Center) { Text(spaceLabel, color = keyText) }
            Box(Modifier.weight(1f).height(28.dp).clip(RoundedCornerShape(4.dp)).background(accent),
                contentAlignment = Alignment.Center) { Text("⏎", color = Color(theme.keyboardBg), fontWeight = FontWeight.Bold) }
        }
    }
}
