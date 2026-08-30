package com.flexboard.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.flexboard.ime.AutoTypeEngine
import com.flexboard.ime.AutoTypeForegroundService
import com.flexboard.ui.screens.*
import com.flexboard.ui.theme.FlexboardTheme
import com.flexboard.security.Obf
import com.flexboard.security.ObfConstants
import com.flexboard.utils.SettingsStore
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val pickFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            try { contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
            val name = queryDisplayName(it)
            AutoTypeEngine.loadFromUri(this, it, name)
        }
    }

    /**
     * POST_NOTIFICATIONS permission prompt for Android 13+ (Tiramisu).
     * The auto-typer's foreground service relies on a persistent notification
     * to stay alive in the background — without this permission Android 13+
     * would silently suppress that notification and the OS could kill the
     * service mid-run.
     */
    private val requestNotifPerm = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* result ignored — user can re-grant from Android settings */ }

    private val pickBg = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            try { contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
            SettingsStore.prefs(this).edit()
                .putString(SettingsStore.KEY_BG_IMAGE_URI, it.toString())
                .apply()
        }
    }

    private fun queryDisplayName(uri: android.net.Uri): String? {
        val proj = arrayOf(android.provider.OpenableColumns.DISPLAY_NAME)
        return contentResolver.query(uri, proj, null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Ask for POST_NOTIFICATIONS on Android 13+ (Tiramisu). Wrapped in
        // a try/catch so a buggy permission registry never blocks the
        // app from launching — worst case the user just sees no popup.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    requestNotifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            } catch (_: Throwable) { /* best-effort */ }
        }
        val initialSection = try { intent?.getStringExtra("section") } catch (_: Throwable) { null }
        try {
            setContent {
                FlexboardTheme {
                    // v1.11 — every screen now sits behind the AccessGate
                    // (security check + GitHub approval/plan + local trial).
                    GatedApp {
                        AppRoot(
                            initialSection = initialSection,
                            onPickTxt = { safeLaunch { pickFile.launch(arrayOf("text/plain", "*/*")) } },
                            onPickBg = { safeLaunch { pickBg.launch(arrayOf("image/*")) } },
                            onOpenImeSettings = { safeLaunch { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) } },
                            onOpenAccessibility = { safeLaunch { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) } },
                            onStartAutoType = {
                                safeLaunch {
                                    AutoTypeForegroundService.start(this)
                                    AutoTypeEngine.start(
                                        this,
                                        SettingsStore.prefs(this)
                                            .getInt(SettingsStore.KEY_AT_START_LINE, 0)
                                    )
                                    lastStartAtMs = System.currentTimeMillis()
                                    try { moveTaskToBack(true) } catch (_: Throwable) {}
                                }
                            },
                            onResumeAutoType = {
                                safeLaunch {
                                    AutoTypeEngine.resume()
                                    lastStartAtMs = System.currentTimeMillis()
                                    try { moveTaskToBack(true) } catch (_: Throwable) {}
                                }
                            },
                            onPause = { safeLaunch { AutoTypeEngine.pause() } },
                            onStop = {
                                safeLaunch {
                                    AutoTypeEngine.stop()
                                    AutoTypeForegroundService.stop(this)
                                }
                            }
                        )
                    } // GatedApp
                }
            }
        } catch (t: Throwable) {
            android.util.Log.e("FlexBoardMain", "setContent failed", t)
            val tv = android.widget.TextView(this).apply {
                text = "FlexBoard Pro\n\nUI failed to initialize:\n${t.javaClass.simpleName}: ${t.message}\n\nPlease share crash log."
                setPadding(40, 80, 40, 40)
                setTextColor(android.graphics.Color.WHITE)
                setBackgroundColor(android.graphics.Color.parseColor("#0D0D0D"))
            }
            setContentView(tv)
        }
    }

    private inline fun safeLaunch(block: () -> Unit) {
        try { block() } catch (t: Throwable) {
            android.util.Log.e("FlexBoardMain", "action failed", t)
        }
    }

    @Volatile private var lastStartAtMs = 0L

    private fun stopTyperIfRunning() {
        try {
            val s = AutoTypeEngine.state.value
            if (s.running) {
                AutoTypeEngine.stop()
                try { AutoTypeForegroundService.stop(this) } catch (_: Throwable) {}
            }
        } catch (_: Throwable) {}
    }

    override fun onStop() {
        super.onStop()
        val withinStartWindow = System.currentTimeMillis() - lastStartAtMs < 2000L
        if (withinStartWindow) return
        stopTyperIfRunning()
    }

    override fun onDestroy() {
        if (isFinishing) {
            stopTyperIfRunning()
        }
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        stopTyperIfRunning()
    }
}

