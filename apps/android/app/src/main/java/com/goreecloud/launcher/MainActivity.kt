package com.goreecloud.launcher

import android.app.role.RoleManager
import android.content.Intent
import android.content.Context
import android.provider.Settings
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.goreecloud.ui.material3.GoreeAccent
import com.goreecloud.ui.material3.GoreeDestination
import com.goreecloud.ui.material3.GoreeFloatingNavigationBar
import com.goreecloud.ui.material3.GoreeCloudExpressiveTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { GoreeCloudExpressiveTheme(accent = GoreeAccent.Iris) { LauncherScreen() } }
    }
}

@Composable
private fun LauncherScreen() {
    val context = LocalContext.current
    val catalog = remember(context) { LauncherCatalog(context) }
    val preferences = remember(context) {
        context.getSharedPreferences("launcher_private", Context.MODE_PRIVATE)
    }
    var installed by remember { mutableStateOf(catalog.discover()) }
    var pinned by remember { mutableStateOf(preferences.getStringSet("pins", emptySet())!!.toSet()) }
    var destination by rememberSaveable { mutableStateOf("home") }
    var query by rememberSaveable { mutableStateOf("") }
    var notice by remember { mutableStateOf("") }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) installed = catalog.discover()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    fun toggle(item: LaunchItem) {
        val next = if (item.id in pinned) pinned - item.id else pinned + item.id
        if (preferences.edit().putStringSet("pins", next).commit()) pinned = next
        else notice = "Unable to save favorites"
    }
    fun open(item: LaunchItem) {
        if (!catalog.open(item)) notice = "Unable to launch ${item.title}"
    }

    val pinnedApps = installed.filter { it.id in pinned }
    Scaffold(
        bottomBar = {
            GoreeFloatingNavigationBar(
                destinations = listOf(
                    GoreeDestination("home", "Home", { Text("⌂") }),
                    GoreeDestination("apps", "All apps", { Text("▦") }),
                    GoreeDestination("settings", "Settings", { Text("⚙") })
                ),
                selectedId = destination,
                onDestinationSelected = { destination = it }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { insets ->
        Column(
            modifier = Modifier.fillMaxSize().padding(insets).padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("GoreeCloud", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("Launcher · Local preview", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                FilledTonalButton(onClick = { installed = catalog.discover() }) { Text("Refresh") }
            }
            when (destination) {
                "home" -> {
                    Text("Your space", style = MaterialTheme.typography.headlineSmall)
                    Text("Pin applications from All apps to add them here.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (pinnedApps.isEmpty()) {
                        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Text("Make it yours", style = MaterialTheme.typography.titleLarge)
                                Text("Your home screen is ready. Choose apps to keep nearby.")
                                Button(onClick = { destination = "apps" }) { Text("Browse all apps") }
                            }
                        }
                    } else {
                        Text("Favorites", style = MaterialTheme.typography.titleMedium)
                        LazyVerticalGrid(columns = GridCells.Adaptive(90.dp),
                            contentPadding = PaddingValues(bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)) {
                            items(pinnedApps, key = { it.id }) { item ->
                                AppTile(item = item, onClick = { open(item) })
                            }
                        }
                    }
                    if (pinnedApps.isNotEmpty()) {
                        Text("Dock · ${pinnedApps.take(5).size} quick launches",
                            style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            pinnedApps.take(5).forEach { item ->
                                OutlinedButton(onClick = { open(item) }, modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 3.dp)) {
                                    Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
                "apps" -> {
                    OutlinedTextField(
                        value = query, onValueChange = { query = it },
                        label = { Text("Search installed apps") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    val shown = installed.filter { matchesApp(query, it.title, it.packageName) }
                    Text("${shown.size} apps", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(104.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(shown, key = { it.id }) { item ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                AppTile(item, onClick = { open(item) })
                                TextButton(onClick = { toggle(item) }) {
                                    Text(if (item.id in pinned) "Unpin" else "Pin")
                                }
                            }
                        }
                    }
                }
                else -> {
                    Text("Launcher settings", style = MaterialTheme.typography.headlineSmall)
                    Text("To make GoreeCloud Launcher your Home app, select it in Android settings.")
                    Button(onClick = {
                        val role = context.getSystemService(RoleManager::class.java)
                        try {
                            if (role.isRoleAvailable(RoleManager.ROLE_HOME) &&
                                !role.isRoleHeld(RoleManager.ROLE_HOME)) {
                                context.startActivity(role.createRequestRoleIntent(RoleManager.ROLE_HOME))
                            } else {
                                context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
                            }
                        } catch (_: RuntimeException) {
                            notice = "Open Android Settings → Apps → Default apps → Home app"
                        }
                    }) { Text("Choose default Home app") }
                    Text("Privacy: no network, location, contacts, notification or usage access requested.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Development preview. Keep another launcher installed.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (notice.isNotBlank()) {
                Text(notice, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun AppTile(item: LaunchItem, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 14.dp)
        ) {
            AndroidView(factory = { ImageView(it) }, update = {
                it.setImageDrawable(item.icon)
                it.contentDescription = null
            }, modifier = Modifier.size(44.dp))
            Text(item.title, maxLines = 2, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelMedium)
        }
    }
}
