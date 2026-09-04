package com.naze.vault.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.naze.vault.data.model.SecretEntry
import com.naze.vault.security.SecretsRepository
import com.naze.vault.ui.theme.NazeBlue
import com.naze.vault.ui.theme.NazePurple
import com.naze.vault.ui.theme.NazeSurfaceElevated
import com.naze.vault.ui.theme.NazeTextSecondary

private val CATEGORIES = listOf("API Key", "API Token", "Password", "Environment Variable", "Credential", "Other")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecretsScreen(triggerAddOnEnter: Boolean = false) {
    var entries by remember { mutableStateOf(SecretsRepository.list()) }
    var showAddDialog by remember { mutableStateOf(triggerAddOnEnter) }
    var editTarget by remember { mutableStateOf<SecretEntry?>(null) }
    var deleteTarget by remember { mutableStateOf<SecretEntry?>(null) }
    val revealed = remember { mutableStateOf(setOf<String>()) }
    val clipboard = LocalClipboardManager.current

    fun refresh() { entries = SecretsRepository.list() }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }, containerColor = NazeBlue) {
                Icon(Icons.Filled.Key, contentDescription = "Secret baru")
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Text(
                "Secrets",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(20.dp)
            )
            if (entries.isEmpty()) {
                Text(
                    "Belum ada secret tersimpan",
                    color = NazeTextSecondary,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(entries, key = { it.id }) { entry ->
                    val isRevealed = revealed.value.contains(entry.id)
                    Surface(
                        color = NazeSurfaceElevated,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Key, contentDescription = null, tint = NazePurple)
                                Column(Modifier.padding(start = 10.dp).weight(1f)) {
                                    Text(entry.title, style = MaterialTheme.typography.titleMedium)
                                    Text(entry.category, style = MaterialTheme.typography.bodyMedium, color = NazeTextSecondary)
                                }
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                            ) {
                                Text(
                                    text = if (isRevealed) runCatching { SecretsRepository.reveal(entry) }.getOrDefault("(error)") else "••••••••••••••",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = {
                                    revealed.value = if (isRevealed) revealed.value - entry.id else revealed.value + entry.id
                                }) {
                                    Icon(if (isRevealed) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, contentDescription = "Show/Hide")
                                }
                                IconButton(onClick = {
                                    val plain = runCatching { SecretsRepository.reveal(entry) }.getOrDefault("")
                                    clipboard.setText(AnnotatedString(plain))
                                }) { Icon(Icons.Filled.ContentCopy, contentDescription = "Copy") }
                                IconButton(onClick = { editTarget = entry }) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Edit")
                                }
                                IconButton(onClick = { deleteTarget = entry }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete")
                                }
                            }
                        }
                    }
                }
                item { androidx.compose.foundation.layout.Spacer(Modifier.padding(60.dp)) }
            }
        }
    }

    if (showAddDialog) {
        SecretEditDialog(
            title = "Secret Baru",
            initialTitle = "",
            initialCategory = CATEGORIES.first(),
            initialValue = "",
            onDismiss = { showAddDialog = false },
            onConfirm = { t, c, v ->
                SecretsRepository.add(t, c, v)
                showAddDialog = false
                refresh()
            }
        )
    }

    editTarget?.let { target ->
        SecretEditDialog(
            title = "Edit Secret",
            initialTitle = target.title,
            initialCategory = target.category,
            initialValue = "",
            valueHint = "Kosongkan jika tidak ingin mengubah nilai",
            onDismiss = { editTarget = null },
            onConfirm = { t, c, v ->
                SecretsRepository.update(target.id, t, c, v.ifBlank { null })
                editTarget = null
                refresh()
            }
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Hapus '${target.title}'?") },
            confirmButton = {
                TextButton(onClick = {
                    SecretsRepository.delete(target.id)
                    deleteTarget = null
                    refresh()
                }) { Text("Hapus", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Batal") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SecretEditDialog(
    title: String,
    initialTitle: String,
    initialCategory: String,
    initialValue: String,
    valueHint: String = "Nilai secret",
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(initialTitle) }
    var category by remember { mutableStateOf(initialCategory) }
    var value by remember { mutableStateOf(initialValue) }
    var categoryMenuOpen by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama") }, singleLine = true)
                androidx.compose.foundation.layout.Spacer(Modifier.padding(4.dp))

                Box {
                    androidx.compose.material3.OutlinedButton(
                        onClick = { categoryMenuOpen = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Kategori: $category", modifier = Modifier.weight(1f))
                    }
                    DropdownMenu(expanded = categoryMenuOpen, onDismissRequest = { categoryMenuOpen = false }) {
                        CATEGORIES.forEach { cat ->
                            DropdownMenuItem(text = { Text(cat) }, onClick = { category = cat; categoryMenuOpen = false })
                        }
                    }
                }

                androidx.compose.foundation.layout.Spacer(Modifier.padding(4.dp))
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text(valueHint) },
                    singleLine = false
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onConfirm(name, category, value) }) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}
