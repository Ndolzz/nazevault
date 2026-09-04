package com.naze.vault.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import com.naze.vault.data.FileRepository
import com.naze.vault.data.IndexStore
import com.naze.vault.data.model.VaultFileKind
import com.naze.vault.ui.theme.NazeTextSecondary
import com.naze.vault.util.FileUtils
import com.naze.vault.util.ZipUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileViewerScreen(file: File, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val kind = remember(file) { FileUtils.detectKind(file) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(file.name, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Kembali") }
                },
                actions = {
                    IconButton(onClick = {
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, FileUtils.mimeTypeFor(file))
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        runCatching { context.startActivity(Intent.createChooser(intent, "Buka dengan")) }
                    }) { Icon(Icons.Filled.OpenInNew, contentDescription = "Open with") }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (kind) {
                VaultFileKind.CODE, VaultFileKind.TEXT, VaultFileKind.MARKDOWN -> TextEditorView(file)
                VaultFileKind.JSON -> JsonView(file)
                VaultFileKind.IMAGE -> ImageView(file)
                VaultFileKind.VIDEO -> VideoPlayerView(file)
                VaultFileKind.AUDIO -> AudioView(file)
                VaultFileKind.PDF -> PdfView(file)
                VaultFileKind.ARCHIVE -> ZipContentsView(file)
                else -> UnsupportedView(file, context)
            }
        }
    }
}

@Composable
private fun TextEditorView(file: File) {
    val scope = rememberCoroutineScope()
    var content by remember { mutableStateOf(TextFieldLoading) }
    var editable by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    DisposableEffect(file) {
        val result = FileRepository.readText(file)
        content = result.getOrDefault("(Gagal membaca file)")
        onDispose { }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            if (editable) {
                TextButton(onClick = {
                    saving = true
                    scope.launch {
                        withContext(Dispatchers.IO) { FileRepository.writeText(file, content) }
                        IndexStore.recordActivity(file.absolutePath, "modified")
                        saving = false
                        editable = false
                    }
                }) {
                    Icon(Icons.Filled.Save, contentDescription = null)
                    Text(if (saving) " Menyimpan..." else " Simpan")
                }
            } else {
                TextButton(onClick = { editable = true }) {
                    Icon(Icons.Filled.Edit, contentDescription = null)
                    Text(" Edit")
                }
            }
        }
        OutlinedTextField(
            value = content,
            onValueChange = { if (editable) content = it },
            readOnly = !editable,
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
                .verticalScroll(rememberScrollState())
        )
    }
}

private const val TextFieldLoading = "Memuat..."

@Composable
private fun JsonView(file: File) {
    var pretty by remember { mutableStateOf("Memuat...") }
    DisposableEffect(file) {
        val raw = FileRepository.readText(file).getOrDefault("{}")
        pretty = runCatching {
            val trimmed = raw.trim()
            when {
                trimmed.startsWith("[") -> JSONArray(trimmed).toString(2)
                trimmed.isBlank() -> ""
                else -> JSONObject(trimmed).toString(2)
            }
        }.getOrElse { "Format JSON tidak valid:\n\n$raw" }
        onDispose { }
    }
    Text(
        text = pretty,
        fontFamily = FontFamily.Monospace,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .horizontalScroll(rememberScrollState())
    )
}

@Composable
private fun ImageView(file: File) {
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    val bitmap = remember(file) { runCatching { BitmapFactory.decodeFile(file.absolutePath) }.getOrNull() }

    if (bitmap == null) {
        Box(Modifier.fillMaxSize()) {
            Text(
                "Tidak dapat memuat gambar",
                modifier = Modifier.align(Alignment.Center),
                color = NazeTextSecondary
            )
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 6f)
                    offsetX += pan.x
                    offsetY += pan.y
                }
            }
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = file.name,
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center)
                .graphicsLayerScaleOffset(scale, offsetX, offsetY)
        )
    }
}

private fun Modifier.graphicsLayerScaleOffset(scale: Float, x: Float, y: Float): Modifier =
    this.then(
        Modifier.graphicsLayer(scaleX = scale, scaleY = scale, translationX = x, translationY = y)
    )

@Composable
private fun VideoPlayerView(file: File) {
    val context = LocalContext.current
    AndroidView(factory = {
        VideoView(context).apply {
            setVideoURI(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file))
            setMediaController(MediaController(context).also { it.setAnchorView(this) })
            setOnPreparedListener { start() }
        }
    }, modifier = Modifier.fillMaxSize())
}

@Composable
private fun AudioView(file: File) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    val player = remember(file) { android.media.MediaPlayer().apply { setDataSource(file.absolutePath); prepare() } }
    DisposableEffect(file) { onDispose { player.release() } }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(file.name, style = MaterialTheme.typography.titleMedium)
        androidx.compose.foundation.layout.Spacer(Modifier.padding(12.dp))
        TextButton(onClick = {
            if (isPlaying) player.pause() else player.start()
            isPlaying = !isPlaying
        }) { Text(if (isPlaying) "Pause" else "Play") }
    }
}

@Composable
private fun PdfView(file: File) {
    val pages = remember(file) { renderPdfPages(file) }
    if (pages.isEmpty()) {
        Text("Tidak dapat membuka PDF ini", modifier = Modifier.fillMaxWidth().padding(16.dp), color = NazeTextSecondary)
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(pages) { bmp ->
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            )
        }
    }
}

private fun renderPdfPages(file: File): List<Bitmap> {
    return runCatching {
        val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)
        val bitmaps = mutableListOf<Bitmap>()
        for (i in 0 until renderer.pageCount) {
            renderer.openPage(i).use { page ->
                val bmp = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmaps.add(bmp)
            }
        }
        renderer.close()
        pfd.close()
        bitmaps
    }.getOrElse { emptyList() }
}

@Composable
private fun ZipContentsView(file: File) {
    val entries = remember(file) { ZipUtils.listContents(file).getOrDefault(emptyList()) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("${file.name} • ${entries.size} entri", style = MaterialTheme.typography.titleMedium)
        androidx.compose.foundation.layout.Spacer(Modifier.padding(6.dp))
        LazyColumn {
            items(entries) { entry ->
                Text(
                    (if (entry.isDirectory) "📁 " else "📄 ") + entry.path,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun UnsupportedView(file: File, context: android.content.Context) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Format file ini belum didukung langsung.", style = MaterialTheme.typography.bodyLarge)
        androidx.compose.foundation.layout.Spacer(Modifier.padding(8.dp))
        TextButton(onClick = {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, FileUtils.mimeTypeFor(file))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            runCatching { context.startActivity(Intent.createChooser(intent, "Buka dengan")) }
        }) { Text("Open with...") }
    }
}
