package com.example.launcherai.ui

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.launcherai.R
import com.example.launcherai.ai.ui.NovaAiChatActivity
import com.example.launcherai.data.db.LauncherDatabase
import com.example.launcherai.data.model.AppInfo
import com.example.launcherai.databinding.ActivityChooseActionBinding
import com.example.launcherai.gestures.NamedAction
import com.example.launcherai.gestures.NovaAction
import com.example.launcherai.util.AppManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 3-tab Nova Action Picker (NOVA | APPS | SHORTCUTS) matching Nova ChooseActionIntentActivity.
 * Used both by Gestures & inputs and by per-icon/per-folder Edit swipe action assignment.
 */
class ChooseActionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChooseActionBinding
    private lateinit var adapter: PickerAdapter

    private var activeTab = TAB_NOVA
    private var query = ""
    private var isFolderMode = false
    private var pageCount = 2

    private var novaItems: List<PickerRow> = emptyList()
    private var appItems: List<PickerRow> = emptyList()
    private var shortcutGroups: MutableList<ShortcutGroup> = mutableListOf()
    private var appsLoaded = false
    private var shortcutsLoaded = false

    private val externalShortcutLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val data = result.data!!
            @Suppress("DEPRECATION")
            val shortcutIntent = data.getParcelableExtra<Intent>(Intent.EXTRA_SHORTCUT_INTENT)
            val shortcutName = data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME) ?: "Shortcut"
            if (shortcutIntent != null) {
                val action = NamedAction.fromIntent(shortcutName, shortcutIntent)
                finishWithAction(action)
            } else {
                Toast.makeText(this, "Shortcut did not return an action", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_NovaLight)
        super.onCreate(savedInstanceState)
        SystemBarHelper.applySystemBars(this, isLightTheme = true, transparentBars = false)
        binding = ActivityChooseActionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        SystemBarHelper.applyEdgeToEdgeInsets(
            insetsRootView = binding.root,
            topInsetView = binding.headerContainer
        )

        isFolderMode = intent.getBooleanExtra(EXTRA_FOLDER_MODE, false)
        val titleText = intent.getStringExtra(EXTRA_TITLE) ?: "Choose action"
        binding.tvPickerTitle.text = titleText

        binding.btnBack.setOnClickListener { finish() }

        adapter = PickerAdapter { row -> handleRowClick(row) }
        binding.rvActionItems.layoutManager = LinearLayoutManager(this)
        binding.rvActionItems.adapter = adapter

        buildNovaTabItems()
        lifecycleScope.launch(Dispatchers.IO) {
            val pages = LauncherDatabase.getInstance(applicationContext).workspaceDao().getAllPagesSync()
            pageCount = pages.size.coerceAtLeast(1)
        }

        binding.tabNova.setOnClickListener { selectTab(TAB_NOVA) }
        binding.tabApps.setOnClickListener { selectTab(TAB_APPS) }
        binding.tabShortcuts.setOnClickListener { selectTab(TAB_SHORTCUTS) }

        binding.etFilterActions.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                query = s?.toString().orEmpty().trim()
                renderActiveTab()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        selectTab(TAB_NOVA)
    }

    private fun selectTab(tab: Int) {
        activeTab = tab
        val orange = ContextCompat.getColor(this, R.color.nova_orange)
        val muted = ContextCompat.getColor(this, R.color.nova_text_brown_secondary)

        binding.tvTabNova.setTextColor(if (tab == TAB_NOVA) orange else muted)
        binding.indicatorTabNova.visibility = if (tab == TAB_NOVA) View.VISIBLE else View.INVISIBLE

        binding.tvTabApps.setTextColor(if (tab == TAB_APPS) orange else muted)
        binding.indicatorTabApps.visibility = if (tab == TAB_APPS) View.VISIBLE else View.INVISIBLE

        binding.tvTabShortcuts.setTextColor(if (tab == TAB_SHORTCUTS) orange else muted)
        binding.indicatorTabShortcuts.visibility = if (tab == TAB_SHORTCUTS) View.VISIBLE else View.INVISIBLE

        if (tab == TAB_APPS && !appsLoaded) {
            loadAppsTab()
        } else if (tab == TAB_SHORTCUTS && !shortcutsLoaded) {
            loadShortcutsTab()
        } else {
            renderActiveTab()
        }
    }

    private fun buildNovaTabItems() {
        val actions = if (isFolderMode) NovaAction.FOLDER_PICKER_ACTIONS else NovaAction.PICKER_ACTIONS
        novaItems = actions.map { action ->
            PickerRow.NovaRow(
                action = action,
                iconRes = iconForNovaAction(action),
                tintIcon = action != NovaAction.NONE
            )
        }
    }

    private fun loadAppsTab() {
        binding.progressPicker.visibility = View.VISIBLE
        lifecycleScope.launch {
            val apps = withContext(Dispatchers.IO) {
                val installed = AppManager.getInstalledApps(this@ChooseActionActivity)
                val labelCounts = installed.groupingBy { it.label }.eachCount()
                installed.map { app ->
                    val dup = (labelCounts[app.label] ?: 0) > 1
                    PickerRow.AppRow(
                        appInfo = app,
                        subtitle = if (dup) app.packageName else null
                    )
                }
            }
            appItems = apps
            appsLoaded = true
            binding.progressPicker.visibility = View.GONE
            if (activeTab == TAB_APPS) renderActiveTab()
        }
    }

    private fun loadShortcutsTab() {
        binding.progressPicker.visibility = View.VISIBLE
        lifecycleScope.launch {
            val groups = withContext(Dispatchers.IO) {
                buildShortcutGroups()
            }
            shortcutGroups.clear()
            shortcutGroups.addAll(groups)
            shortcutsLoaded = true
            binding.progressPicker.visibility = View.GONE
            if (activeTab == TAB_SHORTCUTS) renderActiveTab()
        }
    }

    private fun buildShortcutGroups(): List<ShortcutGroup> {
        val groups = mutableListOf<ShortcutGroup>()

        // Group 1: Built-in LauncherAI Activities (mirrors Nova -> Activities shortcut group)
        val activityChildren = listOf(
            PickerRow.ActivityRow("LauncherAI Settings", packageName, LauncherSettingsActivity::class.java.name,
                ContextCompat.getDrawable(this, R.drawable.ic_settings)),
            PickerRow.ActivityRow("Home screen settings", packageName, HomeScreenSettingsActivity::class.java.name,
                ContextCompat.getDrawable(this, R.drawable.ic_settings_home)),
            PickerRow.ActivityRow("App drawer settings", packageName, AppDrawerSettingsActivity::class.java.name,
                ContextCompat.getDrawable(this, R.drawable.ic_settings_drawer)),
            PickerRow.ActivityRow("Folders settings", packageName, FolderSettingsActivity::class.java.name,
                ContextCompat.getDrawable(this, R.drawable.ic_settings_folders)),
            PickerRow.ActivityRow("Search settings", packageName, SearchSettingsActivity::class.java.name,
                ContextCompat.getDrawable(this, R.drawable.ic_settings_search)),
            PickerRow.ActivityRow("Look & feel settings", packageName, LookFeelSettingsActivity::class.java.name,
                ContextCompat.getDrawable(this, R.drawable.ic_settings_look_feel)),
            PickerRow.ActivityRow("Gestures & inputs", packageName, GesturesSettingsActivity::class.java.name,
                ContextCompat.getDrawable(this, R.drawable.ic_settings_gestures)),
            PickerRow.ActivityRow("Nova AI Chat", packageName, NovaAiChatActivity::class.java.name,
                ContextCompat.getDrawable(this, R.drawable.ic_nova_ai))
        )
        groups.add(
            ShortcutGroup(
                id = "launcherai_activities",
                title = "LauncherAI Activities",
                icon = ContextCompat.getDrawable(this, R.mipmap.ic_launcher),
                children = activityChildren
            )
        )

        // Group 2: External ACTION_CREATE_SHORTCUT apps queried from PackageManager
        val pm = packageManager
        val scIntent = Intent(Intent.ACTION_CREATE_SHORTCUT)
        val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(scIntent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(scIntent, 0)
        }
        val byPkg = resolved
            .filter { it.activityInfo != null && it.activityInfo.exported }
            .groupBy { it.activityInfo.packageName }

        val externalGroups = byPkg.entries.mapNotNull { (pkg, infos) ->
            val appLabel = try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                pm.getApplicationLabel(appInfo).toString()
            } catch (_: Exception) {
                infos.firstOrNull()?.loadLabel(pm)?.toString() ?: pkg
            }
            val appIcon = try {
                pm.getApplicationIcon(pkg)
            } catch (_: Exception) {
                pm.defaultActivityIcon
            }
            val children = infos.map { ri ->
                PickerRow.ExternalShortcutConfigRow(
                    scTitle = ri.loadLabel(pm)?.toString() ?: appLabel,
                    packageName = ri.activityInfo.packageName,
                    activityName = ri.activityInfo.name,
                    icon = try { ri.loadIcon(pm) } catch (_: Exception) { appIcon }
                )
            }
            ShortcutGroup(
                id = "pkg_$pkg",
                title = appLabel,
                icon = appIcon,
                children = children
            )
        }.sortedBy { it.title.lowercase() }

        groups.addAll(externalGroups)
        return groups
    }

    private fun renderActiveTab() {
        val q = query.lowercase()
        val displayRows = when (activeTab) {
            TAB_NOVA -> {
                if (q.isEmpty()) novaItems else novaItems.filter { it.title.lowercase().contains(q) }
            }
            TAB_APPS -> {
                if (q.isEmpty()) appItems else appItems.filter {
                    it.title.lowercase().contains(q) ||
                        ((it as? PickerRow.AppRow)?.appInfo?.packageName?.lowercase()?.contains(q) == true)
                }
            }
            else -> {
                val list = mutableListOf<PickerRow>()
                for (group in shortcutGroups) {
                    val matchingChildren = if (q.isEmpty()) {
                        group.children
                    } else {
                        group.children.filter {
                            it.title.lowercase().contains(q) || group.title.lowercase().contains(q)
                        }
                    }
                    if (q.isNotEmpty() && matchingChildren.isEmpty()) continue
                    if (group.children.size == 1 && group.children.first() is PickerRow.ExternalShortcutConfigRow && q.isEmpty()) {
                        list.add(
                            PickerRow.ShortcutGroupRow(
                                group = group.copy(expanded = false),
                                childCount = group.children.size
                            )
                        )
                    } else {
                        val isExpanded = group.expanded || q.isNotEmpty()
                        list.add(PickerRow.ShortcutGroupRow(group.copy(expanded = isExpanded), group.children.size))
                        if (isExpanded) {
                            list.addAll(matchingChildren)
                        }
                    }
                }
                list
            }
        }
        adapter.submitList(displayRows)
    }

    private fun handleRowClick(row: PickerRow) {
        when (row) {
            is PickerRow.NovaRow -> {
                if (row.action == NovaAction.GOTO_SCREEN) {
                    showJumpToPageDialog()
                } else {
                    finishWithAction(NamedAction.nova(row.action))
                }
            }
            is PickerRow.AppRow -> {
                finishWithAction(
                    NamedAction.app(
                        label = row.appInfo.label,
                        packageName = row.appInfo.packageName,
                        className = row.appInfo.activityName
                    )
                )
            }
            is PickerRow.ShortcutGroupRow -> {
                if (row.group.children.size == 1 && row.group.children.first() is PickerRow.ExternalShortcutConfigRow) {
                    launchExternalShortcutConfig(row.group.children.first() as PickerRow.ExternalShortcutConfigRow)
                    return
                }
                val idx = shortcutGroups.indexOfFirst { it.id == row.group.id }
                if (idx >= 0) {
                    shortcutGroups[idx] = shortcutGroups[idx].copy(expanded = !shortcutGroups[idx].expanded)
                    renderActiveTab()
                }
            }
            is PickerRow.ActivityRow -> {
                val actIntent = Intent().apply {
                    component = ComponentName(row.packageName, row.className)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                finishWithAction(NamedAction.fromIntent(row.title, actIntent))
            }
            is PickerRow.ExternalShortcutConfigRow -> {
                launchExternalShortcutConfig(row)
            }
        }
    }

    private fun launchExternalShortcutConfig(row: PickerRow.ExternalShortcutConfigRow) {
        try {
            val configIntent = Intent(Intent.ACTION_CREATE_SHORTCUT).apply {
                component = ComponentName(row.packageName, row.activityName)
            }
            externalShortcutLauncher.launch(configIntent)
        } catch (e: Exception) {
            Toast.makeText(this, "Cannot configure shortcut: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showJumpToPageDialog() {
        val count = pageCount.coerceAtLeast(1)
        val items = Array(count) { i -> "Page ${i + 1}" }
        AlertDialog.Builder(this)
            .setTitle("Jump to page")
            .setItems(items) { _, which ->
                finishWithAction(NamedAction.nova(NovaAction.GOTO_SCREEN, pageIndex = which))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun finishWithAction(action: NamedAction) {
        val out = Intent().apply {
            putExtra(EXTRA_SELECTED_ACTION, action.serialize())
            putExtra(EXTRA_SLOT_KEY, intent.getStringExtra(EXTRA_SLOT_KEY))
        }
        setResult(Activity.RESULT_OK, out)
        finish()
    }

    private fun iconForNovaAction(action: NovaAction): Int {
        return when (action) {
            NovaAction.NONE -> R.drawable.ic_clear
            NovaAction.APP_DRAWER -> R.drawable.ic_bar_app_drawer
            NovaAction.DEFAULT_SCREEN -> R.drawable.ic_settings_home
            NovaAction.GOTO_SCREEN -> R.drawable.ic_settings_home
            NovaAction.EXPAND_STATUS_BAR -> R.drawable.ic_notifications
            NovaAction.EXPAND_STATUS_SETTINGS_BAR -> R.drawable.ic_settings
            NovaAction.TOGGLE_STATUS_BAR -> R.drawable.ic_notifications
            NovaAction.TOGGLE_DOCK -> R.drawable.ic_bar_app_drawer
            NovaAction.SHOW_PREVIEWS -> R.drawable.ic_settings_home
            NovaAction.RECENT_APPS -> R.drawable.ic_settings_cards
            NovaAction.APP_SEARCH -> R.drawable.ic_search
            NovaAction.TEXT_SEARCH -> R.drawable.ic_bar_logo_google_g
            NovaAction.VOICE_SEARCH -> R.drawable.ic_bar_voice_search
            NovaAction.ASSIST -> R.drawable.ic_bar_assistant
            NovaAction.GOOGLE_LENS -> R.drawable.ic_bar_lens
            NovaAction.GOOGLE_NOW -> R.drawable.ic_settings_feed
            NovaAction.NOVA_SETTINGS -> R.drawable.ic_settings
            NovaAction.SCREEN_OFF -> R.drawable.ic_clock
            NovaAction.FIRST_ITEM_IN_FOLDER -> R.drawable.ic_settings_folders
            NovaAction.OPEN_FOLDER -> R.drawable.ic_settings_folders
        }
    }

    sealed class PickerRow(val title: String) {
        data class NovaRow(val action: NovaAction, val iconRes: Int, val tintIcon: Boolean) : PickerRow(action.title)
        data class AppRow(val appInfo: AppInfo, val subtitle: String?) : PickerRow(appInfo.label)
        data class ShortcutGroupRow(val group: ShortcutGroup, val childCount: Int) : PickerRow(group.title)
        data class ActivityRow(
            val activityTitle: String,
            val packageName: String,
            val className: String,
            val icon: Drawable?
        ) : PickerRow(activityTitle)
        data class ExternalShortcutConfigRow(
            val scTitle: String,
            val packageName: String,
            val activityName: String,
            val icon: Drawable?
        ) : PickerRow(scTitle)
    }

    data class ShortcutGroup(
        val id: String,
        val title: String,
        val icon: Drawable?,
        val children: List<PickerRow>,
        val expanded: Boolean = false
    )

    private inner class PickerAdapter(
        private val onClick: (PickerRow) -> Unit
    ) : RecyclerView.Adapter<PickerAdapter.VH>() {

        private var items: List<PickerRow> = emptyList()

        fun submitList(newItems: List<PickerRow>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun getItemCount(): Int = items.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_action_picker_row, parent, false)
            return VH(view)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            holder.bind(items[position])
        }

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            private val ivIcon: ImageView = view.findViewById(R.id.ivRowIcon)
            private val tvTitle: TextView = view.findViewById(R.id.tvRowTitle)
            private val tvSubtitle: TextView = view.findViewById(R.id.tvRowSubtitle)
            private val ivTrailing: ImageView = view.findViewById(R.id.ivRowTrailing)

            fun bind(row: PickerRow) {
                val density = itemView.resources.displayMetrics.density
                tvTitle.text = row.title
                tvSubtitle.visibility = View.GONE
                ivTrailing.visibility = View.GONE
                ivIcon.clearColorFilter()

                val isIndentedChild = row is PickerRow.ActivityRow || row is PickerRow.ExternalShortcutConfigRow
                val startPad = if (isIndentedChild) (44 * density).toInt() else (20 * density).toInt()
                itemView.setPadding(startPad, itemView.paddingTop, itemView.paddingRight, itemView.paddingBottom)

                when (row) {
                    is PickerRow.NovaRow -> {
                        ivIcon.setImageResource(row.iconRes)
                        if (row.tintIcon) {
                            ivIcon.setColorFilter(ContextCompat.getColor(itemView.context, R.color.nova_icon_tint_brown))
                        } else {
                            ivIcon.setColorFilter(Color.GRAY)
                        }
                    }
                    is PickerRow.AppRow -> {
                        ivIcon.setImageDrawable(row.appInfo.icon)
                        if (!row.subtitle.isNullOrBlank()) {
                            tvSubtitle.visibility = View.VISIBLE
                            tvSubtitle.text = row.subtitle
                        }
                    }
                    is PickerRow.ShortcutGroupRow -> {
                        ivIcon.setImageDrawable(row.group.icon)
                        if (row.childCount > 1 || row.group.children.firstOrNull() is PickerRow.ActivityRow) {
                            tvSubtitle.visibility = View.VISIBLE
                            tvSubtitle.text = "${row.childCount} items"
                            ivTrailing.visibility = View.VISIBLE
                            ivTrailing.setImageResource(
                                if (row.group.expanded) R.drawable.ic_chevron_up else R.drawable.ic_chevron_down
                            )
                        }
                    }
                    is PickerRow.ActivityRow -> {
                        ivIcon.setImageDrawable(row.icon)
                        tvSubtitle.visibility = View.VISIBLE
                        tvSubtitle.text = row.className.substringAfterLast('.')
                    }
                    is PickerRow.ExternalShortcutConfigRow -> {
                        ivIcon.setImageDrawable(row.icon)
                        tvSubtitle.visibility = View.VISIBLE
                        tvSubtitle.text = "Shortcut activity"
                    }
                }
                itemView.setOnClickListener { onClick(row) }
            }
        }
    }

    companion object {
        private const val TAB_NOVA = 0
        private const val TAB_APPS = 1
        private const val TAB_SHORTCUTS = 2

        const val EXTRA_TITLE = "extra_picker_title"
        const val EXTRA_SLOT_KEY = "extra_slot_key"
        const val EXTRA_FOLDER_MODE = "extra_folder_mode"
        const val EXTRA_SELECTED_ACTION = "extra_selected_action"

        fun createIntent(
            context: Context,
            title: String,
            slotKey: String = "",
            folderMode: Boolean = false
        ): Intent {
            return Intent(context, ChooseActionActivity::class.java).apply {
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_SLOT_KEY, slotKey)
                putExtra(EXTRA_FOLDER_MODE, folderMode)
            }
        }
    }
}
