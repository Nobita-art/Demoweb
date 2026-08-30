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
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.flexboard.data.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun ClipboardScreen() {
    val ctx = LocalContext.current
    val dao = remember { AppDatabase.get(ctx).clipboardDao() }
    val items by dao.all().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Clipboard Manager", color = Color(0xFFFF8C00), fontWeight = FontWeight.Bold)
        OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("Search clips") }, singleLine = true, modifier = Modifier.fillMaxWidth())

        val filtered = items.filter { query.isBlank() || it.text.contains(query, ignoreCase = true) }
        LazyColumn(Modifier.weight(1f)) {
            items(filtered) { c ->
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F)),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(c.text, color = Color.White, modifier = Modifier.weight(1f))
                        IconButton(onClick = { scope.launch(Dispatchers.IO) { dao.update(c.copy(pinned = !c.pinned)) } }) {
                            Icon(Icons.Default.PushPin, contentDescription = "Pin", tint = if (c.pinned) Color(0xFFFF8C00) else Color.LightGray)
                        }
                        IconButton(onClick = {
                            val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            // Mark this re-copy as intentional so the
                            // clipboard watcher doesn't add a duplicate
                            // history row (issue #6).
                            com.flexboard.utils.ClipboardWatcher.suppressNext(c.text)
                            cm.setPrimaryClip(ClipData.newPlainText("clip", c.text))
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color(0xFFFF8C00))
                        }
                        IconButton(onClick = { scope.launch(Dispatchers.IO) { dao.delete(c) } }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF6A6A))
                        }
                    }
                }
            }
        }
    }
}
