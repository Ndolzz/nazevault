package com.naze.vault.data.model

/** A section under the vault root. Each maps to a real folder on disk. */
enum class VaultSection(val folderName: String, val label: String) {
    VAULT("Vault", "Vault"),
    PROJECTS("Projects", "Projects"),
    SECRETS(".secrets", "Secrets") // dot-prefixed: not shown by generic gallery/media scanners
}

enum class FileSortMode { NAME_ASC, NAME_DESC, DATE_NEWEST, DATE_OLDEST, SIZE_LARGEST, SIZE_SMALLEST, TYPE }

enum class VaultFileKind {
    FOLDER, CODE, TEXT, MARKDOWN, JSON, IMAGE, VIDEO, AUDIO, PDF, ARCHIVE, UNKNOWN
}

data class VaultNode(
    val name: String,
    val absolutePath: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val lastModified: Long,
    val kind: VaultFileKind,
    val isFavorite: Boolean = false
)

data class SecretEntry(
    val id: String,
    val title: String,
    val category: String, // API Key / Token / Password / Env Var / Credential / Other
    val cipherText: String, // Base64 AES-GCM ciphertext
    val iv: String,         // Base64 IV
    val createdAt: Long,
    val updatedAt: Long
)

data class RecentEntry(
    val path: String,
    val action: String, // opened / created / modified
    val timestamp: Long
)

data class ProjectTemplate(
    val id: String,
    val label: String,
    val description: String
)

val ProjectTemplates = listOf(
    ProjectTemplate("empty", "Empty Project", "Folder kosong, kamu susun sendiri"),
    ProjectTemplate("web", "Web Project", "index.html, css/, js/"),
    ProjectTemplate("android", "Android Project", "Struktur dasar app/src/main"),
    ProjectTemplate("node", "Node.js Project", "package.json, src/, .gitignore"),
    ProjectTemplate("python", "Python Project", "main.py, requirements.txt"),
    ProjectTemplate("react", "React Project", "src/components, src/pages, public/"),
    ProjectTemplate("custom", "Custom Project", "Tempel struktur folder kamu sendiri")
)