private val ORANGE = Color(0xFFFF8C00)
private val DARK_BG = Color(0xFF0D0D0D)
private val DRAWER_BG = Color(0xFF141414)

private data class TabEntry(val id: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val TABS = listOf(
    TabEntry("home",       "Home",       Icons.Default.Home),
    TabEntry("autotype",   "Auto-Type",  Icons.Default.PlayArrow),
    TabEntry("theme",      "Theme",      Icons.Default.Palette),
    TabEntry("dictionary", "Dictionary", Icons.Default.Book),
    TabEntry("macros",     "Macros",     Icons.Default.Bolt),
    TabEntry("sentences",  "Sentences",  Icons.Default.Notes),
    TabEntry("clipboard",  "Clipboard",  Icons.Default.ContentPaste),
    TabEntry("plans",      "Plans",      Icons.Default.Star),
    TabEntry("about",      "About",      Icons.Default.Info),
)

/**
 * v1.11 — top app bar layout flipped per user request:
 *   • Hamburger (≡) on the LEFT
 *   • "FlexBoard Pro" title on the RIGHT
 *
 * The previous DropdownMenu (small floating popup) felt cramped — the
 * user said the list "side by pora hona chahiye" so we now use a
 * proper Material 3 [ModalNavigationDrawer] that slides in from the
 * left edge. Each tab is a full-width row with icon + label.
 *
 * The first launch after the gate passes also fires the WhatsApp
 * community popup (shows on every launch — no opt-out, per user request).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(
    initialSection: String?,
    onPickTxt: () -> Unit,
    onPickBg: () -> Unit,
    onOpenImeSettings: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onStartAutoType: () -> Unit,
    onPause: () -> Unit,
    onResumeAutoType: () -> Unit,
    onStop: () -> Unit
) {
    val initialIdx = TABS.indexOfFirst { it.id == initialSection }.let { if (it < 0) 0 else it }
    var selected by rememberSaveable { mutableStateOf(initialIdx) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val ctx = LocalContext.current
    var showWaPopup by remember { mutableStateOf(true) }

    if (showWaPopup) {
        WhatsAppCommunityDialog(onDismiss = { showWaPopup = false })
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            DrawerContent(
                selected = selected,
                onSelect = {
                    selected = it
                    scope.launch { drawerState.close() }
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    // Hamburger on the LEFT (navigationIcon slot)
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = "Menu",
                                tint = ORANGE
                            )
                        }
                    },
                    // Empty title — branding moves to the actions slot on the RIGHT
                    title = { },
                    actions = {
                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Text(
                                "FlexBoard Pro",
                                color = ORANGE,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            if (selected != 0) {
                                Text(
                                    TABS[selected].label,
                                    color = Color.LightGray,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = DARK_BG,
                        titleContentColor = ORANGE
                    )
                )
            }
        ) { padding ->
            Box(
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(DARK_BG)
            ) {
                when (TABS[selected].id) {
                    "home"       -> HomeScreen(onOpenImeSettings, onOpenAccessibility)
                    "autotype"   -> AutoTypeScreen(onPickTxt, onStartAutoType, onPause, onResumeAutoType, onStop)
                    "theme"      -> ThemeScreen(onPickBackground = onPickBg)
                    "dictionary" -> DictionaryScreen()
                    "macros"     -> MacroScreen()
                    "sentences"  -> SavedSentencesScreen()
                    "clipboard"  -> ClipboardScreen()
                    "plans"      -> PlansScreenAccessAware()
                    "about"      -> AboutScreen()
                }
            }
        }
    }
}

/**
 * Drawer body — a vertical list of full-width navigation rows. Each row
 * has the icon left-aligned and the label next to it (the "side by side
 * pora hona chahiye" the user asked for). Active row is highlighted with
 * the orange accent + background tint.
 */
@Composable
private fun DrawerContent(selected: Int, onSelect: (Int) -> Unit) {
    ModalDrawerSheet(
        drawerContainerColor = DRAWER_BG,
        modifier = Modifier.width(280.dp)
    ) {
        // Drawer header with brand
        Box(
            Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(listOf(Color(0xFF1F1300), Color(0xFF2A1505)))
                )
                .padding(20.dp)
        ) {
            Column {
                Text(
                    "FlexBoard Pro",
                    color = ORANGE,
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp
                )
                Text("Menu", color = Color.LightGray, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
        TABS.forEachIndexed { i, tab ->
            val active = i == selected
            NavigationDrawerItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                label = {
                    Text(
                        tab.label,
                        color = if (active) ORANGE else Color.White,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 15.sp
                    )
                },
                icon = {
                    Icon(
                        tab.icon,
                        contentDescription = tab.label,
                        tint = if (active) ORANGE else Color.LightGray
                    )
                },
                selected = active,
                onClick = { onSelect(i) },
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = ORANGE.copy(alpha = 0.15f),
                    unselectedContainerColor = Color.Transparent
                )
            )
        }
    }
}

/**
 * Wrapper that gives PlansScreen the current AccessState. Because
 * MainActivity sits behind GatedApp, the user landing here from the
 * drawer is always Approved/TrialActive — but we still re-evaluate
 * cheaply so the live countdown reflects whatever the latest cache says.
 */
@Composable
private fun PlansScreenAccessAware() {
    val ctx = LocalContext.current
    val state = remember { com.flexboard.security.AccessGate.cachedState(ctx) }
    PlansScreen(
        state = state,
        onRecheck = {
            try { com.flexboard.security.ApprovalGate.evaluate(ctx, force = true) } catch (_: Throwable) {}
        }
    )
}

/**
 * v1.14 — WhatsApp Community popup. Shows on every MainActivity launch.
 * A "Don't show again" checkbox lets the user permanently dismiss it.
 */
@Composable
private fun WhatsAppCommunityDialog(onDismiss: () -> Unit) {
    val ctx          = LocalContext.current
    val communityUrl = remember {
        Obf.decode(ctx, ObfConstants.WA_COMMUNITY_URL)
            .ifBlank { "https://whatsapp.com" }
    }
    val WA_GREEN     = Color(0xFF25D366)
    val WA_DARK      = Color(0xFF128C7E)

    Dialog(
        onDismissRequest = { onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1A1A1A))
                .border(1.dp, WA_GREEN.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
        ) {
            Column {
                // WhatsApp green header
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(listOf(WA_DARK, WA_GREEN)))
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier.size(64.dp).clip(CircleShape).background(Color.White),
                            contentAlignment = Alignment.Center
                        ) { Text("💬", fontSize = 30.sp) }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Join our WhatsApp Community!",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp
                        )
                    }
                }

                // Body
                Column(Modifier.padding(20.dp)) {
                    Text(
                        "Join the FlexBoard Pro WhatsApp community to get free activation keys, " +
                                "latest updates, tips, and exclusive announcements directly from the developer!",
                        color = Color(0xFFE0E0E0),
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = {
                            try {
                                ctx.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(communityUrl))
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            } catch (_: Throwable) {}
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WA_GREEN, contentColor = Color.Black),
                        shape  = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Text("Join WhatsApp Community", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { onDismiss() },
                        shape   = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) { Text("Later", color = Color.LightGray) }
                }
            }
        }
    }
}
