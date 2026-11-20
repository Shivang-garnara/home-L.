package com.example.launcherai.search

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings

data class ShortcutResult(
    val title: String,
    val subtitle: String,
    val intent: Intent
)

object ShortcutSearchHelper {

    fun searchShortcuts(context: Context, query: String): List<ShortcutResult> {
        if (query.isBlank()) return emptyList()
        val q = query.lowercase().trim()
        val results = mutableListOf<ShortcutResult>()

        if ("alarm".contains(q) || "clock".contains(q) || "timer".contains(q)) {
            results.add(
                ShortcutResult("Set Alarm", "Clock action", Intent(AlarmClock.ACTION_SET_ALARM).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            )
        }
        if ("camera".contains(q) || "photo".contains(q) || "picture".contains(q)) {
            results.add(
                ShortcutResult("Take a picture", "Camera action", Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            )
        }
        if ("settings".contains(q) || "wifi".contains(q) || "bluetooth".contains(q)) {
            results.add(
                ShortcutResult("Open Settings", "System settings", Intent(Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            )
        }
        if ("web".contains(q) || "search".contains(q) || "google".contains(q) || q.length >= 3) {
            val searchIntent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra(SearchManager.QUERY, query)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            results.add(
                ShortcutResult("Search web for \"$query\"", "Web search", searchIntent)
            )
        }
        return results
    }
}
