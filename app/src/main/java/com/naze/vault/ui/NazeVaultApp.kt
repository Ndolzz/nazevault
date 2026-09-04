package com.naze.vault.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Workspaces
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
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
import java.io.File

private enum class NazeTab(val label: String) {
    DASHBOARD("Home"), VAULT("Vault"), PROJECTS("Projects"), SECRETS("Secrets"),
    FAVORITES("Favorites"), RECENT("Recent"), SETTINGS("Settings")
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

@Composable
private fun MainContent(onRequestLock: () -> Unit) {
    var currentTab by remember { mutableStateOf(NazeTab.DASHBOARD) }
    var viewerFile by remember { mutableStateOf<File?>(null) }
    var secretsAddSignal by remember { mutableStateOf(0) }

    if (viewerFile != null) {
        FileViewerScreen(file = viewerFile!!, onBack = { viewerFile = null })
        return
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentTab == NazeTab.DASHBOARD,
                    onClick = { currentTab = NazeTab.DASHBOARD },
                    icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                    label = { Text("Home") },
                    alwaysShowLabel = false
                )
                NavigationBarItem(
                    selected = currentTab == NazeTab.VAULT,
                    onClick = { currentTab = NazeTab.VAULT },
                    icon = { Icon(Icons.Filled.Folder, contentDescription = null) },
                    label = { Text("Vault") },
                    alwaysShowLabel = false
                )
                NavigationBarItem(
                    selected = currentTab == NazeTab.PROJECTS,
                    onClick = { currentTab = NazeTab.PROJECTS },
                    icon = { Icon(Icons.Filled.Workspaces, contentDescription = null) },
                    label = { Text("Projects") },
                    alwaysShowLabel = false
                )
                NavigationBarItem(
                    selected = currentTab == NazeTab.SECRETS,
                    onClick = { currentTab = NazeTab.SECRETS },
                    icon = { Icon(Icons.Filled.Key, contentDescription = null) },
                    label = { Text("Secrets") },
                    alwaysShowLabel = false
                )
                NavigationBarItem(
                    selected = currentTab == NazeTab.FAVORITES,
                    onClick = { currentTab = NazeTab.FAVORITES },
                    icon = { Icon(Icons.Filled.Star, contentDescription = null) },
                    label = { Text("Favorites") },
                    alwaysShowLabel = false
                )
                NavigationBarItem(
                    selected = currentTab == NazeTab.RECENT,
                    onClick = { currentTab = NazeTab.RECENT },
                    icon = { Icon(Icons.Filled.History, contentDescription = null) },
                    label = { Text("Recent") },
                    alwaysShowLabel = false
                )
                NavigationBarItem(
                    selected = currentTab == NazeTab.SETTINGS,
                    onClick = { currentTab = NazeTab.SETTINGS },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    label = { Text("Settings") },
                    alwaysShowLabel = false
                )
            }
        }
    ) { padding ->
        androidx.compose.foundation.layout.Box(modifier = Modifier.padding(padding)) {
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
