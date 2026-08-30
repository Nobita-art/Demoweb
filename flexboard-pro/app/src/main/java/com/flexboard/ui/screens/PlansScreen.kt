package com.flexboard.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flexboard.security.AccessGate
import com.flexboard.security.DeviceId
import com.flexboard.security.KeyManager
import com.flexboard.security.Obf
import com.flexboard.security.ObfConstants
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class Plan(
    val id: String,
    val title: String,
    val pkr: Int,
    val inr: Int,
    val usdt: String,
    val badge: String? = null,
    val billingNote: String
)

private val PLANS = listOf(
    Plan("monthly",  "Monthly",  150,  50,   "0.54", null,             "Billed monthly"),
    Plan("halfyear", "6 Months", 750,  250,  "2.7",  "10% OFF",        "≈ 125 PKR / month · Billed once"),
    Plan("yearly",   "1 Year",   1400, 470,  "5.0",  "BEST VALUE · 15% OFF", "≈ 120 PKR / month · Billed once"),
    Plan("lifetime", "Lifetime", 3000, 1000, "10.7", "ONE-TIME",       "Pay once, use forever")
)

private val ORANGE       = Color(0xFFFF8C00)
private val ORANGE_DEEP  = Color(0xFFFFAA33)
private val DARK_BG      = Color(0xFF0D0D0D)
private val CARD_BG      = Color(0xFF1A1A1A)
private val CARD_BG_ACTIVE = Color(0xFF26190A)
private val WA_GREEN     = Color(0xFF25D366)
private val JAZZ_PINK    = Color(0xFFE91E8C)
private val BINANCE_GOLD = Color(0xFFF3BA2F)

@Composable
fun PlansScreen(
    state: AccessGate.AccessState,
    onRecheck: suspend () -> Unit,
    onOpenAbout: (() -> Unit)? = null,
) {
    val ctx      = LocalContext.current
    val deviceId = remember { DeviceId.get(ctx) }
    val whatsapp = remember { Obf.decode(ctx, ObfConstants.WHATSAPP_NUMBER).ifBlank { "923001677853" } }
    val owner    = remember { Obf.decode(ctx, ObfConstants.OWNER_NAME).ifBlank { "developer" } }
    val waChannel= remember { Obf.decode(ctx, ObfConstants.WA_CHANNEL_URL).ifBlank { "https://whatsapp.com" } }

    var selectedPlanId by remember { mutableStateOf("halfyear") }
    var nameInput by remember {
        mutableStateOf(
            com.flexboard.utils.SettingsStore.prefs(ctx)
                .getString(com.flexboard.utils.SettingsStore.KEY_LAST_BUYER_NAME, "").orEmpty()
        )
    }
    var nameError  by remember { mutableStateOf(false) }
    val scope      = rememberCoroutineScope()
    var rechecking by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(DARK_BG)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        StatusBanner(state)
        LiveCountdownCard(state)

        // ── Activation Key Card ─────────────────────────────────────────
        ActivationKeyCard(ctx = ctx, waChannel = waChannel, onActivated = {
            rechecking = true
            scope.launch { try { onRecheck() } finally { rechecking = false } }
        })

        // ── Buy a Plan section ──────────────────────────────────────────
        Text("Or buy a paid plan", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text(
            "Send payment and your access unlocks within ~2 minutes.",
            color = Color.LightGray, fontSize = 13.sp
        )

        PLANS.forEach { plan ->
            PlanCard(plan = plan, selected = selectedPlanId == plan.id, onClick = { selectedPlanId = plan.id })
        }

        PaymentInfoCard()

        // ── Buyer Name ──────────────────────────────────────────────────
        Card(colors = CardDefaults.cardColors(containerColor = CARD_BG), shape = RoundedCornerShape(14.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("Your name", color = ORANGE, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(2.dp))
                Text("Required — we add this name beside your device ID in our records.",
                    color = Color.LightGray, fontSize = 12.sp)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it.take(40); if (nameError && it.isNotBlank()) nameError = false },
                    placeholder    = { Text("e.g. Ali Khan", color = Color.Gray) },
                    isError        = nameError,
                    supportingText = { if (nameError) Text("Name is required.", color = Color(0xFFFF6464)) },
                    singleLine     = true,
                    modifier       = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = ORANGE, unfocusedBorderColor = Color(0xFF555555),
                        focusedTextColor     = Color.White, unfocusedTextColor = Color.White,
                        cursorColor          = ORANGE, errorBorderColor = Color(0xFFFF6464)
                    )
                )
            }
        }

        // ── Device ID ───────────────────────────────────────────────────
        val ctx2 = LocalContext.current
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141414)),
            shape  = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, ORANGE.copy(alpha = 0.55f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Your Device ID", color = ORANGE, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                    Text("Share with developer", color = Color.Gray, fontSize = 11.sp)
                }
                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .background(Color.Black)
                        .border(1.dp, ORANGE.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                        .padding(14.dp)
                ) {
                    Text(deviceId, color = Color.White,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontSize = 14.sp, fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        val cm = ctx2.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        cm.setPrimaryClip(android.content.ClipData.newPlainText("FlexBoard device id", deviceId))
                        Toast.makeText(ctx2, "Copied!", Toast.LENGTH_SHORT).show()
                    },
                    colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A2A), contentColor = Color.White),
                    shape    = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) { Text("Copy Device ID", fontWeight = FontWeight.SemiBold, fontSize = 14.sp) }
            }
        }

        // ── Buy via WhatsApp ────────────────────────────────────────────
        Button(
            onClick = {
                val trimmed = nameInput.trim()
                if (trimmed.isEmpty()) { nameError = true; Toast.makeText(ctx, "Please enter your name first.", Toast.LENGTH_SHORT).show(); return@Button }
                com.flexboard.utils.SettingsStore.prefs(ctx).edit().putString(com.flexboard.utils.SettingsStore.KEY_LAST_BUYER_NAME, trimmed).apply()
                val plan = PLANS.first { it.id == selectedPlanId }
                openWhatsAppPurchase(ctx, whatsapp, owner, trimmed, deviceId, plan)
            },
            colors   = ButtonDefaults.buttonColors(containerColor = WA_GREEN, contentColor = Color.Black),
            shape    = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) { Text("Buy via WhatsApp", fontWeight = FontWeight.Bold, fontSize = 16.sp) }

        // ── Re-check ────────────────────────────────────────────────────
        OutlinedButton(
            onClick = {
                if (rechecking) return@OutlinedButton
                rechecking = true
                scope.launch { try { onRecheck() } finally { rechecking = false } }
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape    = RoundedCornerShape(12.dp)
        ) {
            Text(if (rechecking) "Re-checking..." else "Re-check approval", color = ORANGE, fontWeight = FontWeight.SemiBold)
        }

        Text(
            "After payment, your access unlocks within ~2 minutes once we add your ID. " +
            "Make sure you have an internet connection.",
            color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        )
        Spacer(Modifier.height(20.dp))
    }
}

