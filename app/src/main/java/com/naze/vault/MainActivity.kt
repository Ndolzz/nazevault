package com.naze.vault

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.naze.vault.ui.NazeVaultApp

/**
 * FragmentActivity (not plain ComponentActivity) because BiometricPrompt
 * requires a FragmentActivity host.
 */
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NazeVaultApp()
        }
    }
}
