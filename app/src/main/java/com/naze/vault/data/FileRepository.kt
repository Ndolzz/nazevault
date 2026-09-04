package com.naze.vault.data

import android.content.Context
import android.net.Uri
import com.naze.vault.data.model.FileSortMode
import com.naze.vault.data.model.VaultNode
import com.naze.vault.data.model.VaultSection
import com.naze.vault.util.FileUtils
import java.io.File
import java.io.IOException

/**
 * Everything here touches the real filesystem. No mocks, no dummy data —
 * every call reflects (and mutates) what is actually on disk under the
 * app's private storage, so nothing here requires a runtime permission.
 */
object FileRepository {

    lateinit var rootDir: File
        private set

    fun init(context: Context) {
        val base = context.getExternalFilesDir(null) ?: context.filesDir
        rootDir = File(base, "NazeVault").apply { mkdirs() }
        VaultSection.entries.forEach { section ->
            File(rootDir, section.folderName).mkdirs()
        }
    }

    fun sectionDir(section: VaultSection): File = File(rootDir, section.folderName)

    fun list(dir: File, sort: FileSortMode = FileSortMode.NAME_ASC, favorites: Set<String> = emptySet()): List<VaultNode> {
        val children = dir.listFiles()?.toList().orEmpty()
            // Hide the internal secrets/index folders from the generic file browser.
            .filterNot { it.name.startsWith(".") && it.parentFile == rootDir }
        val nodes = children.map { toNode(it, favorites) }
        return sortNodes(nodes, sort)
    }

    fun sortNodes(nodes: List<VaultNode>, sort: FileSortMode): List<VaultNode> {
        val (folders, files) = nodes.partition { it.isDirectory }
        val comparator: Comparator<VaultNode> = when (sort) {
            FileSortMode.NAME_ASC -> compareBy { it.name.lowercase() }
            FileSortMode.NAME_DESC -> compareByDescending { it.name.lowercase() }
            FileSortMode.DATE_NEWEST -> compareByDescending { it.lastModified }
            FileSortMode.DATE_OLDEST -> compareBy { it.lastModified }
            FileSortMode.SIZE_LARGEST -> compareByDescending { it.sizeBytes }
            FileSortMode.SIZE_SMALLEST -> compareBy { it.sizeBytes }
            FileSortMode.TYPE -> compareBy { it.kind.name }
        }
        return folders.sortedWith(comparator) + files.sortedWith(comparator)
    }

    fun toNode(file: File, favorites: Set<String> = emptySet()): VaultNode {
        return VaultNode(
            name = file.name,
            absolutePath = file.absolutePath,
            isDirectory = file.isDirectory,
            sizeBytes = if (file.isDirectory) 0L else file.length(),
            lastModified = file.lastModified(),
            kind = FileUtils.detectKind(file),
            isFavorite = favorites.contains(file.absolutePath)
        )
    }

    fun createFolder(parent: File, name: String): Result<File> = runCatching {
        val safeName = FileUtils.uniqueName(parent, name.trim().ifBlank { "New Folder" })
        val dir = File(parent, safeName)
        if (!dir.mkdirs()) throw IOException("Gagal membuat folder")
        dir
    }

    fun createFile(parent: File, name: String, initialContent: String = ""): Result<File> = runCatching {
        val safeName = FileUtils.uniqueName(parent, name.trim().ifBlank { "New File.txt" })
        val file = File(parent, safeName)
        if (!file.createNewFile()) throw IOException("Gagal membuat file")
        if (initialContent.isNotEmpty()) file.writeText(initialContent)
        file
    }

    fun rename(file: File, newName: String): Result<File> = runCatching {
        val target = File(file.parentFile, newName.trim())
        if (target.exists()) throw IOException("Nama sudah digunakan")
        if (!file.renameTo(target)) throw IOException("Gagal mengganti nama")
        target
    }

    fun delete(file: File): Result<Unit> = runCatching {
        if (!file.deleteRecursively()) throw IOException("Gagal menghapus")
    }

    fun copy(src: File, destDir: File): Result<File> = runCatching {
        val targetName = FileUtils.uniqueName(destDir, src.name)
        val target = File(destDir, targetName)
        if (src.isDirectory) src.copyRecursively(target, overwrite = false)
        else src.copyTo(target, overwrite = false)
        target
    }

    fun move(src: File, destDir: File): Result<File> = runCatching {
        val targetName = FileUtils.uniqueName(destDir, src.name)
        val target = File(destDir, targetName)
        if (src.isDirectory) {
            src.copyRecursively(target, overwrite = false)
            src.deleteRecursively()
        } else {
            src.copyTo(target, overwrite = false)
            src.delete()
        }
        target
    }

    fun duplicate(src: File): Result<File> = copy(src, src.parentFile ?: rootDir)

    fun search(root: File, query: String): List<VaultNode> {
        if (query.isBlank()) return emptyList()
        val results = mutableListOf<VaultNode>()
        fun walk(dir: File) {
            dir.listFiles()?.forEach { child ->
                if (child.name.startsWith(".") && child.parentFile == rootDir) return@forEach
                if (child.name.contains(query, ignoreCase = true)) results.add(toNode(child))
                if (child.isDirectory) walk(child)
            }
        }
        walk(root)
        return results
    }

    fun readText(file: File): Result<String> = runCatching { file.readText() }

    fun writeText(file: File, content: String): Result<Unit> = runCatching { file.writeText(content) }

    /** Imports a file the user picked via the system picker (Storage Access Framework). */
    fun importFromUri(context: Context, uri: Uri, destDir: File): Result<File> = runCatching {
        val displayName = queryDisplayName(context, uri) ?: "imported_${System.currentTimeMillis()}"
        val safeName = FileUtils.uniqueName(destDir, displayName)
        val target = File(destDir, safeName)
        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: throw IOException("Tidak bisa membaca file sumber")
        target
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex >= 0) cursor.getString(nameIndex) else null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun countAll(dir: File): Triple<Int, Int, Long> {
        var files = 0
        var folders = 0
        var totalSize = 0L
        fun walk(d: File) {
            d.listFiles()?.forEach { child ->
                if (child.name.startsWith(".") && child.parentFile == rootDir) return@forEach
                if (child.isDirectory) {
                    folders++
                    walk(child)
                } else {
                    files++
                    totalSize += child.length()
                }
            }
        }
        walk(dir)
        return Triple(files, folders, totalSize)
    }
}
