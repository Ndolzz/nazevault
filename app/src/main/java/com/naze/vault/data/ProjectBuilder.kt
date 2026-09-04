package com.naze.vault.data

import java.io.File

/**
 * Turns a template id (or a pasted custom tree) into real folders and files
 * under Projects/<projectName>. Nothing here is decorative — every path
 * listed is actually created on disk.
 */
object ProjectBuilder {

    fun scaffold(projectsDir: File, projectName: String, templateId: String): Result<File> = runCatching {
        val safeName = projectName.trim().ifBlank { "new-project" }
        val root = File(projectsDir, safeName)
        if (root.exists()) throw IllegalStateException("Project '$safeName' sudah ada")
        root.mkdirs()

        when (templateId) {
            "empty" -> { /* just the root folder */ }
            "web" -> {
                File(root, "css").mkdirs()
                File(root, "js").mkdirs()
                File(root, "assets").mkdirs()
                File(root, "index.html").writeText(webIndexHtml(safeName))
                File(root, "css/style.css").writeText("body {\n  margin: 0;\n  font-family: sans-serif;\n}\n")
                File(root, "js/main.js").writeText("console.log(\"$safeName ready\");\n")
                File(root, "README.md").writeText("# $safeName\n\nWeb project scaffolded by Naze Vault.\n")
            }
            "android" -> {
                val pkgPath = File(root, "app/src/main/java/com/example/app")
                pkgPath.mkdirs()
                File(root, "app/src/main/res").mkdirs()
                File(root, "app/src/main/AndroidManifest.xml").writeText(
                    "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\" package=\"com.example.app\" />\n"
                )
                File(root, "build.gradle.kts").writeText("// root build file\n")
                File(root, "settings.gradle.kts").writeText("rootProject.name = \"$safeName\"\ninclude(\":app\")\n")
                File(root, "README.md").writeText("# $safeName\n\nAndroid project scaffolded by Naze Vault.\n")
            }
            "node" -> {
                File(root, "src").mkdirs()
                File(root, "package.json").writeText(nodePackageJson(safeName))
                File(root, "src/index.js").writeText("console.log(\"$safeName running\");\n")
                File(root, ".gitignore").writeText("node_modules/\n.env\n")
                File(root, "README.md").writeText("# $safeName\n\nNode.js project scaffolded by Naze Vault.\n")
            }
            "python" -> {
                File(root, "main.py").writeText("def main():\n    print(\"$safeName running\")\n\n\nif __name__ == \"__main__\":\n    main()\n")
                File(root, "requirements.txt").writeText("")
                File(root, "README.md").writeText("# $safeName\n\nPython project scaffolded by Naze Vault.\n")
            }
            "react" -> {
                File(root, "src/components").mkdirs()
                File(root, "src/pages").mkdirs()
                File(root, "public").mkdirs()
                File(root, "src/App.jsx").writeText(reactAppJsx(safeName))
                File(root, "package.json").writeText(nodePackageJson(safeName))
                File(root, "README.md").writeText("# $safeName\n\nReact project scaffolded by Naze Vault.\n")
            }
            "custom" -> { /* root only — caller adds structure via +Folder/+File or pasted tree */ }
            else -> { /* unrecognized template id -> fall back to an empty project rather than faking one */ }
        }
        root
    }

    /**
     * Parses a simple pasted tree (one path per line, "/" separated, trailing
     * "/" = folder) and creates it under [root]. Lines with tree-drawing
     * characters (├── └── │) are tolerated and stripped.
     */
    fun scaffoldFromTree(root: File, treeText: String): Result<Unit> = runCatching {
        val cleanedLines = treeText.lines()
            .map { line -> line.replace(Regex("[│├└─]"), "").trim() }
            .filter { it.isNotBlank() }

        cleanedLines.forEach { rawPath ->
            val relative = rawPath.trimStart('/').trimEnd()
            if (relative.isBlank()) return@forEach
            val isFolder = relative.endsWith("/")
            val target = File(root, relative.removeSuffix("/"))
            if (isFolder) {
                target.mkdirs()
            } else {
                target.parentFile?.mkdirs()
                if (!target.exists()) target.createNewFile()
            }
        }
    }

    private fun webIndexHtml(name: String) = """
        <!DOCTYPE html>
        <html lang="id">
        <head>
          <meta charset="UTF-8" />
          <title>$name</title>
          <link rel="stylesheet" href="css/style.css" />
        </head>
        <body>
          <h1>$name</h1>
          <script src="js/main.js"></script>
        </body>
        </html>
    """.trimIndent() + "\n"

    private fun nodePackageJson(name: String) = """
        {
          "name": "${name.lowercase().replace(" ", "-")}",
          "version": "1.0.0",
          "main": "src/index.js",
          "scripts": {
            "start": "node src/index.js"
          }
        }
    """.trimIndent() + "\n"

    private fun reactAppJsx(name: String) = """
        export default function App() {
          return <div>$name</div>;
        }
    """.trimIndent() + "\n"
}
