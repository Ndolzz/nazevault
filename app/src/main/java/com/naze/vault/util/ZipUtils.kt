package com.naze.vault.util

import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ZipUtils {

    /** Zips [source] (file or folder) into [destZip]. Runs synchronously — call off the main thread. */
    fun createZip(source: File, destZip: File): Result<File> = runCatching {
        ZipOutputStream(destZip.outputStream().buffered()).use { zos ->
            if (source.isDirectory) {
                val basePath = source.parentFile?.absolutePath ?: source.absolutePath
                source.walkTopDown().filter { it != source }.forEach { file ->
                    val entryName = file.absolutePath.removePrefix(basePath).trimStart('/', '\\')
                    if (file.isDirectory) {
                        zos.putNextEntry(ZipEntry("$entryName/"))
                        zos.closeEntry()
                    } else {
                        zos.putNextEntry(ZipEntry(entryName))
                        file.inputStream().use { it.copyTo(zos) }
                        zos.closeEntry()
                    }
                }
            } else {
                zos.putNextEntry(ZipEntry(source.name))
                source.inputStream().use { it.copyTo(zos) }
                zos.closeEntry()
            }
        }
        destZip
    }

    /** Zips an arbitrary selection of files and/or folders into [destZip]. Runs synchronously — call off the main thread. */
    fun createZipFromFiles(files: List<File>, destZip: File): Result<File> = runCatching {
        ZipOutputStream(destZip.outputStream().buffered()).use { zos ->
            files.forEach { src ->
                if (src.isDirectory) {
                    val basePath = src.parentFile?.absolutePath ?: ""
                    src.walkTopDown().filter { it != src }.forEach { file ->
                        val entryName = file.absolutePath.removePrefix(basePath).trimStart('/', '\\')
                        if (file.isDirectory) {
                            zos.putNextEntry(ZipEntry("$entryName/"))
                            zos.closeEntry()
                        } else {
                            zos.putNextEntry(ZipEntry(entryName))
                            file.inputStream().use { it.copyTo(zos) }
                            zos.closeEntry()
                        }
                    }
                } else {
                    zos.putNextEntry(ZipEntry(src.name))
                    src.inputStream().use { it.copyTo(zos) }
                    zos.closeEntry()
                }
            }
        }
        destZip
    }

    /** Extracts [zipFile] into [destDir], guarding against zip-slip path traversal. */
    fun extractZip(zipFile: File, destDir: File): Result<File> = runCatching {
        destDir.mkdirs()
        val canonicalDest = destDir.canonicalPath
        ZipInputStream(zipFile.inputStream().buffered()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val outFile = File(destDir, entry.name)
                if (!outFile.canonicalPath.startsWith(canonicalDest + File.separator) && outFile.canonicalPath != canonicalDest) {
                    throw IOException("Entry ZIP tidak valid: " + entry.name)
                }
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    outFile.outputStream().use { output -> zis.copyTo(output) }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        destDir
    }

    /**
     * Extracts a single entry from [zipFile] into [destDir], flattening it to its
     * file name (guards against zip-slip). Returns null when the entry does not
     * exist. Runs synchronously — call off the main thread for large entries.
     */
    fun extractEntry(zipFile: File, entryPath: String, destDir: File): Result<File?> = runCatching {
        destDir.mkdirs()
        val canonicalDest = destDir.canonicalPath
        ZipFile(zipFile).use { zf ->
            val entry = zf.getEntry(entryPath) ?: return@runCatching null
            val outFile = File(destDir, File(entry.name).name)
            if (!outFile.canonicalPath.startsWith(canonicalDest + File.separator)) {
                throw IOException("Entry ZIP tidak valid: " + entry.name)
            }
            zf.getInputStream(entry).use { input ->
                outFile.outputStream().use { output -> input.copyTo(output) }
            }
            outFile
        }
    }

    data class ZipTreeEntry(val path: String, val isDirectory: Boolean, val size: Long)

    fun listContents(zipFile: File): Result<List<ZipTreeEntry>> = runCatching {
        ZipFile(zipFile).use { zf ->
            zf.entries().asSequence().map { entry ->
                ZipTreeEntry(entry.name, entry.isDirectory, entry.size)
            }.toList()
        }
    }
}
