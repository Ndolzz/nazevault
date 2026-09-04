package com.naze.vault.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.naze.vault.data.FileRepository
import com.naze.vault.data.IndexStore
import com.naze.vault.data.ProjectBuilder
import com.naze.vault.data.model.FileSortMode
import com.naze.vault.data.model.ProjectTemplates
import com.naze.vault.data.model.VaultFileKind
import com.naze.vault.data.model.VaultNode
import com.naze.vault.data.model.VaultSection
import com.naze.vault.ui.components.NazeBreadcrumb
import com.naze.vault.ui.components.NazeExpandableFab
import com.naze.vault.ui.components.NazeFabAction
import com.naze.vault.ui.components.NazeFileListItem
import com.naze.vault.util.FileUtils
import com.naze.vault.util.ZipUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileBrowserScreen(
    section: VaultSection,
    rootLabel: String,
    onOpenFile: (File) -> Unit,
    onNavigateToSecretsAndAdd: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sectionRoot = remember { FileRepository.sectionDir(section) }

    val pathStack = remember { mutableStateListOf(sectionRoot) }
    val currentDir = pathStack.last()

    var favorites by remember { mutableStateOf(IndexStore.getFavorites()) }
    var sortMode by remember { mutableStateOf(FileSortMode.NAME_ASC) }
    var sortMenuOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchActive by remember { mutableStateOf(false) }
    var refreshTick by remember { mutableStateOf(0) }

    val selection = remember { mutableStateListOf<String>() }
    var clipboard by remember { mutableStateOf<Pair<File, Boolean>?>(null) } // file, isCut

    var showNewFolderDialog by remember { mutableStateOf(false) }
    var showNewFileDialog by remember { mutableStateOf(false) }
    var showNewProjectDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<File?>(null) }
    var deleteTargets by remember { mutableStateOf<List<File>?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        scope.launch {
            withContext(Dispatchers.IO) {
                uris.forEach { uri -> FileRepository.importFromUri(context, uri, currentDir) }
            }
            refreshTick++
        }
    }

    val nodes = remember(currentDir, sortMode, favorites, refreshTick, searchQuery, searchActive) {
        if (searchActive && searchQuery.isNotBlank()) {
            FileRepository.sortNodes(
                FileRepository.search(currentDir, searchQuery).map { it.copy(isFavorite = favorites.contains(it.absolutePath)) },
                sortMode
            )
        } else {
            FileRepository.list(currentDir, sortMode, favorites)
        }
    }

    fun refresh() { refreshTick++ }

    fun toggleFavorite(node: VaultNode) {
        favorites = IndexStore.toggleFavorite(node.absolutePath)
    }

    fun openNode(node: VaultNode) {
        val file = File(node.absolutePath)
        if (file.isDirectory) {
            pathStack.add(file)
        } else {
            IndexStore.recordActivity(file.absolutePath, "opened")
            onOpenFile(file)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selection.isNotEmpty()) {
                        IconButton(onClick = { selection.clear() }) {
                            Icon(Icons.Filled.Close, contentDescription = "Batal pilih")
                        }
                        Text("${selection.size} dipilih", modifier = Modifier.weight(1f))
                        IconButton(onClick = {
                            val target = selection.mapNotNull { p -> nodes.find { it.absolutePath == p } }.firstOrNull()
                            if (selection.size == 1 && target != null) clipboard = File(target.absolutePath) to false
                        }) { Icon(Icons.Filled.ContentCopy, contentDescription = "Copy") }
                        IconButton(onClick = {
                            val target = selection.mapNotNull { p -> nodes.find { it.absolutePath == p } }.firstOrNull()
                            if (selection.size == 1 && target != null) clipboard = File(target.absolutePath) to true
                        }) { Icon(Icons.Filled.ContentCut, contentDescription = "Cut") }
                        IconButton(onClick = {
                            shareFiles(context, selection.map { File(it) })
                        }) { Icon(Icons.Filled.Share, contentDescription = "Share") }
                        IconButton(onClick = {
                            deleteTargets = selection.map { File(it) }
                        }) { Icon(Icons.Filled.Delete, contentDescription = "Delete") }
                    } else if (searchActive) {
                        IconButton(onClick = { searchActive = false; searchQuery = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "Tutup pencarian")
                        }
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Cari nama, extension...") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Text(rootLabel, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                        IconButton(onClick = { searchActive = true }) {
                            Icon(Icons.Filled.Search, contentDescription = "Cari")
                        }
                        Box {
                            IconButton(onClick = { sortMenuOpen = true }) {
                                Icon(Icons.Filled.Sort, contentDescription = "Urutkan")
                            }
                            DropdownMenu(expanded = sortMenuOpen, onDismissRequest = { sortMenuOpen = false }) {
                                sortMenuLabels().forEach { (mode, label) ->
                                    DropdownMenuItem(text = { Text(label) }, onClick = {
                                        sortMode = mode; sortMenuOpen = false
                                    })
                                }
                            }
                        }
                    }
                }
                if (!searchActive) {
                    NazeBreadcrumb(
                        segments = pathStack.map { it.name.ifBlank { rootLabel } },
                        onSegmentClick = { index ->
                            while (pathStack.size > index + 1) pathStack.removeAt(pathStack.lastIndex)
                        }
                    )
                }
                if (clipboard != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Siap ${if (clipboard!!.second) "dipindahkan" else "disalin"}: ${clipboard!!.first.name}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = {
                            val (src, isCut) = clipboard!!
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    if (isCut) FileRepository.move(src, currentDir) else FileRepository.copy(src, currentDir)
                                }
                                clipboard = null
                                refresh()
                            }
                        }) { Text("Paste") }
                        TextButton(onClick = { clipboard = null }) { Text("Batal") }
                    }
                }
            }
        },
        floatingActionButton = {
            NazeExpandableFab(onAction = { action ->
                when (action) {
                    NazeFabAction.NEW_FILE -> showNewFileDialog = true
                    NazeFabAction.NEW_FOLDER -> showNewFolderDialog = true
                    NazeFabAction.NEW_PROJECT -> showNewProjectDialog = true
                    NazeFabAction.NEW_SECRET -> onNavigateToSecretsAndAdd()
                    NazeFabAction.IMPORT_FILE -> importLauncher.launch(arrayOf("*/*"))
                }
            })
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (nodes.isEmpty()) {
                Text(
                    if (searchActive) "Tidak ada hasil" else "Folder ini masih kosong",
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(nodes, key = { it.absolutePath }) { node ->
                        NazeFileListItem(
                            node = node,
                            selected = selection.contains(node.absolutePath),
                            selectionModeActive = selection.isNotEmpty(),
                            onClick = {
                                if (selection.isNotEmpty()) {
                                    if (!selection.remove(node.absolutePath)) selection.add(node.absolutePath)
                                } else {
                                    openNode(node)
                                }
                            },
                            onLongClick = {
                                if (node.kind == VaultFileKind.ARCHIVE) {
                                    // handled via click -> viewer, long-press still allows selection
                                }
                                if (!selection.remove(node.absolutePath)) selection.add(node.absolutePath)
                            },
                            onToggleFavorite = { toggleFavorite(node) }
                        )
                    }
                }
            }

            if (errorMessage != null) {
                androidx.compose.material3.Snackbar(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                    action = { TextButton(onClick = { errorMessage = null }) { Text("OK") } }
                ) { Text(errorMessage!!) }
            }
        }
    }

    if (showNewFolderDialog) {
        NameInputDialog(
            title = "Folder Baru",
            initialValue = "New Folder",
            onDismiss = { showNewFolderDialog = false },
            onConfirm = { name ->
                FileRepository.createFolder(currentDir, name)
                showNewFolderDialog = false
                refresh()
            }
        )
    }

    if (showNewFileDialog) {
        NameInputDialog(
            title = "File Baru",
            initialValue = "New File.txt",
            onDismiss = { showNewFileDialog = false },
            onConfirm = { name ->
                FileRepository.createFile(currentDir, name)
                showNewFileDialog = false
                refresh()
            }
        )
    }

    if (showNewProjectDialog) {
        NewProjectDialog(
            onDismiss = { showNewProjectDialog = false },
            onCreate = { name, templateId ->
                val projectsRoot = FileRepository.sectionDir(VaultSection.PROJECTS)
                ProjectBuilder.scaffold(projectsRoot, name, templateId)
                showNewProjectDialog = false
                refresh()
            }
        )
    }

    renameTarget?.let { target ->
        NameInputDialog(
            title = "Ganti Nama",
            initialValue = target.name,
            onDismiss = { renameTarget = null },
            onConfirm = { name ->
                FileRepository.rename(target, name)
                renameTarget = null
                refresh()
            }
        )
    }

    deleteTargets?.let { targets ->
        AlertDialog(
            onDismissRequest = { deleteTargets = null },
            title = { Text("Hapus ${targets.size} item?") },
            text = { Text("Tindakan ini tidak dapat dibatalkan.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            targets.forEach { f ->
                                FileRepository.delete(f)
                                IndexStore.removePath(f.absolutePath)
                            }
                        }
                        selection.clear()
                        deleteTargets = null
                        refresh()
                    }
                }) { Text("Hapus", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleteTargets = null }) { Text("Batal") } }
        )
    }
}

