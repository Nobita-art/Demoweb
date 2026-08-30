package com.flexboard.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.flexboard.ime.AutoTypeEngine
import com.flexboard.ime.FloatingPointerService
import com.flexboard.utils.SettingsStore

@Composable
fun AutoTypeScreen(
    onPickTxt: () -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit
) {
    val ctx = LocalContext.current
    val prefs = SettingsStore.prefs(ctx)
    var delay by remember { mutableStateOf(prefs.getInt(SettingsStore.KEY_AT_DELAY, 5)) }
    var loop by remember { mutableStateOf(prefs.getBoolean(SettingsStore.KEY_AT_LOOP, false)) }
    var autoSend by remember { mutableStateOf(prefs.getBoolean(SettingsStore.KEY_AT_AUTO_SEND, true)) }
    var sendMethod by remember { mutableStateOf(prefs.getString(SettingsStore.KEY_AT_SEND_METHOD, "auto") ?: "auto") }
    var sendDelay by remember { mutableStateOf(prefs.getInt(SettingsStore.KEY_AT_SEND_DELAY_MS, 300)) }
    var charDelay by remember { mutableStateOf(prefs.getInt(SettingsStore.KEY_AT_CHAR_DELAY_MS, 35)) }
    var startLine by remember { mutableStateOf(prefs.getInt(SettingsStore.KEY_AT_START_LINE, 0)) }
    var customText by remember { mutableStateOf(prefs.getString(SettingsStore.KEY_AT_CUSTOM_TEXT, "") ?: "") }
    var targetName by remember { mutableStateOf(prefs.getString(SettingsStore.KEY_AT_TARGET_NAME, "") ?: "") }
    var pointerOn by remember { mutableStateOf(prefs.getBoolean(SettingsStore.KEY_POINTER_ENABLED, false)) }
    var pointerLocked by remember { mutableStateOf(prefs.getBoolean(SettingsStore.KEY_POINTER_LOCKED, false)) }
    var permTick by remember { mutableStateOf(0) }
    val canDrawOverlay = remember(permTick) { Settings.canDrawOverlays(ctx) }
    val pointerX = remember(permTick, pointerOn, pointerLocked) { prefs.getInt(SettingsStore.KEY_POINTER_X, -1) }
    val pointerY = remember(permTick, pointerOn, pointerLocked) { prefs.getInt(SettingsStore.KEY_POINTER_Y, -1) }

    // If the toolbar "File Import" tile was tapped from the IME, auto-launch
    // the file picker immediately so the user doesn't need a second tap.
    LaunchedEffect(Unit) {
        if (prefs.getBoolean(SettingsStore.KEY_AUTO_PICK_FILE, false)) {
            prefs.edit().putBoolean(SettingsStore.KEY_AUTO_PICK_FILE, false).apply()
            onPickTxt()
        }
    }

    val state by AutoTypeEngine.state.collectAsState()
    var imeTick by remember { mutableStateOf(0) }
    val imeReady = remember(imeTick, state.running) { AutoTypeEngine.isImeReady }
    LaunchedEffect(Unit) {
        while (true) { kotlinx.coroutines.delay(800); imeTick++ }
    }

    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Auto-Type Engine", color = Color(0xFFFF8C00), fontWeight = FontWeight.Bold)

        // ===== IME readiness banner =====
        Card(colors = CardDefaults.cardColors(
            containerColor = if (imeReady) Color(0xFF1B3B1B) else Color(0xFF3B1B1B)
        )) {
            Column(Modifier.padding(12.dp)) {
                Text(
                    if (imeReady) "FlexBoard keyboard active — ready to type"
                    else "FlexBoard keyboard not active",
                    color = if (imeReady) Color(0xFF8DFF8D) else Color(0xFFFF8D8D),
                    fontWeight = FontWeight.Bold
                )
                if (!imeReady) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Set FlexBoard Pro as the default input method, then open the target " +
                                "application and tap the message field before starting Auto-Type.",
                        color = Color.LightGray
                    )
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F))) {
            Column(Modifier.padding(16.dp)) {
                Text("Source A · Text file (.txt)", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Button(onClick = onPickTxt, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8C00), contentColor = Color.Black)) {
                    Text("Load .txt File")
                }
                Spacer(Modifier.height(6.dp))
                val name = state.sourceName.ifBlank { "—" }
                Text("Loaded: $name · ${state.total} lines", color = Color.White)
            }
        }

        // v1.10 — optional Target Name. When non-blank, AutoTypeEngine
        // prepends "$name " to every line typed (both file and custom
        // text sources). Useful for addressing each chat reply to a
        // specific person, e.g. "Raza " + "kaisa hai" → "Raza kaisa hai".
        // Persisted immediately on every keystroke so user sees the
        // value next time the screen opens.
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F))) {
            Column(Modifier.padding(16.dp)) {
                Text("Target Name (optional)", color = Color(0xFFFF8C00), fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "If set, this name is typed before each line. " +
                            "Example: \"Raza\" + line \"kaisa hai\" → \"Raza kaisa hai\". " +
                            "Leave blank to type lines exactly as-is.",
                    color = Color.LightGray
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = targetName,
                    onValueChange = {
                        targetName = it
                        prefs.edit().putString(SettingsStore.KEY_AT_TARGET_NAME, it).apply()
                    },
                    label = { Text("e.g. Raza, Ahmed, Sana...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F))) {
            Column(Modifier.padding(16.dp)) {
                Text("Source B · Custom write text", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = customText,
                    onValueChange = { customText = it },
                    label = { Text("Type or paste lines (one message per line)") },
                    minLines = 4, maxLines = 10,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        AutoTypeEngine.loadFromText(ctx, customText, "Custom text")
                    }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8C00), contentColor = Color.Black)) {
                        Text("Use this text")
                    }
                    OutlinedButton(onClick = { customText = "" }) { Text("Clear") }
                }
            }
        }

        // ============== POINTER OVERLAY ==============
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F))) {
            Column(Modifier.padding(16.dp)) {
                Text("Floating Send Pointer", color = Color(0xFFFF8C00), fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "A draggable indicator that can be positioned over any application's " +
                            "Send control. Used as a fallback for applications not handled " +
                            "by the smart send detector.",
                    color = Color.LightGray
                )
                Spacer(Modifier.height(8.dp))

                if (!canDrawOverlay) {
                    Text("Overlay permission required", color = Color(0xFFFF6A6A))
                    Spacer(Modifier.height(6.dp))
                    Button(onClick = {
                        try {
                            val i = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${ctx.packageName}")
                            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            ctx.startActivity(i)
                        } catch (_: Exception) {}
                    }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8C00), contentColor = Color.Black)) {
                        Text("Grant Overlay Permission")
                    }
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(onClick = { permTick++ }) { Text("I granted it — refresh") }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = pointerOn, onCheckedChange = {
                            pointerOn = it
                            prefs.edit().putBoolean(SettingsStore.KEY_POINTER_ENABLED, it).apply()
                            if (it) FloatingPointerService.start(ctx)
                            else FloatingPointerService.stop(ctx)
                        })
                        Spacer(Modifier.width(8.dp))
                        Text("Pointer overlay ON", color = Color.White)
                    }
                    if (pointerOn) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            if (pointerX < 0) "Position: not set yet — drag the orange dot to your SEND button"
                            else "Position: ($pointerX, $pointerY)",
                            color = Color.LightGray
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(checked = pointerLocked, onCheckedChange = {
                                pointerLocked = it
                                if (it) FloatingPointerService.lock(ctx) else FloatingPointerService.unlock(ctx)
                                permTick++
                            })
                            Spacer(Modifier.width(8.dp))
                            Text("Lock pointer (touches pass through)", color = Color.White)
                        }
                        Spacer(Modifier.height(6.dp))
                        OutlinedButton(onClick = {
                            prefs.edit().remove(SettingsStore.KEY_POINTER_X).remove(SettingsStore.KEY_POINTER_Y).apply()
                            FloatingPointerService.stop(ctx)
                            FloatingPointerService.start(ctx)
                            permTick++
                        }) { Text("Reset position") }
                    }
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F))) {
            Column(Modifier.padding(16.dp)) {
                Text("Settings", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Delay between messages: $delay sec", color = Color.White)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = {
                        delay = (delay - 1).coerceAtLeast(1); prefs.edit().putInt(SettingsStore.KEY_AT_DELAY, delay).apply()
                    }) { Text("-") }
                    Slider(
                        value = delay.toFloat(),
                        onValueChange = { delay = it.toInt().coerceIn(1, 60); prefs.edit().putInt(SettingsStore.KEY_AT_DELAY, delay).apply() },
                        valueRange = 1f..60f,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                    )
                    OutlinedButton(onClick = {
                        delay = (delay + 1).coerceAtMost(60); prefs.edit().putInt(SettingsStore.KEY_AT_DELAY, delay).apply()
                    }) { Text("+") }
                }
                Spacer(Modifier.height(8.dp))
                Text("Typing speed: $charDelay ms / character (lower = faster)", color = Color.White)
                Slider(
                    value = charDelay.toFloat(), valueRange = 0f..200f,
                    onValueChange = {
                        charDelay = it.toInt().coerceIn(0, 200)
                        prefs.edit().putInt(SettingsStore.KEY_AT_CHAR_DELAY_MS, charDelay).apply()
                    }
                )
                Text(
                    "Each character is delivered through the input method one at a time. " +
                            "Lower values increase typing speed.",
                    color = Color.LightGray,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text("Send delay (after typing each line): $sendDelay ms", color = Color.White)
                Slider(
                    value = sendDelay.toFloat(), valueRange = 0f..2000f,
                    onValueChange = {
                        sendDelay = it.toInt().coerceIn(0, 2000)
                        prefs.edit().putInt(SettingsStore.KEY_AT_SEND_DELAY_MS, sendDelay).apply()
                    }
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = autoSend, onCheckedChange = {
                        autoSend = it; prefs.edit().putBoolean(SettingsStore.KEY_AT_AUTO_SEND, it).apply()
                    })
                    Spacer(Modifier.width(8.dp))
                    Text("Auto-send after each line", color = Color.White)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = loop, onCheckedChange = {
                        loop = it; prefs.edit().putBoolean(SettingsStore.KEY_AT_LOOP, it).apply()
                    })
                    Spacer(Modifier.width(8.dp)); Text("Loop mode", color = Color.White)
                }
                Spacer(Modifier.height(8.dp))
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF161616))) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Smart Send", color = Color(0xFFFF8C00), fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Auto-Type selects the appropriate send action for the active " +
                                    "application: a Send button tap for supported messaging " +
                                    "applications, or the field's IME action (Enter / Go / " +
                                    "Search / Done) for browsers, search boxes and forms.",
                            color = Color.LightGray
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                var showAdvanced by remember { mutableStateOf(false) }
                TextButton(onClick = { showAdvanced = !showAdvanced }) {
                    Text(
                        if (showAdvanced) "Hide advanced send options" else "Show advanced send options",
                        color = Color(0xFFFF8C00)
                    )
                }
                if (showAdvanced) {
                    Text("Send method override", color = Color.White)
                    Row {
                        listOf(
                            "auto" to "Auto",
                            "ime" to "IME Enter",
                            "accessibility" to "Scan button",
                            "pointer" to "Pointer click"
                        ).forEach { (id, label) ->
                            FilterChip(selected = sendMethod == id, onClick = {
                                sendMethod = id; prefs.edit().putString(SettingsStore.KEY_AT_SEND_METHOD, id).apply()
                            }, label = { Text(label) }, modifier = Modifier.padding(end = 6.dp))
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Auto is recommended for normal use. Override only when " +
                                "diagnosing a specific application.",
                        color = Color.LightGray
                    )
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = startLine.toString(),
                    onValueChange = {
                        startLine = it.toIntOrNull() ?: 0
                        prefs.edit().putInt(SettingsStore.KEY_AT_START_LINE, startLine).apply()
                    },
                    label = { Text("Start from line (0 = beginning)") },
                    singleLine = true
                )
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F))) {
            Column(Modifier.padding(16.dp)) {
                val total = state.total.coerceAtLeast(1)
                LinearProgressIndicator(
                    progress = { (state.current.toFloat() / total).coerceIn(0f, 1f) },
                    color = Color(0xFFFF8C00),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                Text("Message ${state.current} of ${state.total}", color = Color.White)
                if (state.currentLine.isNotEmpty()) Text("> ${state.currentLine}", color = Color.LightGray)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        // Kick off the typer. MainActivity's onStartAutoType
                        // callback handles the moveTaskToBack itself (and
                        // marks the expected programmatic minimize so onStop
                        // doesn't pause the typer right away).
                        onStart()
                    }, enabled = !state.running,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8C00), contentColor = Color.Black)
                    ) { Text("Start") }
                    if (state.paused) {
                        Button(onClick = onResume) { Text("Resume") }
                    } else {
                        Button(onClick = onPause, enabled = state.running) { Text("Pause") }
                    }
                    OutlinedButton(onClick = onStop) { Text("Stop") }
                }
                state.lastError?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = Color(0xFFFF6A6A))
                }
            }
        }

    }
}
