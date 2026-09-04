package com.naze.vault.security

import android.content.Context
import com.naze.vault.data.model.SecretEntry
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * Every secret value is encrypted with CryptoManager (Keystore-backed AES-GCM)
 * before it ever reaches disk. The store file holds ciphertext + IV only —
 * never plaintext, and plaintext values are never written to Logcat.
 */
object SecretsRepository {

    private lateinit var storeFile: File

    fun init(context: Context) {
        val dir = File(context.filesDir, "index").apply { mkdirs() }
        storeFile = File(dir, "secrets.vault")
        if (!storeFile.exists()) storeFile.writeText("[]")
    }

    fun list(): List<SecretEntry> {
        val arr = JSONArray(storeFile.readText())
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            SecretEntry(
                id = o.optString("id"),
                title = o.optString("title"),
                category = o.optString("category"),
                cipherText = o.optString("cipherText"),
                iv = o.optString("iv"),
                createdAt = o.optLong("createdAt"),
                updatedAt = o.optLong("updatedAt")
            )
        }.sortedByDescending { it.updatedAt }
    }

    fun add(title: String, category: String, plainValue: String): SecretEntry {
        val encrypted = CryptoManager.encrypt(plainValue)
        val now = System.currentTimeMillis()
        val entry = SecretEntry(
            id = UUID.randomUUID().toString(),
            title = title,
            category = category,
            cipherText = encrypted.cipherTextB64,
            iv = encrypted.ivB64,
            createdAt = now,
            updatedAt = now
        )
        persist(list() + entry)
        return entry
    }

    fun update(id: String, title: String, category: String, plainValue: String?): SecretEntry? {
        val current = list()
        val existing = current.find { it.id == id } ?: return null
        val encrypted = plainValue?.let { CryptoManager.encrypt(it) }
        val updated = existing.copy(
            title = title,
            category = category,
            cipherText = encrypted?.cipherTextB64 ?: existing.cipherText,
            iv = encrypted?.ivB64 ?: existing.iv,
            updatedAt = System.currentTimeMillis()
        )
        persist(current.map { if (it.id == id) updated else it })
        return updated
    }

    fun delete(id: String) {
        persist(list().filterNot { it.id == id })
    }

    /** Decrypts on demand only — never cached in plaintext. */
    fun reveal(entry: SecretEntry): String = CryptoManager.decrypt(entry.cipherText, entry.iv)

    private fun persist(entries: List<SecretEntry>) {
        val arr = JSONArray()
        entries.forEach { e ->
            arr.put(JSONObject().apply {
                put("id", e.id)
                put("title", e.title)
                put("category", e.category)
                put("cipherText", e.cipherText)
                put("iv", e.iv)
                put("createdAt", e.createdAt)
                put("updatedAt", e.updatedAt)
            })
        }
        storeFile.writeText(arr.toString())
    }
}
