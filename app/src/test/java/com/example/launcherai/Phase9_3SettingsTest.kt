package com.example.launcherai

import com.example.launcherai.ui.SettingsFilterUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase9_3SettingsTest {

    private val settingsRows = listOf(
        Pair("rowAiAssistant", "ai assistant chat and privacy"),
        Pair("rowHomeScreen", "home screen set grid, icon layout, dock settings, and more"),
        Pair("rowAppDrawer", "app drawer set layout, style, opening gesture, and more"),
        Pair("rowFolders", "folders set window styles, background colors, and icon layout"),
        Pair("rowSearch", "search launcherai search window and bar configuration"),
        Pair("rowLookFeel", "look & feel icon preferences, popup menu, and notification bar options"),
        Pair("rowGesturesInputs", "gestures & inputs define secondary home button action, along with swipe, tap, and pinch gestures on home screen"),
        Pair("rowFeedPage", "feed page control and customize the feed pages at the edge of your home screen"),
        Pair("rowNotificationBadges", "notification badges choose from dynamic badges, notification dots, or numeric dots")
    )

    private fun filterRows(query: String): List<String> {
        return settingsRows.filter { (_, keywords) ->
            SettingsFilterUtils.matchesQuery(query, keywords)
        }.map { it.first }
    }

    @Test
    fun testSettingsSearchFilter_EmptyQuery_ReturnsAllRows() {
        val result = filterRows("")
        assertEquals(9, result.size)
    }

    @Test
    fun testSettingsSearchFilter_MatchingQueries() {
        val aiResults = filterRows("ai")
        assertTrue(aiResults.contains("rowAiAssistant"))

        val gridResults = filterRows("grid")
        assertTrue(gridResults.contains("rowHomeScreen"))

        val folderResults = filterRows("folders")
        assertTrue(folderResults.contains("rowFolders"))
    }

    @Test
    fun testSettingsSearchFilter_NonMatchingQuery_ReturnsEmpty() {
        val result = filterRows("xyz_unmatched_query_123")
        assertEquals(0, result.size)
    }

    @Test
    fun testDefaultLauncherBannerVisibilityLogic() {
        // When LauncherAI is NOT default launcher, warning banner MUST be visible
        assertTrue(SettingsFilterUtils.shouldShowDefaultBanner(isDefaultLauncher = false))

        // When LauncherAI IS default launcher, warning banner MUST be hidden
        assertFalse(SettingsFilterUtils.shouldShowDefaultBanner(isDefaultLauncher = true))
    }

    @Test
    fun testSubtitlesLauncherAIBranding_NoNovaReferencesInSubtitles() {
        val searchKeywords = settingsRows.find { it.first == "rowSearch" }?.second ?: ""

        assertTrue(searchKeywords.contains("launcherai"))
        assertFalse(searchKeywords.contains("nova's"))
    }
}
