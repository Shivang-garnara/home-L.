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
import com.example.launcherai.icons.DeviceIconMetrics
import com.example.launcherai.icons.IconShapeMasker
import com.example.launcherai.util.AppManager
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Production-grade multi-select app picker dialog to hide/unhide applications from App Drawer
 * matching Nova Launcher's Hide Apps setting.
 */
class HiddenAppsDialogFragment : DialogFragment() {

    private var initialHiddenPackages: Set<String> = emptySet()
    private val selectedPackages: MutableSet<String> = mutableSetOf()
    private var allApps: List<AppInfo> = emptyList()
    private var filteredApps: List<AppInfo> = emptyList()
    private var onSaveListener: ((Set<String>) -> Unit)? = null

    private lateinit var adapter: HideAppsAdapter
    private lateinit var tvCounter: TextView
    private lateinit var tvEmpty: TextView
    private lateinit var etSearch: EditText
    private lateinit var btnClearSearch: ImageView

    fun setInitialData(
        hiddenPackages: Set<String>,
        apps: List<AppInfo>? = null,
        onSave: (Set<String>) -> Unit
    ) {
        this.initialHiddenPackages = hiddenPackages
        this.selectedPackages.clear()
        this.selectedPackages.addAll(hiddenPackages)
        if (apps != null && apps.isNotEmpty()) {
            this.allApps = apps.sortedBy { it.label.lowercase(Locale.ROOT) }
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

        val rv = view.findViewById<RecyclerView>(R.id.rvHideApps)
        tvCounter = view.findViewById(R.id.tvHideAppsCounter)
        tvEmpty = view.findViewById(R.id.tvHideAppsEmpty)
        etSearch = view.findViewById(R.id.etHideAppsSearch)
        btnClearSearch = view.findViewById(R.id.btnClearHideAppsSearch)
        val btnSelectAll = view.findViewById<TextView>(R.id.btnSelectAllApps)
        val btnDeselectAll = view.findViewById<TextView>(R.id.btnDeselectAllApps)
        val btnCancel = view.findViewById<Button>(R.id.btnHideAppsCancel)
        val btnApply = view.findViewById<Button>(R.id.btnHideAppsApply)

        adapter = HideAppsAdapter(
            apps = filteredApps,
            selected = selectedPackages,
            onItemToggled = { pkg ->
                if (selectedPackages.contains(pkg)) {
                    selectedPackages.remove(pkg)
                } else {
                    selectedPackages.add(pkg)
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
                allApps = loaded.sortedBy { it.label.lowercase(Locale.ROOT) }
                filterApps(etSearch.text.toString())
            }
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString() ?: ""
                btnClearSearch.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
                filterApps(query)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnClearSearch.setOnClickListener {
            etSearch.setText("")
        }

        btnSelectAll.setOnClickListener {
            filteredApps.forEach { selectedPackages.add(it.packageName) }
            adapter.notifyDataSetChanged()
            updateCounter()
        }

        btnDeselectAll.setOnClickListener {
            filteredApps.forEach { selectedPackages.remove(it.packageName) }
            adapter.notifyDataSetChanged()
            updateCounter()
        }

        btnCancel.setOnClickListener {
            dismiss()
        }

        btnApply.setOnClickListener {
            val prefs = requireContext().getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
            prefs.edit().putStringSet("key_hidden_packages", selectedPackages).apply()
            onSaveListener?.invoke(selectedPackages)
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
        val count = selectedPackages.size
        tvCounter.text = if (count == 1) "1 hidden" else "$count hidden"
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.94).toInt(),
            (resources.displayMetrics.heightPixels * 0.85).toInt()
        )
    }

    private class HideAppsAdapter(
        private var apps: List<AppInfo>,
        private val selected: MutableSet<String>,
        private val onItemToggled: (String) -> Unit
    ) : RecyclerView.Adapter<HideAppsAdapter.ViewHolder>() {

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
            holder.tvLabel.text = app.label
            holder.tvPackage.text = app.packageName

            val isHidden = selected.contains(app.packageName)
            holder.cbHide.isChecked = isHidden

            // Mask icon nicely matching Nova Launcher shape
            val context = holder.itemView.context
            val prefs = context.getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
            val shape = prefs.getString("key_icon_shape", "Round") ?: "Round"
            val iconSizePx = (44f * context.resources.displayMetrics.density).toInt()
            val rawIcon = app.icon ?: androidx.core.content.ContextCompat.getDrawable(context, R.mipmap.ic_launcher)
            val masked = IconShapeMasker.createMaskedIcon(context, rawIcon, shape, 100, iconSizePx)
            holder.ivIcon.setImageDrawable(masked)

            holder.itemView.setOnClickListener {
                val newState = !holder.cbHide.isChecked
                holder.cbHide.isChecked = newState
                onItemToggled(app.packageName)
            }
        }

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val ivIcon: ImageView = view.findViewById(R.id.ivHideAppIcon)
            val tvLabel: TextView = view.findViewById(R.id.tvHideAppLabel)
            val tvPackage: TextView = view.findViewById(R.id.tvHideAppPackage)
            val cbHide: CheckBox = view.findViewById(R.id.cbHideApp)
        }
    }

    companion object {
        const val TAG = "HiddenAppsDialogFragment"

        fun newInstance(
            hiddenPackages: Set<String>,
            apps: List<AppInfo>? = null,
            onSave: (Set<String>) -> Unit
        ): HiddenAppsDialogFragment {
            return HiddenAppsDialogFragment().apply {
                setInitialData(hiddenPackages, apps, onSave)
            }
        }
    }
}