@Composable
private fun NameInputDialog(title: String, initialValue: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(value = text, onValueChange = { text = it }, singleLine = true)
        },
        confirmButton = { TextButton(onClick = { onConfirm(text) }) { Text("Simpan") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@Composable
private fun NewProjectDialog(onDismiss: () -> Unit, onCreate: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var selectedTemplate by remember { mutableStateOf(ProjectTemplates.first().id) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Project Baru") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama project") },
                    singleLine = true
                )
                Column(Modifier.padding(top = 12.dp)) {
                    ProjectTemplates.forEach { tpl ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            androidx.compose.material3.RadioButton(
                                selected = selectedTemplate == tpl.id,
                                onClick = { selectedTemplate = tpl.id }
                            )
                            Column {
                                Text(tpl.label, style = MaterialTheme.typography.bodyLarge)
                                Text(tpl.description, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onCreate(name, selectedTemplate) }) { Text("Buat") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

private fun sortMenuLabels() = listOf(
    FileSortMode.NAME_ASC to "Nama (A-Z)",
    FileSortMode.NAME_DESC to "Nama (Z-A)",
    FileSortMode.DATE_NEWEST to "Terbaru",
    FileSortMode.DATE_OLDEST to "Terlama",
    FileSortMode.SIZE_LARGEST to "Ukuran terbesar",
    FileSortMode.SIZE_SMALLEST to "Ukuran terkecil",
    FileSortMode.TYPE to "Tipe file"
)

private fun shareFiles(context: android.content.Context, files: List<File>) {
    if (files.isEmpty()) return
    val uris = files.map { f ->
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", f)
    }
    val intent = Intent().apply {
        action = if (uris.size == 1) Intent.ACTION_SEND else Intent.ACTION_SEND_MULTIPLE
        if (uris.size == 1) putExtra(Intent.EXTRA_STREAM, uris.first())
        else putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        type = "*/*"
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Bagikan"))
}
