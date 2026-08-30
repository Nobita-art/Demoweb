package com.flexboard.ui.screens

import android.content.Context
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

private fun safeImeEnabled(ctx: Context): Boolean = try {
    val list = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ENABLED_INPUT_METHODS) ?: ""
    list.contains("com.flexboard")
} catch (_: Throwable) { false }

private fun safeImeDefault(ctx: Context): Boolean = try {
    val cur = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD) ?: ""
    cur.contains("com.flexboard")
} catch (_: Throwable) { false }

private fun safeAccessibilityOn(ctx: Context): Boolean = try {
    val flag = try { Settings.Secure.getInt(ctx.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED) } catch (_: Throwable) { 0 }
    val list = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
    flag == 1 && list.contains("com.flexboard")
} catch (_: Throwable) { false }

@Composable
fun HomeScreen(onOpenImeSettings: () -> Unit, onOpenAccessibility: () -> Unit) {
    val ctx = LocalContext.current
    var refreshTick by remember { mutableStateOf(0) }

    val imeEnabled = remember(refreshTick) { safeImeEnabled(ctx) }
    val imeSelected = remember(refreshTick) { safeImeDefault(ctx) }
    val accessibilityOn = remember(refreshTick) { safeAccessibilityOn(ctx) }

    val allReady = imeEnabled && imeSelected && accessibilityOn

    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ===== PRODUCT HEADER =====
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F))) {
            Column(Modifier.padding(16.dp)) {
                Text("FlexBoard Pro", color = Color(0xFFFF8C00), fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(
                    "A professional Android keyboard with an integrated Auto-Type engine, " +
                            "customizable themes, multi-language support, dictionary, macros, " +
                            "clipboard manager and font system.",
                    color = Color.White
                )
            }
        }

        // ===== SETUP STATUS SUMMARY =====
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (allReady) Color(0xFF14361B) else Color(0xFF2A1F0F)
            )
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    if (allReady) "Setup complete" else "Setup required",
                    color = if (allReady) Color(0xFF7CFF7C) else Color(0xFFFFB070),
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (allReady)
                        "FlexBoard Pro is enabled, selected as the active input method, and accessibility is granted. " +
                                "You can use the keyboard in any application."
                    else
                        "Complete the steps below to activate the keyboard.",
                    color = Color.White
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { refreshTick++ }) { Text("Re-check status") }
            }
        }

        // ===== SETUP STEPS =====
        StepCard(
            number = "1",
            title = "Enable the keyboard",
            body = "Open System Settings, navigate to Languages & Input → Manage Keyboards, " +
                    "and toggle FlexBoard Pro to ON.",
            action = "Open Keyboard Settings",
            done = imeEnabled,
            onClick = onOpenImeSettings
        )

        StepCard(
            number = "2",
            title = "Set as default input method",
            body = "Tap any text field and select FlexBoard Pro from the input method picker, " +
                    "or long-press the spacebar.",
            action = "Open Input Picker",
            done = imeSelected,
            onClick = {
                try {
                    val imm = ctx.getSystemService(Context.INPUT_METHOD_SERVICE)
                            as? android.view.inputmethod.InputMethodManager
                    imm?.showInputMethodPicker()
                } catch (_: Throwable) {}
            }
        )

        StepCard(
            number = "3",
            title = "Grant Accessibility permission",
            body = "Required for the Auto-Type engine to detect and tap the Send button " +
                    "in messaging applications such as WhatsApp, Messenger and Telegram.",
            action = "Open Accessibility Settings",
            done = accessibilityOn,
            onClick = onOpenAccessibility
        )
    }
}

@Composable
private fun StepCard(
    number: String,
    title: String,
    body: String,
    action: String,
    done: Boolean,
    onClick: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F))) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Step $number  ", color = Color(0xFFFF8C00), fontWeight = FontWeight.Bold)
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                val (statusText, statusColor) = if (done)
                    "Completed" to Color(0xFF7CFF7C)
                else
                    "Pending" to Color(0xFFFFB070)
                Text(statusText, color = statusColor, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(6.dp))
            Text(body, color = Color.White)
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8C00), contentColor = Color.Black)
            ) { Text(action) }
        }
    }
}
