package com.flexboard.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.remember
import com.flexboard.security.Obf
import com.flexboard.security.ObfConstants

private const val APP_VERSION = "1.15.0"

private val ORANGE = Color(0xFFFF8C00)
private val GOLD = Color(0xFFFFD700)
private val DARK_BG = Color(0xFF0D0D0D)
private val CARD_BG = Color(0xFF1A1A1A)

/**
 * v1.11 — About screen with a HERO developer card. The previous build
 * showed the developer in a small subdued card; the user asked for a
 * big, highlighted, cool-looking presentation. This card uses a multi-
 * stop gradient backdrop, a circular monogram avatar with glow, gradient
 * text for the name (~36sp), and a "Founder & Developer" badge.
 */
@Composable
fun AboutScreen() {
    val ctx = LocalContext.current
    val ownerName = remember { Obf.decode(ctx, ObfConstants.OWNER_NAME).ifBlank { "Kashif Raza" } }
    val ownerTeam = remember { Obf.decode(ctx, ObfConstants.OWNER_TEAM).ifBlank { "ATF Team" } }
    val instaUrl = remember { Obf.decode(ctx, ObfConstants.INSTAGRAM_URL) }
    val facebookUrl = remember { Obf.decode(ctx, ObfConstants.FACEBOOK_URL) }
    val waChannel = remember { Obf.decode(ctx, ObfConstants.WA_CHANNEL_URL) }
    val waCommunity = remember { Obf.decode(ctx, ObfConstants.WA_COMMUNITY_URL) }
    val licenseLine = remember {
        Obf.decode(ctx, ObfConstants.LICENSE_LINE)
            .ifBlank { "Kashif Raza · ATF Team. All rights reserved." }
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(DARK_BG)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        // ===== HERO DEVELOPER CARD =====
        DeveloperHeroCard(name = ownerName, team = ownerTeam)

        // ===== PRODUCT IDENTITY =====
        Card(
            colors = CardDefaults.cardColors(containerColor = CARD_BG),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("FlexBoard Pro", color = ORANGE, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(4.dp))
                Text("Version $APP_VERSION", color = Color.LightGray)
                Spacer(Modifier.height(8.dp))
                Text(
                    "A professional Android keyboard with an integrated Auto-Type engine, " +
                            "customizable themes, multi-language input, smart dictionary, macros, " +
                            "clipboard manager, font system and floating send pointer.",
                    color = Color.White
                )
                Spacer(Modifier.height(8.dp))
                LabelValueRow("Platform", "Android 7.0 and above")
                LabelValueRow("Connectivity", "Internet used only for licence check")
                LabelValueRow("Languages", "English, Urdu, Arabic")
            }
        }

        // ===== FEATURES =====
        Card(
            colors = CardDefaults.cardColors(containerColor = CARD_BG),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Features", color = ORANGE, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(6.dp))
                listOf(
                    "Input Method (IME) service",
                    "Auto-Type engine with per-character delivery",
                    "Word suggestions and dictionary",
                    "Theme engine with built-in styles",
                    "Custom font system with curated bundled fonts",
                    "Clipboard history with pinning",
                    "Macros and text expansion",
                    "Multi-language layouts (EN / UR / AR)",
                    "Floating send pointer with lock mode",
                    "Auto-saved sentences and quick recall"
                ).forEach { item ->
                    Row(verticalAlignment = Alignment.Top) {
                        Text("•  ", color = ORANGE)
                        Text(item, color = Color.White)
                    }
                }
            }
        }

        // ===== OFFICIAL CHANNELS =====
        Card(
            colors = CardDefaults.cardColors(containerColor = CARD_BG),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Official Channels", color = ORANGE, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = { openUrl(ctx, waChannel.ifBlank { "https://whatsapp.com" }) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF25D366),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) { Text("WhatsApp Channel", fontWeight = FontWeight.Bold) }
                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = { openUrl(ctx, waCommunity.ifBlank { "https://whatsapp.com" }) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF128C7E),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) { Text("WhatsApp Community", fontWeight = FontWeight.Bold) }
                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = { openUrl(ctx, instaUrl.ifBlank { "https://instagram.com" }) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE1306C),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) { Text("Instagram", fontWeight = FontWeight.Bold) }
                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = { openUrl(ctx, facebookUrl.ifBlank { "https://www.facebook.com" }) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1877F2),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) { Text("Facebook", fontWeight = FontWeight.Bold) }
            }
        }

        // ===== LEGAL =====
        Card(
            colors = CardDefaults.cardColors(containerColor = CARD_BG),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Legal", color = ORANGE, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    "© ${java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)} " + licenseLine,
                    color = Color.White
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "FlexBoard Pro does not collect or transmit your typing data. " +
                            "Internet access is used only to verify device approval against " +
                            "a list maintained by the developer.",
                    color = Color.LightGray
                )
            }
        }
    }
}

/**
 * The hero card. Big circular monogram (initials) on the left with a
 * radial-style accent ring; on the right a stack of:
 *   • "DEVELOPER & FOUNDER" badge
 *   • the name in 30sp gradient text
 *   • the team line
 *   • a small "Made with ♥ in Pakistan" footer
 */
@Composable
private fun DeveloperHeroCard(name: String, team: String) {
    val initials = name.split(" ", "·").filter { it.isNotBlank() }
        .take(2).joinToString("") { it.first().uppercaseChar().toString() }
        .ifBlank { "KR" }

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF2A1505),
                            Color(0xFF3F1E08),
                            Color(0xFF1A1300)
                        )
                    )
                )
                .border(
                    1.dp,
                    Brush.linearGradient(listOf(ORANGE.copy(alpha = 0.7f), GOLD.copy(alpha = 0.5f), Color.Transparent)),
                    RoundedCornerShape(20.dp)
                )
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Avatar with two-tone glow
                Box(contentAlignment = Alignment.Center) {
                    // Outer glow
                    Box(
                        Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(ORANGE.copy(alpha = 0.45f), Color.Transparent)
                                )
                            )
                    )
                    Box(
                        Modifier
                            .size(82.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(ORANGE, Color(0xFFE65C00), GOLD)
                                )
                            )
                            .border(2.dp, GOLD.copy(alpha = 0.7f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            initials,
                            color = Color(0xFF1A0F00),
                            fontWeight = FontWeight.Black,
                            fontSize = 32.sp
                        )
                    }
                }

                Spacer(Modifier.width(16.dp))

                Column(Modifier.weight(1f)) {
                    // Badge
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(ORANGE.copy(alpha = 0.18f))
                            .border(1.dp, ORANGE.copy(alpha = 0.6f), RoundedCornerShape(50))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "DEVELOPER · FOUNDER",
                            color = ORANGE,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    // Name — gradient text
                    Text(
                        text = buildAnnotatedString {
                            withStyle(
                                SpanStyle(
                                    brush = Brush.linearGradient(
                                        listOf(GOLD, ORANGE, Color(0xFFFF6E00))
                                    ),
                                    fontWeight = FontWeight.Black
                                )
                            ) { append(name) }
                        },
                        fontSize = 28.sp,
                        lineHeight = 32.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        team,
                        color = Color(0xFFE0E0E0),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Made with ♥ in Pakistan",
                        color = Color(0xFFAAAAAA),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun LabelValueRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(
            "$label:  ",
            color = Color.LightGray,
            modifier = Modifier.widthIn(min = 100.dp)
        )
        Text(value, color = Color.White)
    }
}

private fun openUrl(ctx: android.content.Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(intent)
    } catch (_: Exception) { /* no browser / no handler installed */ }
}
