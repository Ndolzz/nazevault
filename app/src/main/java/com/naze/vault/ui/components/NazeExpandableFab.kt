package com.naze.vault.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Workspaces
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.naze.vault.ui.theme.NazeBlue
import com.naze.vault.ui.theme.NazeSurfaceElevated

enum class NazeFabAction { NEW_FILE, NEW_FOLDER, NEW_PROJECT, NEW_SECRET, IMPORT_FILE }

@Composable
fun NazeExpandableFab(
    onAction: (NazeFabAction) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(horizontalAlignment = Alignment.End, modifier = modifier) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
        ) {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FabMenuItem("New File", Icons.Filled.InsertDriveFile) {
                    expanded = false; onAction(NazeFabAction.NEW_FILE)
                }
                FabMenuItem("New Folder", Icons.Filled.CreateNewFolder) {
                    expanded = false; onAction(NazeFabAction.NEW_FOLDER)
                }
                FabMenuItem("New Project", Icons.Filled.Workspaces) {
                    expanded = false; onAction(NazeFabAction.NEW_PROJECT)
                }
                FabMenuItem("New Secret", Icons.Filled.Key) {
                    expanded = false; onAction(NazeFabAction.NEW_SECRET)
                }
                FabMenuItem("Import File", Icons.Filled.FileDownload) {
                    expanded = false; onAction(NazeFabAction.IMPORT_FILE)
                }
                androidx.compose.foundation.layout.Spacer(Modifier.padding(2.dp))
            }
        }

        FloatingActionButton(
            onClick = { expanded = !expanded },
            containerColor = NazeBlue
        ) {
            Icon(
                imageVector = if (expanded) Icons.Filled.Close else Icons.Filled.Add,
                contentDescription = if (expanded) "Tutup" else "Tambah"
            )
        }
    }
}

@Composable
private fun FabMenuItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(
            color = NazeSurfaceElevated,
            shape = MaterialTheme.shapes.medium
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }
        FloatingActionButton(
            onClick = onClick,
            containerColor = NazeSurfaceElevated
        ) {
            Icon(icon, contentDescription = label)
        }
    }
}
