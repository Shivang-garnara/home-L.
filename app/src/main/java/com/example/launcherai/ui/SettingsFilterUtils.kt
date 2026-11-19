package com.example.launcherai.ui

/**
 * Pure production helper functions for Settings search filtering and default launcher banner logic.
 * Called directly by LauncherSettingsActivity and tested directly by unit tests.
 */
object SettingsFilterUtils {

    /**
     * Determines whether a settings row should be visible based on search query.
     */
    fun matchesQuery(query: String, keywords: String): Boolean {
        val q = query.trim().lowercase()
        return q.isEmpty() || keywords.lowercase().contains(q)
    }

    /**
     * Computes whether the "Not set as default" warning banner should be visible.
     */
    fun shouldShowDefaultBanner(isDefaultLauncher: Boolean): Boolean {
        return !isDefaultLauncher
    }
}
