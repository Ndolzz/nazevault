package com.naze.vault.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.naze.vault.data.IndexStore
import com.naze.vault.ui.components.iconFor
import com.naze.vault.ui.theme.NazeBlue
import com.naze.vault.ui.theme.NazeTextSecondary
import com.naze.vault.util.FileUtils
import java.io.File

@Composable
fun RecentScreen(onOpenFile: (File) -> Unit) {
    val entries = remember { IndexStore.getRecents() }

    Column(Modifier.fillMaxSize()) {
        Text("Recent", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(20.dp))
        if (entries.isEmpty()) {
            Text("Belum ada aktivitas", color = NazeTextSecondary, modifier = Modifier.padding(horizontal = 20.dp))
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(entries, key = { it.path + it.timestamp }) { entry ->
                val file = File(entry.path)
                if (file.exists()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenFile(file) }
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Icon(iconFor(FileUtils.detectKind(file)), contentDescription = null, tint = NazeBlue)
                        Column(Modifier.padding(start = 12.dp)) {
                            Text(file.name, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "${entry.action} • ${FileUtils.formatDate(entry.timestamp)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = NazeTextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}
