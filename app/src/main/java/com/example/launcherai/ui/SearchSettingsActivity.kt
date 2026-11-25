package com.example.launcherai.ui

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.launcherai.databinding.ActivitySearchSettingsBinding

/**
 * Dedicated Search Settings Activity matching Nova Launcher 9.0.1 Search.
 * Supports all one-line toggles (Limit 1 row, Microresults, Folders, Shortcuts, Contacts,
 * Contact Card View, Recent Shortcuts, Private Keyboard) and clickable dialogs for
 * Mode, API Integrations, Web Suggestions, Search Providers, Nova AI in Search, and Search Bars.
 */
class SearchSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchSettingsBinding
    private lateinit var prefs: SharedPreferences

    companion object {
        const val PREFS_NAME = "workspace_prefs"

        const val KEY_SEARCH_MODE = "key_search_mode"
        const val KEY_SEARCH_LIMIT_ONE_ROW = "key_search_limit_one_row"
        const val KEY_SEARCH_MICRORESULTS = "key_search_microresults"
        const val KEY_SEARCH_FOLDERS = "key_search_folders"

        const val KEY_SHORTCUT_SEARCH = "key_shortcut_search"
        const val KEY_CONTACT_SEARCH = "key_contact_search"
        const val KEY_CONTACT_CARD_VIEW = "key_contact_card_view"
        const val KEY_WEB_SUGGESTIONS = "key_web_suggestions"
        const val KEY_SEARCH_PROVIDERS = "key_search_providers"
        const val KEY_NOVA_AI_IN_SEARCH = "key_nova_ai_in_search"
        const val KEY_API_INTEGRATIONS = "key_api_integrations"

        const val KEY_RECENT_SEARCH_SHORTCUTS = "key_recent_search_shortcuts"
        const val KEY_PRIVATE_KEYBOARD = "key_private_keyboard"

        const val KEY_SEARCH_BAR_PLACEMENT_HOME = "key_search_bar_placement_home"
        const val KEY_HOME_BAR_RADIUS = "key_home_bar_radius"
        const val KEY_HOME_BAR_SHADOW = "key_home_bar_shadow"
        const val KEY_HOME_BAR_WIREFRAME = "key_home_bar_wireframe"
        const val KEY_HOME_BAR_INTERFACE = "key_home_bar_interface"

        const val KEY_SEARCH_BAR_PLACEMENT_DRAWER = "key_search_bar_placement_drawer"
        const val KEY_DRAWER_BAR_RADIUS = "key_drawer_bar_radius"
        const val KEY_DRAWER_BAR_VOICE = "key_drawer_bar_voice"

        const val KEY_NOVA_NOW_BAR_ENABLED = "key_nova_now_bar_enabled"
        const val KEY_NOVA_NOW_BAR_RADIUS = "key_nova_now_bar_radius"

        val ALL_PROVIDERS = arrayOf(
            "Google", "Play Store", "Wikipedia", "Google Maps",
            "YouTube", "DuckDuckGo", "YouTube Music", "Spotify"
        )
        val DEFAULT_PROVIDERS = setOf("Google", "Play Store", "Wikipedia", "Google Maps", "YouTube")

        val ALL_INTEGRATIONS = arrayOf("Calendar", "Spotify", "Deezer", "Twitch", "Slack", "Discord")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        SystemBarHelper.applySystemBars(this, isLightTheme = true)
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        binding.btnBack.setOnClickListener { finish() }

        setupLayoutAndResults()
        setupShortcutSearchAndProviders()
        setupBehaviorAndPrivacy()
        setupSearchBars()
        refreshUi()
    }

    // =========================================================================
    // 1. LAYOUT & ON-DEVICE RESULTS
    // =========================================================================
    private fun setupLayoutAndResults() {
        binding.rowSearchMode.setOnClickListener {
            val modes = arrayOf("Immersive", "Window")
            val current = prefs.getString(KEY_SEARCH_MODE, "Immersive") ?: "Immersive"
            val idx = modes.indexOfFirst { it.equals(current, ignoreCase = true) }.coerceAtLeast(0)
            AlertDialog.Builder(this)
                .setTitle("Mode")
                .setSingleChoiceItems(modes, idx) { dialog, which ->
                    prefs.edit().putString(KEY_SEARCH_MODE, modes[which]).apply()
                    refreshUi()
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.swLimitOneRow.isChecked = prefs.getBoolean(KEY_SEARCH_LIMIT_ONE_ROW, false)
        binding.swLimitOneRow.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_SEARCH_LIMIT_ONE_ROW, isChecked).apply()
        }

        binding.swMicroresults.isChecked = prefs.getBoolean(KEY_SEARCH_MICRORESULTS, true)
        binding.swMicroresults.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_SEARCH_MICRORESULTS, isChecked).apply()
        }

        binding.swSearchFolders.isChecked = prefs.getBoolean(KEY_SEARCH_FOLDERS, true)
        binding.swSearchFolders.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_SEARCH_FOLDERS, isChecked).apply()
        }
    }

    // =========================================================================
    // 2. SHORTCUT SEARCH & PROVIDERS
    // =========================================================================
    private fun setupShortcutSearchAndProviders() {
        binding.swShortcutSearch.isChecked = prefs.getBoolean(KEY_SHORTCUT_SEARCH, true)
        binding.swShortcutSearch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_SHORTCUT_SEARCH, isChecked).apply()
        }

        val hasContactPerm = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        binding.swContactSearch.isChecked = prefs.getBoolean(KEY_CONTACT_SEARCH, false) && hasContactPerm
        binding.swContactSearch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_CONTACTS), 201)
                binding.swContactSearch.isChecked = false
            } else {
                prefs.edit().putBoolean(KEY_CONTACT_SEARCH, isChecked).apply()
            }
        }

        binding.swContactCardView.isChecked = prefs.getBoolean(KEY_CONTACT_CARD_VIEW, true)
        binding.swContactCardView.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_CONTACT_CARD_VIEW, isChecked).apply()
        }

        binding.rowApiIntegrations.setOnClickListener {
            val saved = prefs.getStringSet(KEY_API_INTEGRATIONS, setOf("Calendar")) ?: setOf("Calendar")
            val checked = BooleanArray(ALL_INTEGRATIONS.size) { saved.contains(ALL_INTEGRATIONS[it]) }
            AlertDialog.Builder(this)
                .setTitle("API Integrations")
                .setMultiChoiceItems(ALL_INTEGRATIONS, checked) { _, which, isChecked ->
                    checked[which] = isChecked
                }
                .setPositiveButton("Save") { _, _ ->
                    val newSet = ALL_INTEGRATIONS.filterIndexed { i, _ -> checked[i] }.toSet()
                    prefs.edit().putStringSet(KEY_API_INTEGRATIONS, newSet).apply()
                    refreshUi()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.rowWebSuggestions.setOnClickListener {
            val options = arrayOf("None", "DuckDuckGo", "Google")
            val current = prefs.getString(KEY_WEB_SUGGESTIONS, "Google") ?: "Google"
            val idx = options.indexOfFirst { it.equals(current, ignoreCase = true) }.coerceAtLeast(0)
            AlertDialog.Builder(this)
                .setTitle("Web suggestions")
                .setSingleChoiceItems(options, idx) { dialog, which ->
                    prefs.edit().putString(KEY_WEB_SUGGESTIONS, options[which]).apply()
                    refreshUi()
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.rowSearchProviders.setOnClickListener {
            val saved = prefs.getStringSet(KEY_SEARCH_PROVIDERS, DEFAULT_PROVIDERS) ?: DEFAULT_PROVIDERS
            val checked = BooleanArray(ALL_PROVIDERS.size) { saved.contains(ALL_PROVIDERS[it]) }
            AlertDialog.Builder(this)
                .setTitle("Search More providers")
                .setMultiChoiceItems(ALL_PROVIDERS, checked) { _, which, isChecked ->
                    checked[which] = isChecked
                }
                .setPositiveButton("Apply") { _, _ ->
                    val newSet = ALL_PROVIDERS.filterIndexed { i, _ -> checked[i] }.toSet()
                    prefs.edit().putStringSet(KEY_SEARCH_PROVIDERS, newSet).apply()
                    refreshUi()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.rowNovaAiInSearch.setOnClickListener {
            val options = arrayOf("Open on Enter", "Tap to open", "Hidden")
            val current = prefs.getString(KEY_NOVA_AI_IN_SEARCH, "Tap to open") ?: "Tap to open"
            val idx = options.indexOfFirst { it.equals(current, ignoreCase = true) }.coerceAtLeast(0)
            AlertDialog.Builder(this)
                .setTitle("Nova AI in search")
                .setSingleChoiceItems(options, idx) { dialog, which ->
                    prefs.edit().putString(KEY_NOVA_AI_IN_SEARCH, options[which]).apply()
                    refreshUi()
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    // =========================================================================
    // 3. BEHAVIOR & PRIVACY
    // =========================================================================
    private fun setupBehaviorAndPrivacy() {
        binding.swRecentShortcuts.isChecked = prefs.getBoolean(KEY_RECENT_SEARCH_SHORTCUTS, false)
        binding.swRecentShortcuts.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_RECENT_SEARCH_SHORTCUTS, isChecked).apply()
        }

        binding.swPrivateKeyboard.isChecked = prefs.getBoolean(KEY_PRIVATE_KEYBOARD, false)
        binding.swPrivateKeyboard.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_PRIVATE_KEYBOARD, isChecked).apply()
        }
    }

    // =========================================================================
    // 4. SEARCH BARS
    // =========================================================================
    private fun setupSearchBars() {
        binding.rowDesktopSearchBar.setOnClickListener {
            showDesktopSearchBarDialog()
        }

        binding.rowDrawerSearchBar.setOnClickListener {
            showDrawerSearchBarDialog()
        }

        binding.rowNovaNowSearchBar.setOnClickListener {
            showNovaNowSearchBarDialog()
        }
    }

    private fun showDesktopSearchBarDialog() {
        val placementLabels = arrayOf("None", "Widget", "Dock (Above icons)", "Dock (Below icons)", "Persistent")
        val placementKeys = arrayOf("NONE", "WIDGET", "DOCK_ABOVE", "DOCK_BELOW", "PERSISTENT")
        val currentPlacement = prefs.getString(KEY_SEARCH_BAR_PLACEMENT_HOME, "DOCK_BELOW") ?: "DOCK_BELOW"
        val placementIdx = placementKeys.indexOf(currentPlacement).coerceAtLeast(3)

        val currentRadius = prefs.getInt(KEY_HOME_BAR_RADIUS, 22)
        val hasShadow = prefs.getBoolean(KEY_HOME_BAR_SHADOW, true)
        val isWireframe = prefs.getBoolean(KEY_HOME_BAR_WIREFRAME, false)
        val searchInterface = prefs.getString(KEY_HOME_BAR_INTERFACE, "Nova Search") ?: "Nova Search"

        val items = arrayOf(
            "Placement: ${placementLabels[placementIdx]}",
            "Corner radius: ${currentRadius}dp",
            "Shadow: ${if (hasShadow) "ON" else "OFF"}",
            "Wireframe style: ${if (isWireframe) "ON" else "OFF"}",
            "Search interface: $searchInterface"
        )

        AlertDialog.Builder(this)
            .setTitle("Desktop search bar")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> AlertDialog.Builder(this)
                        .setTitle("Placement")
                        .setSingleChoiceItems(placementLabels, placementIdx) { d, idx ->
                            prefs.edit().putString(KEY_SEARCH_BAR_PLACEMENT_HOME, placementKeys[idx]).apply()
                            refreshUi()
                            d.dismiss()
                        }.show()
                    1 -> {
                        val radii = arrayOf("4dp", "10dp", "16dp", "22dp (Pill)", "28dp")
                        val radVals = intArrayOf(4, 10, 16, 22, 28)
                        val rIdx = radVals.indexOfFirst { it == currentRadius }.coerceAtLeast(3)
                        AlertDialog.Builder(this)
                            .setTitle("Corner radius")
                            .setSingleChoiceItems(radii, rIdx) { d, idx ->
                                prefs.edit().putInt(KEY_HOME_BAR_RADIUS, radVals[idx]).apply()
                                refreshUi()
                                d.dismiss()
                            }.show()
                    }
                    2 -> {
                        prefs.edit().putBoolean(KEY_HOME_BAR_SHADOW, !hasShadow).apply()
                        refreshUi()
                    }
                    3 -> {
                        prefs.edit().putBoolean(KEY_HOME_BAR_WIREFRAME, !isWireframe).apply()
                        refreshUi()
                    }
                    4 -> {
                        val interfaces = arrayOf("Nova Search", "Directly use Google App")
                        val iIdx = interfaces.indexOf(searchInterface).coerceAtLeast(0)
                        AlertDialog.Builder(this)
                            .setTitle("Search interface")
                            .setSingleChoiceItems(interfaces, iIdx) { d, idx ->
                                prefs.edit().putString(KEY_HOME_BAR_INTERFACE, interfaces[idx]).apply()
                                refreshUi()
                                d.dismiss()
                            }.show()
                    }
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showDrawerSearchBarDialog() {
        val placements = arrayOf("None", "Top", "Bottom")
        val currentPlacement = prefs.getString(KEY_SEARCH_BAR_PLACEMENT_DRAWER, "Top") ?: "Top"
        val idx = placements.indexOfFirst { it.equals(currentPlacement, ignoreCase = true) }.coerceAtLeast(1)
        val hasVoice = prefs.getBoolean(KEY_DRAWER_BAR_VOICE, true)

        val items = arrayOf(
            "Placement: ${placements[idx]}",
            "Voice search icon: ${if (hasVoice) "ON" else "OFF"}"
        )

        AlertDialog.Builder(this)
            .setTitle("Drawer search bar")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> AlertDialog.Builder(this)
                        .setTitle("Placement")
                        .setSingleChoiceItems(placements, idx) { d, i ->
                            prefs.edit().putString(KEY_SEARCH_BAR_PLACEMENT_DRAWER, placements[i]).apply()
                            refreshUi()
                            d.dismiss()
                        }.show()
                    1 -> {
                        prefs.edit().putBoolean(KEY_DRAWER_BAR_VOICE, !hasVoice).apply()
                        refreshUi()
                    }
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showNovaNowSearchBarDialog() {
        val enabled = prefs.getBoolean(KEY_NOVA_NOW_BAR_ENABLED, true)
        val radius = prefs.getInt(KEY_NOVA_NOW_BAR_RADIUS, 25)
        val items = arrayOf(
            "Enable: ${if (enabled) "ON" else "OFF"}",
            "Corner radius: $radius%"
        )

        AlertDialog.Builder(this)
            .setTitle("Nova Now search bar")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> {
                        prefs.edit().putBoolean(KEY_NOVA_NOW_BAR_ENABLED, !enabled).apply()
                        refreshUi()
                    }
                    1 -> {
                        val radii = arrayOf("0%", "15%", "25%", "50%")
                        val vals = intArrayOf(0, 15, 25, 50)
                        val idx = vals.indexOf(radius).coerceAtLeast(2)
                        AlertDialog.Builder(this)
                            .setTitle("Corner radius")
                            .setSingleChoiceItems(radii, idx) { d, i ->
                                prefs.edit().putInt(KEY_NOVA_NOW_BAR_RADIUS, vals[i]).apply()
                                refreshUi()
                                d.dismiss()
                            }.show()
                    }
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun refreshUi() {
        binding.tvSearchModeSubtitle.text = prefs.getString(KEY_SEARCH_MODE, "Immersive") ?: "Immersive"
        binding.tvWebSuggestionsSubtitle.text = prefs.getString(KEY_WEB_SUGGESTIONS, "Google") ?: "Google"
        binding.tvNovaAiInSearchSubtitle.text = prefs.getString(KEY_NOVA_AI_IN_SEARCH, "Tap to open") ?: "Tap to open"

        val providers = prefs.getStringSet(KEY_SEARCH_PROVIDERS, DEFAULT_PROVIDERS) ?: DEFAULT_PROVIDERS
        val orderedProviders = ALL_PROVIDERS.filter { providers.contains(it) }
        val providerText = if (orderedProviders.isEmpty()) "None" else orderedProviders.joinToString(", ")
        binding.tvSearchProvidersSubtitle.text = providerText
        binding.tvPreviewProviders.text = "Search with: $providerText"

        val integrations = prefs.getStringSet(KEY_API_INTEGRATIONS, setOf("Calendar")) ?: setOf("Calendar")
        binding.tvApiIntegrationsSubtitle.text = if (integrations.isEmpty()) "None connected" else "Connected: ${integrations.joinToString(", ")}"

        val placementMap = mapOf(
            "NONE" to "None", "WIDGET" to "Widget",
            "DOCK_ABOVE" to "Dock (Above icons)", "DOCK_BELOW" to "Dock (Below icons)",
            "PERSISTENT" to "Persistent"
        )
        val homePlace = placementMap[prefs.getString(KEY_SEARCH_BAR_PLACEMENT_HOME, "DOCK_BELOW")] ?: "Dock (Below icons)"
        val homeRad = prefs.getInt(KEY_HOME_BAR_RADIUS, 22)
        val homeShadow = prefs.getBoolean(KEY_HOME_BAR_SHADOW, true)
        val homeWireframe = prefs.getBoolean(KEY_HOME_BAR_WIREFRAME, false)
        binding.tvDesktopSearchBarSubtitle.text = "$homePlace • ${homeRad}dp radius"

        val drawerPlace = prefs.getString(KEY_SEARCH_BAR_PLACEMENT_DRAWER, "Top") ?: "Top"
        val drawerVoice = prefs.getBoolean(KEY_DRAWER_BAR_VOICE, true)
        binding.tvDrawerSearchBarSubtitle.text = "$drawerPlace" + if (drawerVoice) " • Voice search" else ""

        val novaNowEnabled = prefs.getBoolean(KEY_NOVA_NOW_BAR_ENABLED, true)
        val novaNowRad = prefs.getInt(KEY_NOVA_NOW_BAR_RADIUS, 25)
        binding.tvNovaNowSearchBarSubtitle.text = if (novaNowEnabled) "Enabled • $novaNowRad% corner radius" else "Disabled"

        // Update Top Live Search Bar Preview
        val density = resources.displayMetrics.density
        val bg = GradientDrawable().apply {
            cornerRadius = homeRad * density
            if (homeWireframe) {
                setColor(android.graphics.Color.TRANSPARENT)
                setStroke((1.5f * density).toInt(), android.graphics.Color.parseColor("#6D5D55"))
            } else {
                setColor(android.graphics.Color.WHITE)
            }
        }
        binding.previewSearchBar.background = bg
        binding.previewSearchBar.elevation = if (homeShadow) 4f * density else 0f
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 201) {
            val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            prefs.edit().putBoolean(KEY_CONTACT_SEARCH, granted).apply()
            binding.swContactSearch.isChecked = granted
            if (!granted) Toast.makeText(this, "Contacts permission denied", Toast.LENGTH_SHORT).show()
        }
    }
}
