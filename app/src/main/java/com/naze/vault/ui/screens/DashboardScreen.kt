package com.naze.vault.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Workspaces
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.naze.vault.data.FileRepository
import com.naze.vault.data.IndexStore
import com.naze.vault.data.model.VaultSection
import com.naze.vault.security.SecretsRepository
import com.naze.vault.ui.components.iconFor
import com.naze.vault.ui.theme.NazeBlue
import com.naze.vault.ui.theme.NazePurple
import com.naze.vault.ui.theme.NazeSurfaceElevated
import com.naze.vault.ui.theme.NazeTextSecondary
import com.naze.vault.util.FileUtils
import java.io.File
import java.util.Calendar

@Composable
fun DashboardScreen(
    onOpenRecent: (File) -> Unit,
    onQuickAction: (String) -> Unit
) {
    var filesCount by remember { mutableStateOf(0) }
    var foldersCount by remember { mutableStateOf(0) }
    var projectsCount by remember { mutableStateOf(0) }
    var secretsCount by remember { mutableStateOf(0) }

    remember {
        val (files, folders, _) = FileRepository.countAll(FileRepository.rootDir)
        filesCount = files
        foldersCount = folders
        projectsCount = FileRepository.sectionDir(VaultSection.PROJECTS).listFiles()?.count { it.isDirectory } ?: 0
        secretsCount = SecretsRepository.list().size
        true
    }

    val recents = remember { IndexStore.getRecents().take(8) }
    val greeting = remember { greetingForNow() }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(greeting, style = MaterialTheme.typography.bodyMedium, color = NazeTextSecondary)
                Text("Your Vault", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)

                Spacer(Modifier.height(16.dp))

                Surface(color = NazeSurfaceElevated, shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(20.dp)) {
                        StatRow("Files", filesCount.toString())
                        StatRow("Folders", foldersCount.toString())
                        StatRow("Projects", projectsCount.toString())
                        StatRow("Secrets", secretsCount.toString(), isLast = true)
                    }
                }

                Spacer(Modifier.height(24.dp))
                Text("Quick Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuickAction(Icons.Filled.InsertDriveFile, "File") { onQuickAction("NEW_FILE") }
                    QuickAction(Icons.Filled.CreateNewFolder, "Folder") { onQuickAction("NEW_FOLDER") }
                    QuickAction(Icons.Filled.Workspaces, "Project") { onQuickAction("NEW_PROJECT") }
                    QuickAction(Icons.Filled.Key, "Secret") { onQuickAction("NEW_SECRET") }
                    QuickAction(Icons.Filled.FileDownload, "Import") { onQuickAction("IMPORT") }
                }

                Spacer(Modifier.height(24.dp))
                Text("Recent", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
        }

        if (recents.isEmpty()) {
            item {
                Text(
                    "Belum ada aktivitas",
                    color = NazeTextSecondary,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        } else {
            items(recents) { entry ->
                val file = File(entry.path)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                ) {
                    Icon(iconFor(FileUtils.detectKind(file)), contentDescription = null, tint = NazeBlue)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(file.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                        Text(
                            "${entry.action} • ${FileUtils.formatDate(entry.timestamp)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NazeTextSecondary
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(100.dp)) }
    }
}

@Composable
private fun StatRow(label: String, value: String, isLast: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = NazeTextSecondary, style = MaterialTheme.typography.bodyLarge)
        Text(value, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun QuickAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
        Surface(
            color = NazePurple.copy(alpha = 0.15f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            androidx.compose.material3.IconButton(onClick = onClick) {
                Icon(icon, contentDescription = label, tint = NazePurple)
            }
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = NazeTextSecondary)
    }
}

private fun greetingForNow(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "Good morning"
        hour < 18 -> "Good afternoon"
        else -> "Good evening"
    }
}
