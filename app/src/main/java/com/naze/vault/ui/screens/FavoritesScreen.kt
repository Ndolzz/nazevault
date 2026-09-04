package com.naze.vault.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.naze.vault.data.FileRepository
import com.naze.vault.data.IndexStore
import com.naze.vault.ui.components.NazeFileListItem
import com.naze.vault.ui.theme.NazeTextSecondary
import java.io.File

@Composable
fun FavoritesScreen(onOpenFile: (File) -> Unit) {
    var favorites by remember { mutableStateOf(IndexStore.getFavorites()) }
    val nodes = remember(favorites) {
        favorites.mapNotNull { path ->
            val f = File(path)
            if (f.exists()) FileRepository.toNode(f, favorites) else null
        }.sortedBy { it.name.lowercase() }
    }

    Column(Modifier.fillMaxSize()) {
        Text("Favorites", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(20.dp))
        if (nodes.isEmpty()) {
            Text("Belum ada favorite", color = NazeTextSecondary, modifier = Modifier.padding(horizontal = 20.dp))
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(nodes, key = { it.absolutePath }) { node ->
                NazeFileListItem(
                    node = node,
                    selected = false,
                    selectionModeActive = false,
                    onClick = {
                        val f = File(node.absolutePath)
                        if (f.isDirectory) { /* favorites screen is flat; opening a folder is out of scope here */ }
                        else onOpenFile(f)
                    },
                    onLongClick = {},
                    onToggleFavorite = { favorites = IndexStore.toggleFavorite(node.absolutePath) }
                )
            }
        }
    }
}
