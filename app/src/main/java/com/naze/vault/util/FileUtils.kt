package com.naze.vault.util

import android.webkit.MimeTypeMap
import com.naze.vault.data.model.VaultFileKind
import java.io.File
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow

private val CODE_EXT = setOf(
    "kt", "java", "js", "ts", "jsx", "tsx", "py", "c", "cpp", "h", "hpp",
    "css", "html", "xml", "gradle", "yml", "yaml", "sh", "rb", "go", "rs", "swift", "php"
)

object FileUtils {

    fun detectKind(file: File): VaultFileKind {
        if (file.isDirectory) return VaultFileKind.FOLDER
        return when (file.extension.lowercase(Locale.ROOT)) {
            "md", "markdown" -> VaultFileKind.MARKDOWN
            "json" -> VaultFileKind.JSON
            "txt", "log", "csv" -> VaultFileKind.TEXT
            "jpg", "jpeg", "png", "webp", "gif", "bmp" -> VaultFileKind.IMAGE
            "mp4", "mkv", "webm", "3gp", "mov" -> VaultFileKind.VIDEO
            "mp3", "wav", "m4a", "ogg", "flac" -> VaultFileKind.AUDIO
            "pdf" -> VaultFileKind.PDF
            "zip", "jar" -> VaultFileKind.ARCHIVE
            in CODE_EXT -> VaultFileKind.CODE
            else -> VaultFileKind.UNKNOWN
        }
    }

    fun mimeTypeFor(file: File): String {
        val ext = file.extension.lowercase(Locale.ROOT)
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
    }

    fun formatSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt().coerceIn(0, units.size - 1)
        val value = bytes / 1024.0.pow(digitGroups.toDouble())
        return String.format(Locale.getDefault(), if (digitGroups == 0) "%.0f %s" else "%.1f %s", value, units[digitGroups])
    }

    fun formatDate(timestampMillis: Long): String {
        return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault())
            .format(Date(timestampMillis))
    }

    /** Returns a non-colliding path inside [directory] for a proposed [desiredName]. */
    fun uniqueName(directory: File, desiredName: String): String {
        var candidate = desiredName
        var counter = 1
        val dotIndex = desiredName.lastIndexOf('.')
        val base = if (dotIndex > 0) desiredName.substring(0, dotIndex) else desiredName
        val ext = if (dotIndex > 0) desiredName.substring(dotIndex) else ""
        while (File(directory, candidate).exists()) {
            candidate = "$base ($counter)$ext"
            counter++
        }
        return candidate
    }
}
