package com.naze.vault.ui.screens

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.naze.vault.security.VaultLockManager
import com.naze.vault.ui.theme.NazeBlue
import com.naze.vault.ui.theme.NazeDanger
import com.naze.vault.ui.theme.NazePurple
import com.naze.vault.ui.theme.NazeSurfaceElevated
import com.naze.vault.ui.theme.NazeTextSecondary

private const val PIN_LENGTH = 6

@Composable
fun LockScreen(onUnlocked: () -> Unit) {
    val context = LocalContext.current
    val isFirstTime = !VaultLockManager.hasPinConfigured()

    var stage by remember { mutableStateOf(if (isFirstTime) LockStage.CREATE else LockStage.VERIFY) }
    var pinInput by remember { mutableStateOf("") }
    var firstPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun tryBiometric() {
        val activity = context as? FragmentActivity ?: return
        val canAuth = BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) return

        val executor = ContextCompat.getMainExecutor(context)
        val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                VaultLockManager.unlock()
                onUnlocked()
            }
        })
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Buka Naze Vault")
            .setSubtitle("Gunakan sidik jari atau wajah")
            .setNegativeButtonText("Gunakan PIN")
            .build()
        prompt.authenticate(info)
    }

    LaunchedEffect(Unit) {
        if (stage == LockStage.VERIFY && VaultLockManager.isBiometricEnabled()) {
            tryBiometric()
        }
    }

    fun onDigit(digit: String) {
        errorMessage = null
        if (pinInput.length >= PIN_LENGTH) return
        pinInput += digit
        if (pinInput.length == PIN_LENGTH) {
            when (stage) {
                LockStage.CREATE -> {
                    firstPin = pinInput
                    pinInput = ""
                    stage = LockStage.CONFIRM
                }
                LockStage.CONFIRM -> {
                    if (pinInput == firstPin) {
                        VaultLockManager.setPin(pinInput)
                        VaultLockManager.unlock()
                        onUnlocked()
                    } else {
                        errorMessage = "PIN tidak cocok, coba lagi"
                        pinInput = ""
                        stage = LockStage.CREATE
                    }
                }
                LockStage.VERIFY -> {
                    if (VaultLockManager.verifyPin(pinInput)) {
                        VaultLockManager.unlock()
                        onUnlocked()
                    } else {
                        errorMessage = "PIN salah"
                        pinInput = ""
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(NazeBlue.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Lock, contentDescription = null, tint = NazeBlue, modifier = Modifier.size(32.dp))
        }

        androidx.compose.foundation.layout.Spacer(Modifier.height(20.dp))

        Text("NAZE VAULT", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        androidx.compose.foundation.layout.Spacer(Modifier.height(6.dp))
        Text(
            text = when (stage) {
                LockStage.CREATE -> "Buat PIN untuk mengamankan vault"
                LockStage.CONFIRM -> "Masukkan ulang PIN"
                LockStage.VERIFY -> "Your files. Your secrets. Protected."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = NazeTextSecondary
        )

        androidx.compose.foundation.layout.Spacer(Modifier.height(32.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(PIN_LENGTH) { index ->
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .background(
                            if (index < pinInput.length) NazeBlue else NazeSurfaceElevated,
                            CircleShape
                        )
                )
            }
        }

        if (errorMessage != null) {
            androidx.compose.foundation.layout.Spacer(Modifier.height(12.dp))
            Text(errorMessage!!, color = NazeDanger, style = MaterialTheme.typography.bodyMedium)
        }

        androidx.compose.foundation.layout.Spacer(Modifier.height(36.dp))

        NumPad(
            onDigit = ::onDigit,
            onBackspace = { if (pinInput.isNotEmpty()) pinInput = pinInput.dropLast(1) },
            onBiometric = if (stage == LockStage.VERIFY && VaultLockManager.isBiometricEnabled()) ::tryBiometric else null
        )
    }
}

private enum class LockStage { CREATE, CONFIRM, VERIFY }

@Composable
private fun NumPad(onDigit: (String) -> Unit, onBackspace: () -> Unit, onBiometric: (() -> Unit)?) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9")
    )
    Column(verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                row.forEach { digit -> NumPadKey(digit) { onDigit(digit) } }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            if (onBiometric != null) {
                IconButton(onClick = onBiometric) {
                    Icon(Icons.Filled.Fingerprint, contentDescription = "Biometrik", tint = NazePurple)
                }
            } else {
                Box(modifier = Modifier.size(56.dp))
            }
            NumPadKey("0") { onDigit("0") }
            IconButton(onClick = onBackspace) {
                Icon(Icons.Filled.Backspace, contentDescription = "Hapus", tint = NazeTextSecondary)
            }
        }
    }
}

@Composable
private fun NumPadKey(digit: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .background(NazeSurfaceElevated, CircleShape)
            .then(Modifier),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.TextButton(onClick = onClick) {
            Text(digit, style = MaterialTheme.typography.titleLarge)
        }
    }
}
