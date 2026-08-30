package com.flexboard.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.flexboard.data.db.AppDatabase
import com.flexboard.utils.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun SavedSentencesScreen() {
    val ctx = LocalContext.current
    val dao = remember { AppDatabase.get(ctx).savedSentenceDao() }
    val items by dao.top(200).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val prefs = SettingsStore.prefs(ctx)
    var enabled by remember { mutableStateOf(prefs.getBoolean(SettingsStore.KEY_AUTO_SAVE_SENTENCE, true)) }
    var minLen by remember { mutableStateOf(prefs.getInt(SettingsStore.KEY_AUTO_SAVE_MIN_LEN, 12)) }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Auto-Saved Sentences", color = Color(0xFFFF8C00), fontWeight = FontWeight.Bold)

        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F))) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = enabled, onCheckedChange = {
                        enabled = it; prefs.edit().putBoolean(SettingsStore.KEY_AUTO_SAVE_SENTENCE, it).apply()
                    })
                    Spacer(Modifier.width(8.dp))
                    Text("Auto-save typed sentences", color = Color.White)
                }
                Spacer(Modifier.height(8.dp))
                Text("Minimum length: $minLen chars", color = Color.White)
                Slider(value = minLen.toFloat(), valueRange = 5f..60f, onValueChange = {
                    minLen = it.toInt(); prefs.edit().putInt(SettingsStore.KEY_AUTO_SAVE_MIN_LEN, minLen).apply()
                })
                Text(
                    "Sentences ending with . ? ! or ۔ are captured automatically and " +
                            "ranked by usage frequency.",
                    color = Color.LightGray
                )
            }
        }

        Text("Total saved: ${items.size}", color = Color.White)
        LazyColumn(Modifier.weight(1f)) {
            items(items) { s ->
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F)),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(s.sentence, color = Color.White)
                            Text("Used ×${s.useCount}", color = Color.LightGray)
                        }
                        IconButton(onClick = {
                            val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            // Mark this re-copy as intentional so the
                            // clipboard watcher doesn't add a duplicate
                            // history row (issue #6).
                            com.flexboard.utils.ClipboardWatcher.suppressNext(s.sentence)
                            cm.setPrimaryClip(ClipData.newPlainText("sentence", s.sentence))
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color(0xFFFF8C00))
                        }
                        IconButton(onClick = { scope.launch(Dispatchers.IO) { dao.delete(s) } }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF6A6A))
                        }
                    }
                }
            }
        }
    }
}
