package com.naze.vault

import android.app.Application
import com.naze.vault.data.FileRepository
import com.naze.vault.data.IndexStore
import com.naze.vault.security.SecretsRepository
import com.naze.vault.security.VaultLockManager

class NazeVaultApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Ensure the vault root and its top-level sections exist on first launch.
        FileRepository.init(this)
        IndexStore.init(this)
        SecretsRepository.init(this)
        VaultLockManager.init(this)
    }
}
