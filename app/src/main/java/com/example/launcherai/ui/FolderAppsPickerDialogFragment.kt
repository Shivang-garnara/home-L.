package com.example.launcherai.ui

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.launcherai.R
import com.example.launcherai.data.model.AppInfo
import com.example.launcherai.data.model.DrawerFolderHelper
import com.example.launcherai.icons.IconShapeMasker
import com.example.launcherai.util.AppManager
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Multi-select app picker dialog for App Drawer Folders (Nova GroupAppListActivity parity).
 * Reuses dialog_hide_apps / item_hide_app layout with component-level selection and live search.
 */
class FolderAppsPickerDialogFragment : DialogFragment() {

    private var folderTitle: String = "Folder"
    private val selectedKeys: MutableSet<String> = mutableSetOf()
    private var allApps: List<AppInfo> = emptyList()
    private var filteredApps: List<AppInfo> = emptyList()
    private var onSaveListener: ((List<AppInfo>) -> Unit)? = null

    private lateinit var adapter: FolderAppsAdapter
    private lateinit var tvCounter: TextView
    private lateinit var tvEmpty: TextView
    private lateinit var etSearch: EditText
    private lateinit var btnClearSearch: ImageView

    fun setInitialData(
        title: String,
        preselectedKeys: Set<String>,
        apps: List<AppInfo>? = null,
        onSave: (List<AppInfo>) -> Unit
    ) {
        this.folderTitle = title
        this.selectedKeys.clear()
        this.selectedKeys.addAll(preselectedKeys)
        if (!apps.isNullOrEmpty()) {
            this.allApps = apps.filter { !it.isFolder }.sortedBy { it.label.lowercase(Locale.ROOT) }
            this.filteredApps = this.allApps
        }
        this.onSaveListener = onSave
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.requestFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_hide_apps, container, false)

        view.findViewById<TextView>(R.id.tvHideAppsDialogTitle)?.text = folderTitle
        view.findViewById<TextView>(R.id.tvHideAppsDialogSubtitle)?.text =
            "Select applications to show inside \"$folderTitle\""

        val rv = view.findViewById<RecyclerView>(R.id.rvHideApps)
        tvCounter = view.findViewById(R.id.tvHideAppsCounter)
        tvEmpty = view.findViewById(R.id.tvHideAppsEmpty)
        etSearch = view.findViewById(R.id.etHideAppsSearch)
        btnClearSearch = view.findViewById(R.id.btnClearHideAppsSearch)
        val btnSelectAll = view.findViewById<TextView>(R.id.btnSelectAllApps)
        val btnDeselectAll = view.findViewById<TextView>(R.id.btnDeselectAllApps)
        val btnCancel = view.findViewById<Button>(R.id.btnHideAppsCancel)
        val btnApply = view.findViewById<Button>(R.id.btnHideAppsApply)

        adapter = FolderAppsAdapter(
            apps = filteredApps,
            selectedKeys = selectedKeys,
            onItemToggled = { key ->
                if (selectedKeys.contains(key)) {
                    selectedKeys.remove(key)
                } else {
                    selectedKeys.add(key)
                }
                updateCounter()
            }
        )

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        updateCounter()

        if (allApps.isEmpty()) {
            lifecycleScope.launch {
                val loaded = AppManager.getInstalledApps(requireContext())
                allApps = loaded.filter { !it.isFolder }.sortedBy { it.label.lowercase(Locale.ROOT) }
                filterApps(etSearch.text.toString())
            }
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString().orEmpty()
                btnClearSearch.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
                filterApps(query)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnClearSearch.setOnClickListener { etSearch.setText("") }

        btnSelectAll.setOnClickListener {
            filteredApps.forEach { selectedKeys.add(DrawerFolderHelper.componentKey(it.packageName, it.activityName)) }
            adapter.notifyDataSetChanged()
            updateCounter()
        }

        btnDeselectAll.setOnClickListener {
            filteredApps.forEach { selectedKeys.remove(DrawerFolderHelper.componentKey(it.packageName, it.activityName)) }
            adapter.notifyDataSetChanged()
            updateCounter()
        }

        btnCancel.setOnClickListener { dismiss() }

        btnApply.setOnClickListener {
            val chosen = allApps.filter { app ->
                DrawerFolderHelper.componentKey(app.packageName, app.activityName) in selectedKeys ||
                    app.packageName in selectedKeys
            }
            onSaveListener?.invoke(chosen)
            dismiss()
        }

        return view
    }