// ── Activation Key Card ───────────────────────────────────────────────────────

@Composable
private fun ActivationKeyCard(ctx: Context, waChannel: String, onActivated: () -> Unit) {
    var keyInput   by remember { mutableStateOf(KeyManager.getSavedKey(ctx)) }
    var keyError   by remember { mutableStateOf("") }
    var activating by remember { mutableStateOf(false) }
    val scope      = rememberCoroutineScope()

    val currentKeyBad = remember(ctx) {
        val s = KeyManager.cachedState(ctx)
        s is KeyManager.KeyState.KeyInvalid || s is KeyManager.KeyState.KeyExpired
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(18.dp),
        colors   = CardDefaults.cardColors(containerColor = Color(0xFF081808)),
        border   = androidx.compose.foundation.BorderStroke(1.5.dp, WA_GREEN.copy(alpha = 0.55f))
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🔑", fontSize = 22.sp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Active via Approval Key", color = WA_GREEN, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text("Enter your activation key to unlock FlexBoard Pro", color = Color(0xFF9AE69A), fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(14.dp))

            OutlinedTextField(
                value         = keyInput,
                onValueChange = { keyInput = it.uppercase().take(30); keyError = "" },
                placeholder    = { Text("e.g. FLEX-ABCD-1234", color = Color.Gray) },
                isError        = keyError.isNotEmpty() || (currentKeyBad && keyError.isEmpty()),
                supportingText = {
                    val msg = when {
                        keyError.isNotEmpty() -> keyError
                        currentKeyBad         -> "This key is invalid or expired. Get a new one from the channel."
                        else                  -> null
                    }
                    if (msg != null) Text(msg, color = Color(0xFFFF6464))
                },
                singleLine = true,
                modifier   = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = WA_GREEN, unfocusedBorderColor = Color(0xFF3A5A3A),
                    focusedTextColor     = Color.White, unfocusedTextColor = Color.White,
                    cursorColor          = WA_GREEN, errorBorderColor = Color(0xFFFF6464)
                )
            )

            Spacer(Modifier.height(10.dp))

            Button(
                onClick = {
                    val trimmed = keyInput.trim()
                    if (trimmed.length < 4) { keyError = "Please enter a valid activation key."; return@Button }
                    activating = true
                    scope.launch {
                        try {
                            when (KeyManager.checkKey(ctx, key = trimmed, force = true)) {
                                is KeyManager.KeyState.KeyActive  -> { Toast.makeText(ctx, "Key activated! Unlocking...", Toast.LENGTH_SHORT).show(); onActivated() }
                                is KeyManager.KeyState.KeyExpired -> keyError = "This key has expired. Join the channel for a new key."
                                is KeyManager.KeyState.KeyInvalid -> keyError = "Invalid key. Check the key carefully and try again."
                                else -> keyError = "Could not verify key. Check your internet and try again."
                            }
                        } finally { activating = false }
                    }
                },
                enabled  = !activating,
                colors   = ButtonDefaults.buttonColors(containerColor = WA_GREEN, contentColor = Color.Black),
                shape    = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) { Text(if (activating) "Checking..." else "Activate Key", fontWeight = FontWeight.Bold, fontSize = 15.sp) }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = Color(0xFF2A4A2A))
            Spacer(Modifier.height(14.dp))

            Text("Don't have an activation key?", color = Color(0xFF9AE69A), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
            Text("Join our WhatsApp community to get a free activation key.", color = Color.LightGray, fontSize = 12.sp)
            Spacer(Modifier.height(10.dp))

            Button(
                onClick = {
                    try { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(waChannel)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) {}
                },
                colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A3A1A), contentColor = WA_GREEN),
                shape    = RoundedCornerShape(12.dp),
                border   = androidx.compose.foundation.BorderStroke(1.dp, WA_GREEN.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth().height(46.dp)
            ) { Text("📢  Join WhatsApp Channel", fontWeight = FontWeight.Bold, fontSize = 14.sp) }
        }
    }
}

