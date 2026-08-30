package com.flexboard.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.flexboard.security.UpdateChecker

/**
 * Non-dismissable update dialog.
 *
 * Shown on top of everything (including GatedApp content) when the server
 * reports a newer version. The user cannot dismiss it — the only action is
 * "Update Now" which opens the download URL in the browser.
 *
 * The dialog stays visible until the user actually installs the new version
 * (the version check will no longer flag an update after that).
 */
@Composable
fun UpdateDialog(info: UpdateChecker.UpdateInfo) {
    val ctx = LocalContext.current

    Dialog(
        onDismissRequest = { /* intentionally non-dismissable */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0xCC000000)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF1A1A1A), Color(0xFF141414))
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🚀",
                    fontSize = 48.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Text(
                    text = "Update Required",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF6B2B),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "v${info.latestVersion} is available",
                    fontSize = 14.sp,
                    color = Color(0xFF888888),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(16.dp))

                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF222222), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        text = info.releaseNotes,
                        fontSize = 13.sp,
                        color = Color(0xFFCCCCCC),
                        textAlign = TextAlign.Start,
                        lineHeight = 20.sp
                    )
                }

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(info.downloadUrl))
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            ctx.startActivity(intent)
                        } catch (_: Throwable) {}
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF6B2B)
                    )
                ) {
                    Text(
                        text = "Update Now",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(Modifier.height(10.dp))

                Text(
                    text = "This update is required to continue using FlexBoard Pro",
                    fontSize = 11.sp,
                    color = Color(0xFF555555),
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
