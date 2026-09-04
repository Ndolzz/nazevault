package com.naze.vault.data

import android.content.Context
import com.naze.vault.data.model.RecentEntry
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Favorites and "recent" activity are metadata, not file content, so they're
 * kept in two small private JSON files instead of pulling in Room/SQLite for
 * two lists. Lives under internal filesDir — never inside the visible vault.
 */
object IndexStore {

    private lateinit var favoritesFile: File
    private lateinit var recentsFile: File
    private const val MAX_RECENTS = 60

    fun init(context: Context) {
        val dir = File(context.filesDir, "index").apply { mkdirs() }
        favoritesFile = File(dir, "favorites.json")
        recentsFile = File(dir, "recents.json")
        if (!favoritesFile.exists()) favoritesFile.writeText("[]")
        if (!recentsFile.exists()) recentsFile.writeText("[]")
    }

    fun getFavorites(): Set<String> {
        val arr = JSONArray(favoritesFile.readText())
        return (0 until arr.length()).map { arr.getString(it) }.toSet()
    }

    fun toggleFavorite(path: String): Set<String> {
        val current = getFavorites().toMutableSet()
        if (!current.remove(path)) current.add(path)
        favoritesFile.writeText(JSONArray(current.toList()).toString())
        return current
    }

    fun isFavorite(path: String): Boolean = getFavorites().contains(path)

    fun getRecents(): List<RecentEntry> {
        val arr = JSONArray(recentsFile.readText())
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            RecentEntry(
                path = o.optString("path"),
                action = o.optString("action"),
                timestamp = o.optLong("timestamp")
            )
        }.sortedByDescending { it.timestamp }
    }

    fun recordActivity(path: String, action: String) {
        val entries = getRecents().toMutableList()
        entries.removeAll { it.path == path }
        entries.add(0, RecentEntry(path, action, System.currentTimeMillis()))
        val trimmed = entries.take(MAX_RECENTS)
        val arr = JSONArray()
        trimmed.forEach { entry ->
            arr.put(JSONObject().apply {
                put("path", entry.path)
                put("action", entry.action)
                put("timestamp", entry.timestamp)
            })
        }
        recentsFile.writeText(arr.toString())
    }

    fun removePath(path: String) {
        // Called after delete/rename so stale entries don't linger.
        val favorites = getFavorites().toMutableSet()
        if (favorites.remove(path)) {
            favoritesFile.writeText(JSONArray(favorites.toList()).toString())
        }
        val recents = getRecents().filterNot { it.path == path }
        val arr = JSONArray()
        recents.forEach { entry ->
            arr.put(JSONObject().apply {
                put("path", entry.path)
                put("action", entry.action)
                put("timestamp", entry.timestamp)
            })
        }
        recentsFile.writeText(arr.toString())
    }
}