// ── Payment Info ──────────────────────────────────────────────────────────────

@Composable
private fun PaymentInfoCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111111)),
        shape  = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333333)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("💳  Payment Methods", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Account Holder: Kashif Raza", color = Color.LightGray, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PaymentMethodChip("🏦", "JazzCash",  "03001677853", JAZZ_PINK,        Modifier.weight(1f))
                PaymentMethodChip("📱", "Easypaisa", "03001677853", Color(0xFF00A550), Modifier.weight(1f))
            }
            PaymentMethodChip("🟡", "Binance", "UID: 439629680", BINANCE_GOLD, Modifier.fillMaxWidth())
            Text("Send payment screenshot on WhatsApp after transfer.", color = Color.Gray, fontSize = 12.sp)
        }
    }
}

@Composable
private fun PaymentMethodChip(emoji: String, label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Box(modifier.clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = 0.10f))
        .border(1.dp, color.copy(alpha = 0.40f), RoundedCornerShape(12.dp)).padding(14.dp, 12.dp)) {
        Column {
            Text("$emoji  $label", color = color, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
            Text(value, color = Color.White, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ── Status Banner ─────────────────────────────────────────────────────────────

@Composable
private fun StatusBanner(state: AccessGate.AccessState) {
    val (title, subtitle, tone) = when (state) {
        is AccessGate.AccessState.TrialActive      -> Triple("🎁  Trial active", "You have a trial. Enter an activation key or pick a plan to continue after it ends.", ORANGE_DEEP)
        AccessGate.AccessState.TrialExpired        -> Triple("⏰  Trial ended", "Your trial has ended. Enter an activation key or buy a plan to continue.", Color(0xFFFFB070))
        AccessGate.AccessState.NotApproved         -> Triple("🔒  Not activated", "Enter your activation key below, or buy a plan and send your Device ID to us on WhatsApp.", Color(0xFF66BBFF))
        AccessGate.AccessState.KeyInvalid          -> Triple("🔑  Invalid key", "The key you entered is invalid or expired. Get a new key from the WhatsApp channel.", Color(0xFFFF6464))
        is AccessGate.AccessState.PlanExpired      -> Triple("⏰  Plan expired", "Your active plan has ended. Renew below to continue using FlexBoard Pro.", Color(0xFFFFB070))
        is AccessGate.AccessState.ApprovedWithPlan -> Triple("✅  Plan active", "Your plan is active. Countdown shows remaining time.", Color(0xFF7CFF7C))
        AccessGate.AccessState.ApprovedLifetime    -> Triple("✅  Lifetime access", "You have unlimited access.", Color(0xFF7CFF7C))
        AccessGate.AccessState.KeyActive           -> Triple("✅  Key active", "Your activation key is valid. Enjoy FlexBoard Pro!", Color(0xFF7CFF7C))
        AccessGate.AccessState.Blocked             -> Triple("🚫  Access revoked", "Contact us on WhatsApp if you think this is a mistake.", Color(0xFFFF6464))
        AccessGate.AccessState.Tampered            -> Triple("⚠️  Build invalid", "This APK appears to have been modified. Please reinstall an official copy.", Color(0xFFFF6464))
        is AccessGate.AccessState.OfflineUnknown   -> Triple("📶  Internet required", "FlexBoard Pro needs internet to verify your access. Connect and try again.", Color(0xFF66BBFF))
    }
    Card(colors = CardDefaults.cardColors(containerColor = CARD_BG), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, color = tone, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(6.dp))
            Text(subtitle, color = Color.LightGray, fontSize = 13.sp)
        }
    }
}

// ── Live Countdown ────────────────────────────────────────────────────────────

@Composable
private fun LiveCountdownCard(state: AccessGate.AccessState) {
    val untilMs: Long; val label: String; val accent: Color
    when (state) {
        is AccessGate.AccessState.TrialActive      -> { untilMs = state.trialUntilMs; label = "Trial ends in";   accent = ORANGE_DEEP }
        is AccessGate.AccessState.ApprovedWithPlan -> { untilMs = state.planUntilMs;  label = "Plan ends in";    accent = Color(0xFF7CFF7C) }
        else -> return
    }
    var tick by remember { mutableStateOf(0) }
    LaunchedEffect(state) { while (true) { delay(1000L); tick++ } }
    @Suppress("UNUSED_EXPRESSION") tick
    val live  = (untilMs - System.currentTimeMillis()).coerceAtLeast(0L)
    val days  = live / 86400000L; val hours = (live % 86400000L) / 3600000L
    val mins  = (live % 3600000L) / 60000L; val secs = (live % 60000L) / 1000L
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = CARD_BG_ACTIVE)) {
        Column(Modifier.background(Brush.horizontalGradient(listOf(Color(0xFF1A1300), Color(0xFF26190A)))).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = accent, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CountChip(days.toString().padStart(2,'0'), "DAYS"); CountChip(hours.toString().padStart(2,'0'), "HRS")
                CountChip(mins.toString().padStart(2,'0'), "MIN");  CountChip(secs.toString().padStart(2,'0'), "SEC")
            }
        }
    }
}

