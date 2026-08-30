package com.flexboard.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flexboard.security.AccessGate
import com.flexboard.security.DeviceId
import com.flexboard.security.Obf
import com.flexboard.security.ObfConstants
import kotlinx.coroutines.launch

/**
 * v1.11 — full-screen lock for the truly-blocked outcomes (Blocked,
 * Tampered, OfflineUnknown). The TrialExpired / PlanExpired /
 * NotApproved outcomes are routed to [PlansScreen] instead so the
 * user gets the purchase path, not just a static message.
 */
@Composable
fun LockScreen(state: AccessGate.AccessState, onRecheck: suspend () -> Unit) {
    val ctx = LocalContext.current
    val deviceId = remember { DeviceId.get(ctx) }
    val whatsapp = remember { Obf.decode(ctx, ObfConstants.WHATSAPP_NUMBER) }
    val owner = remember { Obf.decode(ctx, ObfConstants.OWNER_NAME) }
    val scope = rememberCoroutineScope()
    var checking by remember { mutableStateOf(false) }

    val title: String
    val subtitle: String
    val tone: Color
    when (state) {
        AccessGate.AccessState.Blocked -> {
            title = "Access revoked"
            subtitle = "Your access to FlexBoard Pro has been revoked by the developer. " +
                    "Contact us on WhatsApp if you think this is a mistake."
            tone = Color(0xFFFF4444)
        }
        AccessGate.AccessState.Tampered -> {
            title = "Build invalid"
            subtitle = "This APK appears to have been modified or re-signed. " +
                    "Please reinstall an official copy from the developer."
            tone = Color(0xFFFF4444)
        }
        is AccessGate.AccessState.OfflineUnknown -> {
            title = "Internet required"
            subtitle = "FlexBoard Pro needs an internet connection to verify your access. " +
                    "Connect to Wi-Fi or mobile data and tap Re-check."
            tone = Color(0xFF66BBFF)
        }
        else -> {
            title = "Locked"
            subtitle = "FlexBoard Pro is currently unavailable on this device."
            tone = Color(0xFFFFAA33)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(32.dp))
        Text(
            "FlexBoard Pro",
            color = Color(0xFFFF8C00),
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp
        )
        Text(title, color = tone, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        Text(subtitle, color = Color.LightGray)

        Spacer(Modifier.height(8.dp))
        Text("Your device ID", color = Color(0xFFFF8C00), fontWeight = FontWeight.Bold)

        Box(
            Modifier
                .fillMaxWidth()
                .background(Color(0xFF1F1F1F), RoundedCornerShape(8.dp))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                deviceId,
                color = Color.White,
                fontFamily = FontFamily.Monospace,
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("FlexBoard device id", deviceId))
                    Toast.makeText(ctx, "ID copied", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF333333),
                    contentColor = Color.White
                ),
                modifier = Modifier.weight(1f)
            ) { Text("Copy ID") }

            Button(
                onClick = {
                    val text = "Approval request%0AID%3A%20$deviceId"
                    val number = whatsapp.ifBlank { "923001677853" }
                    val url = "https://wa.me/$number?text=$text"
                    try {
                        ctx.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    } catch (_: Exception) {
                        Toast.makeText(ctx, "WhatsApp not installed", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF25D366),
                    contentColor = Color.Black
                ),
                modifier = Modifier.weight(1.6f)
            ) {
                Text(
                    "Send to ${owner.ifBlank { "developer" }} via WhatsApp",
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                if (checking) return@Button
                checking = true
                scope.launch { try { onRecheck() } finally { checking = false } }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF8C00),
                contentColor = Color.Black
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (checking) "Re-checking..." else "Re-check approval",
                fontWeight = FontWeight.Bold
            )
        }
    }
}
