package com.example.launcherai

import com.example.launcherai.search.MicroresultsEngine
import com.example.launcherai.ui.SearchBarConfig
import com.example.launcherai.ui.SearchSettingsActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for SearchSettingsActivity keys and MicroresultsEngine (math & unit conversions).
 */
class SearchSettingsTest {

    @Test
    fun testSearchSettingsPreferenceKeys() {
        assertEquals("key_search_mode", SearchSettingsActivity.KEY_SEARCH_MODE)
        assertEquals("key_search_limit_one_row", SearchSettingsActivity.KEY_SEARCH_LIMIT_ONE_ROW)
        assertEquals("key_search_microresults", SearchSettingsActivity.KEY_SEARCH_MICRORESULTS)
        assertEquals("key_search_folders", SearchSettingsActivity.KEY_SEARCH_FOLDERS)
        assertEquals("key_shortcut_search", SearchSettingsActivity.KEY_SHORTCUT_SEARCH)
        assertEquals("key_contact_search", SearchSettingsActivity.KEY_CONTACT_SEARCH)
        assertEquals("key_contact_card_view", SearchSettingsActivity.KEY_CONTACT_CARD_VIEW)
        assertEquals("key_web_suggestions", SearchSettingsActivity.KEY_WEB_SUGGESTIONS)
        assertEquals("key_search_providers", SearchSettingsActivity.KEY_SEARCH_PROVIDERS)
        assertEquals("key_nova_ai_in_search", SearchSettingsActivity.KEY_NOVA_AI_IN_SEARCH)
        assertEquals("key_recent_search_shortcuts", SearchSettingsActivity.KEY_RECENT_SEARCH_SHORTCUTS)
        assertEquals("key_private_keyboard", SearchSettingsActivity.KEY_PRIVATE_KEYBOARD)
    }

    @Test
    fun testMathMicroresults() {
        val r1 = MicroresultsEngine.evaluate("25*4")
        assertNotNull(r1)
        assertEquals("= 100", r1!!.title)
        assertEquals("100", r1.copyValue)

        val r2 = MicroresultsEngine.evaluate("(10 + 5) * 2")
        assertNotNull(r2)
        assertEquals("= 30", r2!!.title)

        val r3 = MicroresultsEngine.evaluate("100 / 0")
        assertNull(r3)
    }

    @Test
    fun testUnitConversionMicroresults() {
        val r1 = MicroresultsEngine.evaluate("10km to miles")
        assertNotNull(r1)
        assertEquals("6.214 mile", r1!!.title)
        assertEquals("10 kilometer", r1.subtitle)

        val r2 = MicroresultsEngine.evaluate("100c to f")
        assertNotNull(r2)
        assertEquals("212 °F", r2!!.title)

        val r3 = MicroresultsEngine.evaluate("2gb to mb")
        assertNotNull(r3)
        assertEquals("2,048 MB", r3!!.title)
    }

    @Test
    fun testSearchBarConfigActionsAndDateFormat() {
        assertEquals(8, SearchBarConfig.DEFAULT_DESKTOP_ACTIONS.size)
        assertEquals(7, SearchBarConfig.DEFAULT_DRAWER_ACTIONS.size)
        assertTrue(SearchBarConfig.DEFAULT_DESKTOP_ACTIONS.contains(SearchBarConfig.ACTION_APP_DRAWER))
        assertFalse(SearchBarConfig.DEFAULT_DRAWER_ACTIONS.contains(SearchBarConfig.ACTION_APP_DRAWER))

        assertEquals(12, SearchBarConfig.SYSTEM_PALETTE_COLORS.size)
        assertEquals(36, SearchBarConfig.FIXED_PALETTE_COLORS.size)

        val d1 = SearchBarConfig.formatDate("January 31")
        val d2 = SearchBarConfig.formatDate("Jan 31")
        val d3 = SearchBarConfig.formatDate("1/31")
        assertTrue(d1.isNotBlank())
        assertTrue(d2.isNotBlank())
        assertTrue(d3.contains("/"))
    }
}