@Composable
private fun CountChip(value: String, label: String) {
    Column(modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Color.Black)
        .border(1.dp, ORANGE.copy(alpha = 0.4f), RoundedCornerShape(10.dp)).padding(14.dp, 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = ORANGE, fontWeight = FontWeight.Black, fontSize = 24.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
        Text(label, color = Color.LightGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

// ── Plan Cards ────────────────────────────────────────────────────────────────

@Composable
private fun PlanCard(plan: Plan, selected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .border(if (selected) 2.dp else 1.dp, if (selected) ORANGE else Color(0xFF333333), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape  = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (selected) CARD_BG_ACTIVE else CARD_BG)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (selected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, null, tint = if (selected) ORANGE else Color(0xFF666666))
                Spacer(Modifier.width(10.dp))
                Text(plan.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.weight(1f))
                if (plan.badge != null) {
                    Box(Modifier.clip(RoundedCornerShape(50)).background(ORANGE.copy(alpha = 0.18f))
                        .border(1.dp, ORANGE.copy(alpha = 0.5f), RoundedCornerShape(50)).padding(10.dp, 4.dp)) {
                        Text(plan.badge, color = ORANGE, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PriceChip("🇵🇰", "${plan.pkr} PKR", Modifier.weight(1f))
                PriceChip("🇮🇳", "${plan.inr} INR", Modifier.weight(1f))
                PriceChip("💲", "${plan.usdt} USDT", Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Text(plan.billingNote, color = Color.Gray, fontSize = 12.sp)
        }
    }
}

@Composable
private fun PriceChip(flag: String, price: String, modifier: Modifier = Modifier) {
    Box(modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFF0D0D0D))
        .border(1.dp, Color(0xFF333333), RoundedCornerShape(8.dp)).padding(8.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(flag, fontSize = 16.sp)
            Spacer(Modifier.height(2.dp))
            Text(price, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.Center)
        }
    }
}

// ── WhatsApp purchase helper ──────────────────────────────────────────────────

private fun openWhatsAppPurchase(ctx: Context, waNumber: String, owner: String, name: String, deviceId: String, plan: Plan) {
    val msg = "Hi $owner! I want to buy FlexBoard Pro.%0A%0A📦 Plan: ${plan.title}%0A💰 Price: ${plan.pkr} PKR / ${plan.inr} INR / ${plan.usdt} USDT%0A%0A👤 Name: $name%0A📱 Device ID: $deviceId%0A%0A💳 Sending payment to:%0A  • JazzCash / Easypaisa: 03001677853%0A  • Binance UID: 439629680%0A%0ASending payment screenshot now. Please activate. Thank you!"
        .replace(" ", "%20")
    try {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$waNumber?text=$msg")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Exception) { Toast.makeText(ctx, "WhatsApp not installed", Toast.LENGTH_SHORT).show() }
}
