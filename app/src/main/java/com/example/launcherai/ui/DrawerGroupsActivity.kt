package com.example.launcherai.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.launcherai.R
import com.example.launcherai.data.model.AppInfo
import com.example.launcherai.data.model.DrawerFolderHelper
import com.example.launcherai.data.model.SmartCategoryHelper
import com.example.launcherai.icons.FolderIconRenderer
import kotlinx.coroutines.launch

/**
 * Manage App Drawer Folders (Nova Launcher's DrawerTabSetupActivity / "Drawer groups" parity).
 * Allows creating folders in the app drawer, picking apps with checkboxes, toggling whether member apps
 * stay in the main drawer, renaming, sorting, and deleting drawer folders.
 */
class DrawerGroupsActivity : AppCompatActivity() {

    private val viewModel: WorkspaceViewModel by viewModels {
        WorkspaceViewModel.Factory(application)
    }

    private lateinit var rvGroups: RecyclerView
    private lateinit var emptyLayout: View
    private lateinit var tvCount: TextView
    private lateinit var adapter: DrawerGroupAdapter

    private var currentFolders: List<WorkspaceFolderResolvedItem> = emptyList()
    private var currentInstalledApps: List<AppInfo> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_NovaLight)
        super.onCreate(savedInstanceState)
        SystemBarHelper.applySystemBars(this, isLightTheme = true, transparentBars = false)
        setContentView(R.layout.activity_drawer_groups)

        SystemBarHelper.applyEdgeToEdgeInsets(
            insetsRootView = findViewById(android.R.id.content),
            topInsetView = findViewById(R.id.headerContainer),
            bottomInsetView = null
        )

        rvGroups = findViewById(R.id.rvDrawerGroups)
        emptyLayout = findViewById(R.id.layoutDrawerGroupsEmpty)
        tvCount = findViewById(R.id.tvDrawerFoldersCount)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<View>(R.id.btnAddDrawerFolder).setOnClickListener {
            showCreateDrawerFolderDialog()
        }

        adapter = DrawerGroupAdapter(
            onFolderClick = { folder -> showSelectAppsPicker(folder) },
            onMenuClick = { folder, anchor -> showFolderOptionsMenu(folder, anchor) }
        )
        rvGroups.layoutManager = LinearLayoutManager(this)
        rvGroups.adapter = adapter

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.installedApps.collect { apps ->
                        currentInstalledApps = apps
                    }
                }
                launch {
                    viewModel.drawerFolders.collect { folders ->
                        currentFolders = folders
                        adapter.submitList(folders)
                        emptyLayout.visibility = if (folders.isEmpty()) View.VISIBLE else View.GONE
                        tvCount.text = if (folders.size == 1) "1 folder" else "${folders.size} folders"
                    }
                }
            }
        }
    }

    private fun showCreateDrawerFolderDialog() {
        DrawerFolderDialogHelper.showFolderEditorDialog(
            context = this,
            initialTitle = "",
            initialKeepInMainDrawer = false,
            installedApps = currentInstalledApps,
            isEditing = false,
            onSave = { title, keepInMain, categoryId, autoAddNewApps ->
                val preselectedKeys = if (categoryId != null) {
                    currentInstalledApps
                        .filter { !it.isFolder && SmartCategoryHelper.matchesCategory(it, categoryId) }
                        .map { DrawerFolderHelper.componentKey(it.packageName, it.activityName) }
                        .toSet()
                } else {
                    emptySet()
                }
                viewModel.createDrawerFolder(
                    title = title,
                    keepAppsInMainDrawer = keepInMain,
                    initialApps = emptyList(),
                    categoryId = categoryId,
                    autoAddNewApps = autoAddNewApps
                ) { newFolderId ->
                    runOnUiThread {
                        val dialog = FolderAppsPickerDialogFragment.newInstance(
                            title = title,
                            preselectedKeys = preselectedKeys,
                            apps = currentInstalledApps
                        ) { chosen ->
                            viewModel.setDrawerFolderApps(newFolderId, chosen)
                        }
                        dialog.show(supportFragmentManager, FolderAppsPickerDialogFragment.TAG)
                    }
                }
            }
        )
    }

    private fun showEditDrawerFolderDialog(folder: WorkspaceFolderResolvedItem) {
        val keepInMain = folder.entity.cellX == 1
        val initialCategoryId = SmartCategoryHelper.decodeCategoryId(folder.entity.packageName)
        val initialAutoAdd = SmartCategoryHelper.isAutoAddEnabled(folder.entity)
        DrawerFolderDialogHelper.showFolderEditorDialog(
            context = this,
            initialTitle = folder.title,
            initialKeepInMainDrawer = keepInMain,
            initialCategoryId = initialCategoryId,
            initialAutoAddNewApps = initialAutoAdd,
            installedApps = currentInstalledApps,
            isEditing = true,
            onSave = { newTitle, newKeepInMain, newCategoryId, newAutoAdd ->
                viewModel.updateDrawerFolder(
                    folderId = folder.entity.id,
                    title = newTitle,
                    keepAppsInMainDrawer = newKeepInMain,
                    categoryId = newCategoryId,
                    autoAddNewApps = newAutoAdd,
                    updateCategoryConfig = true
                )
            },
            onDelete = {
                confirmDeleteFolder(folder)
            }
        )
    }

    private fun showSelectAppsPicker(folder: WorkspaceFolderResolvedItem) {
        val preselected = folder.children.map {
            DrawerFolderHelper.componentKey(it.appInfo.packageName, it.appInfo.activityName)
        }.toSet()
        val dialog = FolderAppsPickerDialogFragment.newInstance(
            title = folder.title,
            preselectedKeys = preselected,
            apps = currentInstalledApps
        ) { chosen ->
            viewModel.setDrawerFolderApps(folder.entity.id, chosen)
        }
        dialog.show(supportFragmentManager, FolderAppsPickerDialogFragment.TAG)
    }

    private fun showFolderOptionsMenu(folder: WorkspaceFolderResolvedItem, anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menu.add(0, 1, 0, "Select apps")
        popup.menu.add(0, 2, 1, "Edit folder")
        popup.menu.add(0, 3, 2, "Sort alphabetically")
        popup.menu.add(0, 4, 3, "Delete folder")
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> {
                    showSelectAppsPicker(folder)
                    true
                }
                2 -> {
                    showEditDrawerFolderDialog(folder)
                    true
                }
                3 -> {
                    viewModel.sortFolderAppsAlphabetically(folder.entity.id)
                    true
                }
                4 -> {
                    confirmDeleteFolder(folder)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun confirmDeleteFolder(folder: WorkspaceFolderResolvedItem) {
        AlertDialog.Builder(this)
            .setTitle("Delete \"${folder.title}\"?")
            .setMessage("Apps inside this folder will stay installed and appear back in your app drawer.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteDrawerFolder(folder.entity.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private class DrawerGroupAdapter(
        private val onFolderClick: (WorkspaceFolderResolvedItem) -> Unit,
        private val onMenuClick: (WorkspaceFolderResolvedItem, View) -> Unit
    ) : RecyclerView.Adapter<DrawerGroupAdapter.ViewHolder>() {

        private var folders: List<WorkspaceFolderResolvedItem> = emptyList()

        fun submitList(list: List<WorkspaceFolderResolvedItem>) {
            folders = list
            notifyDataSetChanged()
        }

        override fun getItemCount(): Int = folders.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_drawer_group, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val folder = folders[position]
            val context = holder.itemView.context
            holder.tvTitle.text = folder.title

            val childDrawables = folder.children.mapNotNull { it.appInfo.icon }
            val sizePx = (44f * context.resources.displayMetrics.density).toInt()
            holder.ivIcon.setImageDrawable(
                FolderIconRenderer.renderFolderIconFromPrefs(context, childDrawables, targetSizePx = sizePx)
            )

            val categoryBadge = SmartCategoryHelper.decodeCategory(folder.entity.packageName)?.let { cat ->
                if (SmartCategoryHelper.isAutoAddEnabled(folder.entity)) "Smart (${cat.defaultTitle}) • " else "${cat.defaultTitle} • "
            }.orEmpty()
            val appsText = if (folder.children.isEmpty()) {
                "Empty"
            } else {
                val names = folder.children.take(4).joinToString(", ") { it.appInfo.label }
                if (folder.children.size > 4) "$names +${folder.children.size - 4}" else names
            }
            val visibilityHint = if (folder.entity.cellX == 1) " • Also in main drawer" else ""
            holder.tvSubtitle.text = "$categoryBadge$appsText$visibilityHint"

            holder.itemView.setOnClickListener { onFolderClick(folder) }
            holder.btnMenu.setOnClickListener { onMenuClick(folder, it) }
        }

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val ivIcon: ImageView = view.findViewById(R.id.ivGroupFolderIcon)
            val tvTitle: TextView = view.findViewById(R.id.tvGroupTitle)
            val tvSubtitle: TextView = view.findViewById(R.id.tvGroupSubtitle)
            val btnMenu: TextView = view.findViewById(R.id.btnGroupMenu)
        }
    }
}
