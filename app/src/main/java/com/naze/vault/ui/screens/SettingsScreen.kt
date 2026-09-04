package com.naze.vault.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.naze.vault.data.FileRepository
import com.naze.vault.security.VaultLockManager
import com.naze.vault.ui.theme.NazeSurfaceElevated
import com.naze.vault.ui.theme.NazeTextSecondary
import com.naze.vault.util.FileUtils
import com.naze.vault.util.ZipUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun SettingsScreen(onLockNow: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var biometricEnabled by remember { mutableStateOf(VaultLockManager.isBiometricEnabled()) }
    var autoLockMinutes by remember { mutableStateOf(VaultLockManager.getAutoLockMinutes()) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var showAutoLockDialog by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val biometricAvailable = remember {
        BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            statusMessage = "Membuat backup..."
            val tempZip = File(context.cacheDir, "naze_vault_backup_${System.currentTimeMillis()}.zip")
            withContext(Dispatchers.IO) {
                ZipUtils.createZip(FileRepository.rootDir, tempZip)
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    tempZip.inputStream().use { it.copyTo(out) }
                }
                tempZip.delete()
            }
            statusMessage = "Backup berhasil disimpan"
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            statusMessage = "Memulihkan backup..."
            withContext(Dispatchers.IO) {
                val tempZip = File(context.cacheDir, "restore_${System.currentTimeMillis()}.zip")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    tempZip.outputStream().use { input.copyTo(it) }
                }
                ZipUtils.extractZip(tempZip, FileRepository.rootDir)
                tempZip.delete()
            }
            statusMessage = "Backup berhasil dipulihkan. Buka ulang Vault untuk melihat perubahan."
        }
    }

    val (fileCount, folderCount, totalSize) = remember { FileRepository.countAll(FileRepository.rootDir) }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Text("Settings", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(20.dp))
        }

        item { SettingsSectionLabel("Keamanan") }
        item {
            SettingsRow(title = "Biometric Unlock", subtitle = if (biometricAvailable) "Gunakan sidik jari / wajah" else "Tidak didukung perangkat ini") {
                Switch(
                    checked = biometricEnabled && biometricAvailable,
                    enabled = biometricAvailable,
                    onCheckedChange = {
                        biometricEnabled = it
                        VaultLockManager.setBiometricEnabled(it)
                    }
                )
            }
        }
        item {
            ClickableSettingsRow(title = "Ganti PIN", subtitle = "Perbarui PIN vault kamu") {
                showChangePinDialog = true
            }
        }
        item {
            ClickableSettingsRow(title = "Auto Lock", subtitle = if (autoLockMinutes <= 0) "Tidak pernah" else "$autoLockMinutes menit setelah keluar aplikasi") {
                showAutoLockDialog = true
            }
        }
        item {
            ClickableSettingsRow(title = "Lock Now", subtitle = "Kunci vault sekarang") { onLockNow() }
        }

        item { SettingsSectionLabel("Backup") }
        item {
            ClickableSettingsRow(title = "Export Backup", subtitle = "Simpan seluruh vault sebagai file .zip") {
                exportLauncher.launch("naze_vault_backup.zip")
            }
        }
        item {
            ClickableSettingsRow(title = "Import / Restore Backup", subtitle = "Pulihkan dari file backup .zip") {
                importLauncher.launch(arrayOf("application/zip"))
            }
        }
        item {
            Text(
                "Backup tidak pernah diunggah ke cloud secara otomatis — sepenuhnya berada di kendali kamu.",
                style = MaterialTheme.typography.bodyMedium,
                color = NazeTextSecondary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
        }

        item { SettingsSectionLabel("Storage") }
        item {
            SettingsRow(title = "Digunakan", subtitle = "$fileCount file, $folderCount folder") {
                Text(FileUtils.formatSize(totalSize), color = NazeTextSecondary)
            }
        }

        item { SettingsSectionLabel("Tentang") }
        item {
            SettingsRow(title = "Naze Vault", subtitle = "Personal Secure Vault + File Manager + Project Workspace") {}
        }

        if (statusMessage != null) {
            item {
                Text(statusMessage!!, color = NazeTextSecondary, modifier = Modifier.padding(20.dp))
            }
        }

        item { androidx.compose.foundation.layout.Spacer(Modifier.padding(40.dp)) }
    }

    if (showChangePinDialog) {
        ChangePinDialog(onDismiss = { showChangePinDialog = false })
    }

    if (showAutoLockDialog) {
        AutoLockDialog(
            current = autoLockMinutes,
            onDismiss = { showAutoLockDialog = false },
            onSelect = { minutes ->
                autoLockMinutes = minutes
                VaultLockManager.setAutoLockMinutes(minutes)
                showAutoLockDialog = false
            }
        )
    }
}

@Composable
private fun SettingsSectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = NazeTextSecondary,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
    )
}

@Composable
private fun SettingsRow(title: String, subtitle: String, trailing: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = NazeTextSecondary)
        }
        trailing()
    }
}

@Composable
private fun ClickableSettingsRow(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = NazeTextSecondary)
        }
    }
}

@Composable
private fun ChangePinDialog(onDismiss: () -> Unit) {
    var current by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ganti PIN") },
        text = {
            Column {
                OutlinedTextField(value = current, onValueChange = { current = it }, label = { Text("PIN saat ini") }, singleLine = true)
                OutlinedTextField(value = newPin, onValueChange = { newPin = it }, label = { Text("PIN baru") }, singleLine = true, modifier = Modifier.padding(top = 8.dp))
                OutlinedTextField(value = confirmPin, onValueChange = { confirmPin = it }, label = { Text("Konfirmasi PIN baru") }, singleLine = true, modifier = Modifier.padding(top = 8.dp))
                if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when {
                    !VaultLockManager.verifyPin(current) -> error = "PIN saat ini salah"
                    newPin.length < 4 -> error = "PIN minimal 4 digit"
                    newPin != confirmPin -> error = "Konfirmasi PIN tidak cocok"
                    else -> {
                        VaultLockManager.setPin(newPin)
                        onDismiss()
                    }
                }
            }) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@Composable
private fun AutoLockDialog(current: Int, onDismiss: () -> Unit, onSelect: (Int) -> Unit) {
    val options = listOf(0 to "Tidak pernah", 1 to "1 menit", 5 to "5 menit", 15 to "15 menit", 30 to "30 menit")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Auto Lock") },
        text = {
            Column {
                options.forEach { (minutes, label) ->
                    Surface(
                        color = if (minutes == current) NazeSurfaceElevated else androidx.compose.ui.graphics.Color.Transparent,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(minutes) }
                    ) {
                        Text(label, modifier = Modifier.padding(12.dp))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Tutup") } }
    )
}
