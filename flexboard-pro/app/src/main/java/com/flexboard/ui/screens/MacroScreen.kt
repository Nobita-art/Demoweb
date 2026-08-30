package com.flexboard.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
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
import com.flexboard.data.db.Macro
import com.flexboard.ime.MacroEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun MacroScreen() {
    val ctx = LocalContext.current
    val dao = remember { AppDatabase.get(ctx).macroDao() }
    val macros by dao.all().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var shortcode by remember { mutableStateOf("") }
    var expansion by remember { mutableStateOf("") }
    var caseSensitive by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Macros / Text Expansion", color = Color(0xFFFF8C00), fontWeight = FontWeight.Bold)

        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F))) {
            Column(Modifier.padding(16.dp)) {
                OutlinedTextField(value = shortcode, onValueChange = { shortcode = it }, label = { Text("Shortcode (e.g. addr1)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(value = expansion, onValueChange = { expansion = it }, label = { Text("Expansion text") }, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = caseSensitive, onCheckedChange = { caseSensitive = it })
                    Spacer(Modifier.width(8.dp)); Text("Case-sensitive", color = Color.White)
                }
                Spacer(Modifier.height(6.dp))
                Button(onClick = {
                    if (shortcode.isNotBlank() && expansion.isNotBlank()) {
                        scope.launch(Dispatchers.IO) {
                            dao.upsert(Macro(shortcode = shortcode.trim(), expansion = expansion, caseSensitive = caseSensitive))
                            MacroEngine.reload(ctx)
                        }
                        shortcode = ""; expansion = ""
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8C00), contentColor = Color.Black)) {
                    Text("Save Macro")
                }
            }
        }

        LazyColumn(Modifier.weight(1f)) {
            items(macros) { m ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(m.shortcode, color = Color(0xFFFF8C00), fontWeight = FontWeight.Bold)
                        Text(m.expansion, color = Color.White)
                    }
                    IconButton(onClick = { scope.launch(Dispatchers.IO) { dao.delete(m); MacroEngine.reload(ctx) } }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF6A6A))
                    }
                }
                HorizontalDivider(color = Color(0xFF2A2A2A))
            }
        }
    }
}
