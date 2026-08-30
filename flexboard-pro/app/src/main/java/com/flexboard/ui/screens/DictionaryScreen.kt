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
import com.flexboard.data.db.DictionaryWord
import com.flexboard.utils.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun DictionaryScreen() {
    val ctx = LocalContext.current
    val dao = remember { AppDatabase.get(ctx).dictionaryDao() }
    val words by dao.all().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var newWord by remember { mutableStateOf("") }
    val prefs = SettingsStore.prefs(ctx)
    var suggestions by remember { mutableStateOf(prefs.getBoolean(SettingsStore.KEY_SUGGESTIONS, true)) }
    var autoCorrect by remember { mutableStateOf(prefs.getBoolean(SettingsStore.KEY_AUTOCORRECT, true)) }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Dictionary", color = Color(0xFFFF8C00), fontWeight = FontWeight.Bold)

        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F))) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = suggestions, onCheckedChange = {
                        suggestions = it; prefs.edit().putBoolean(SettingsStore.KEY_SUGGESTIONS, it).apply()
                    })
                    Spacer(Modifier.width(8.dp)); Text("Word suggestions", color = Color.White)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = autoCorrect, onCheckedChange = {
                        autoCorrect = it; prefs.edit().putBoolean(SettingsStore.KEY_AUTOCORRECT, it).apply()
                    })
                    Spacer(Modifier.width(8.dp)); Text("Auto-correct", color = Color.White)
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F))) {
            Column(Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = newWord, onValueChange = { newWord = it }, label = { Text("Add word") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                Button(onClick = {
                    val w = newWord.trim()
                    if (w.isNotEmpty()) scope.launch(Dispatchers.IO) {
                        dao.insert(DictionaryWord(word = w.lowercase(), frequency = 10))
                    }
                    newWord = ""
                }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8C00), contentColor = Color.Black)) {
                    Text("Add")
                }
            }
        }

        Text("Saved words: ${words.size}", color = Color.White)
        LazyColumn(Modifier.weight(1f)) {
            items(words) { w ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(w.word, color = Color.White, modifier = Modifier.weight(1f))
                    Text("×${w.frequency}", color = Color.LightGray)
                    IconButton(onClick = { scope.launch(Dispatchers.IO) { dao.delete(w) } }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF6A6A))
                    }
                }
                HorizontalDivider(color = Color(0xFF2A2A2A))
            }
        }
    }
}
