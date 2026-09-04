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

    /** Extracts [zipFile] into [destDir], guarding against zip-slip path traversal. */
    fun extractZip(zipFile: File, destDir: File): Result<File> = runCatching {
        destDir.mkdirs()
        val canonicalDest = destDir.canonicalPath
        ZipInputStream(zipFile.inputStream().buffered()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val outFile = File(destDir, entry.name)
                if (!outFile.canonicalPath.startsWith(canonicalDest + File.separator) && outFile.canonicalPath != canonicalDest) {
                    throw IOException("Entry ZIP tidak valid: ${entry.name}")
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

    data class ZipTreeEntry(val path: String, val isDirectory: Boolean, val size: Long)

    fun listContents(zipFile: File): Result<List<ZipTreeEntry>> = runCatching {
        ZipFile(zipFile).use { zf ->
            zf.entries().asSequence().map { entry ->
                ZipTreeEntry(entry.name, entry.isDirectory, entry.size)
            }.toList()
        }
    }
}