    private fun filterApps(query: String) {
        val trimmed = query.trim().lowercase(Locale.ROOT)
        filteredApps = if (trimmed.isEmpty()) {
            allApps
        } else {
            allApps.filter {
                it.label.lowercase(Locale.ROOT).contains(trimmed) ||
                    it.packageName.lowercase(Locale.ROOT).contains(trimmed)
            }
        }
        adapter.updateList(filteredApps)
        tvEmpty.visibility = if (filteredApps.isEmpty() && allApps.isNotEmpty()) View.VISIBLE else View.GONE
    }

    private fun updateCounter() {
        val count = selectedKeys.size
        tvCounter.text = if (count == 1) "1 app selected" else "$count apps selected"
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.94).toInt(),
            (resources.displayMetrics.heightPixels * 0.85).toInt()
        )
    }

    private class FolderAppsAdapter(
        private var apps: List<AppInfo>,
        private val selectedKeys: MutableSet<String>,
        private val onItemToggled: (String) -> Unit
    ) : RecyclerView.Adapter<FolderAppsAdapter.ViewHolder>() {

        fun updateList(newList: List<AppInfo>) {
            apps = newList
            notifyDataSetChanged()
        }

        override fun getItemCount(): Int = apps.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_hide_app, parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val app = apps[position]
            val key = DrawerFolderHelper.componentKey(app.packageName, app.activityName)
            holder.tvLabel.text = app.label
            holder.tvPackage.text = app.packageName

            val isChecked = selectedKeys.contains(key) || selectedKeys.contains(app.packageName)
            holder.cbApp.isChecked = isChecked

            val context = holder.itemView.context
            val prefs = context.getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
            val shape = prefs.getString("key_icon_shape", "Round") ?: "Round"
            val iconSizePx = (44f * context.resources.displayMetrics.density).toInt()
            val rawIcon = app.icon ?: androidx.core.content.ContextCompat.getDrawable(context, R.mipmap.ic_launcher)
            val masked = IconShapeMasker.createMaskedIcon(context, rawIcon, shape, 100, iconSizePx)
            holder.ivIcon.setImageDrawable(masked)

            holder.itemView.setOnClickListener {
                val nowChecked = !holder.cbApp.isChecked
                holder.cbApp.isChecked = nowChecked
                if (!nowChecked && selectedKeys.contains(app.packageName) && !selectedKeys.contains(key)) {
                    selectedKeys.remove(app.packageName)
                }
                onItemToggled(key)
            }
        }

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val ivIcon: ImageView = view.findViewById(R.id.ivHideAppIcon)
            val tvLabel: TextView = view.findViewById(R.id.tvHideAppLabel)
            val tvPackage: TextView = view.findViewById(R.id.tvHideAppPackage)
            val cbApp: CheckBox = view.findViewById(R.id.cbHideApp)
        }
    }

    companion object {
        const val TAG = "FolderAppsPickerDialog"

        fun newInstance(
            title: String,
            preselectedKeys: Set<String>,
            apps: List<AppInfo>? = null,
            onSave: (List<AppInfo>) -> Unit
        ): FolderAppsPickerDialogFragment {
            return FolderAppsPickerDialogFragment().apply {
                setInitialData(title, preselectedKeys, apps, onSave)
            }
        }
    }
}
