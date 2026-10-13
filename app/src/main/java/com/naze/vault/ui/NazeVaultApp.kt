package com.naze.vault.ui

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Menu
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Workspaces
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.naze.vault.data.model.VaultSection
import com.naze.vault.security.VaultLockManager
import com.naze.vault.ui.screens.DashboardScreen
import com.naze.vault.ui.screens.FavoritesScreen
import com.naze.vault.ui.screens.FileBrowserScreen
import com.naze.vault.ui.screens.FileViewerScreen
import com.naze.vault.ui.screens.LockScreen
import com.naze.vault.ui.screens.RecentScreen
import com.naze.vault.ui.screens.SecretsScreen
import com.naze.vault.ui.screens.SettingsScreen
import com.naze.vault.ui.theme.NazeVaultTheme
import kotlinx.coroutines.launch
import java.io.File

private enum class NazeTab(val label: String, val icon: ImageVector) {
    DASHBOARD("Home", Icons.Filled.Home),
    VAULT("Vault", Icons.Filled.Folder),
    PROJECTS("Projects", Icons.Filled.Workspaces),
    SECRETS("Secrets", Icons.Filled.Key),
    FAVORITES("Favorites", Icons.Filled.Star),
    RECENT("Recent", Icons.Filled.History),
    SETTINGS("Settings", Icons.Filled.Settings)
}

@Composable
fun NazeVaultApp() {
    NazeVaultTheme {
        var isLocked by remember { mutableStateOf(!VaultLockManager.isUnlockedThisSession) }

        // Re-lock automatically after the configured auto-lock window once the
        // app returns to the foreground.
        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_STOP -> VaultLockManager.onAppBackgrounded()
                    Lifecycle.Event.ON_START -> {
                        VaultLockManager.onAppForegrounded()
                        if (!VaultLockManager.isUnlockedThisSession) isLocked = true
                    }
                    else -> {}
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        if (isLocked) {
            LockScreen(onUnlocked = { isLocked = false })
        } else {
            MainContent(onRequestLock = { isLocked = true })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainContent(onRequestLock: () -> Unit) {
    var currentTab by remember { mutableStateOf(NazeTab.DASHBOARD) }
    var viewerFile by remember { mutableStateOf<File?>(null) }
    var secretsAddSignal by remember { mutableStateOf(0) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    if (viewerFile != null) {
        FileViewerScreen(file = viewerFile!!, onBack = { viewerFile = null })
        return
    }

    // The hamburger drawer blurs the content behind it while open.
    // Modifier.blur relies on RenderEffect, available from Android 12 (API 31);
    // on Android 11 the drawer scrim alone provides the depth cue.
    val drawerOpening = drawerState.targetValue == DrawerValue.Open

    BackHandler(enabled = drawerState.isOpen) { scope.launch { drawerState.close() } }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(300.dp)) {
                Column(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 16.dp)
                    ) {
                        Text("Naze Vault", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            "Personal secure vault",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    HorizontalDivider()
                    NazeTab.entries.forEach { tab ->
                        NavigationDrawerItem(
                            label = { Text(tab.label) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            selected = currentTab == tab,
                            onClick = {
                                currentTab = tab
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(currentTab.label) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.AutoMirrored.Filled.Menu, contentDescription = "Menu navigasi")
                        }
                    }
                )
            },
            modifier = if (drawerOpening && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Modifier.blur(18.dp)
            } else {
                Modifier
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                when (currentTab) {
                    NazeTab.DASHBOARD -> DashboardScreen(
                        onOpenRecent = { viewerFile = it },
                        onQuickAction = { action ->
                            when (action) {
                                "NEW_SECRET" -> { secretsAddSignal++; currentTab = NazeTab.SECRETS }
                                else -> currentTab = NazeTab.VAULT
                            }
                        }
                    )
                    NazeTab.VAULT -> FileBrowserScreen(
                        section = VaultSection.VAULT,
                        rootLabel = "Vault",
                        onOpenFile = { viewerFile = it },
                        onNavigateToSecretsAndAdd = { secretsAddSignal++; currentTab = NazeTab.SECRETS }
                    )
                    NazeTab.PROJECTS -> FileBrowserScreen(
                        section = VaultSection.PROJECTS,
                        rootLabel = "Projects",
                        onOpenFile = { viewerFile = it },
                        onNavigateToSecretsAndAdd = { secretsAddSignal++; currentTab = NazeTab.SECRETS }
                    )
                    NazeTab.SECRETS -> SecretsScreen(triggerAddOnEnter = secretsAddSignal > 0)
                    NazeTab.FAVORITES -> FavoritesScreen(onOpenFile = { viewerFile = it })
                    NazeTab.RECENT -> RecentScreen(onOpenFile = { viewerFile = it })
                    NazeTab.SETTINGS -> SettingsScreen(onLockNow = {
                        VaultLockManager.lockNow()
                        onRequestLock()
                    })
                }
            }
        }
    }
}
