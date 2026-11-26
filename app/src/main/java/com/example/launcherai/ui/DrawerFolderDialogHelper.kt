package com.example.launcherai.ui

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.example.launcherai.R
import com.example.launcherai.data.model.AppInfo
import com.example.launcherai.data.model.SmartCategoryHelper
import com.example.launcherai.data.model.SmartFolderCategory

/**
 * Shared Nova-style Create / Edit Drawer Folder dialog (DialogAddDrawerGroupFragment + Smart Category presets).
 * Allows choosing a built-in default category preset (which auto-fills the folder name, pre-selects
 * matching installed apps, and optionally auto-adds future apps in that category) OR typing any custom name.
 */
object DrawerFolderDialogHelper {

    fun showFolderEditorDialog(
        context: Context,
        initialTitle: String = "",
        initialKeepInMainDrawer: Boolean = false,
        initialCategoryId: String? = null,
        initialAutoAddNewApps: Boolean = false,
        installedApps: List<AppInfo> = emptyList(),
        isEditing: Boolean = false,
        onSave: (title: String, keepInMainDrawer: Boolean, categoryId: String?, autoAddNewApps: Boolean) -> Unit,
        onDelete: (() -> Unit)? = null
    ) {
        val density = context.resources.displayMetrics.density
        val padH = (20 * density).toInt()
        val padV = (12 * density).toInt()

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padH, padV, padH, (4 * density).toInt())
        }

        val tvPresetHeader = TextView(context).apply {
            text = "Default category"
            textSize = 12f
            setTextColor(ContextCompat.getColor(context, R.color.nova_text_brown_secondary))
            setPadding(0, 0, 0, (6 * density).toInt())
        }
        container.addView(tvPresetHeader)

        var selectedCategoryId: String? = SmartFolderCategory.fromIdOrName(initialCategoryId)?.id
        var lastAutoFilledTitle: String? = null
        val nonFolderApps = installedApps.filter { !it.isFolder }

        val input = EditText(context).apply {
            hint = "Folder name"
            setText(initialTitle)
            setSelection(text.length)
            isSingleLine = true
        }

        val tvCategoryHint = TextView(context).apply {
            textSize = 12f
            setTextColor(ContextCompat.getColor(context, R.color.nova_text_brown_secondary))
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (6 * density).toInt()
            }
            layoutParams = lp
        }

        val cbAutoAddCategory = CheckBox(context).apply {
            text = "Auto-add future apps in this category"
            isChecked = if (isEditing) initialAutoAddNewApps else (selectedCategoryId != null)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (8 * density).toInt()
            }
            layoutParams = lp
        }

        val chipRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val chipScroll = HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = (10 * density).toInt()
            }
            layoutParams = lp
            addView(chipRow)
        }
        container.addView(chipScroll)

        fun updatePresetState() {
            val cat = SmartFolderCategory.fromIdOrName(selectedCategoryId)
            if (cat == null) {
                cbAutoAddCategory.visibility = View.GONE
                cbAutoAddCategory.isChecked = false
                tvCategoryHint.text = "Manual folder with any custom name and apps"
            } else {
                cbAutoAddCategory.visibility = View.VISIBLE
                if (nonFolderApps.isNotEmpty()) {
                    val count = nonFolderApps.count { SmartCategoryHelper.matchesCategory(it, cat.id) }
                    tvCategoryHint.text = if (isEditing) {
                        "$count installed ${cat.defaultTitle} apps match this category"
                    } else if (count > 0) {
                        "$count installed ${cat.defaultTitle} apps will be pre-selected"
                    } else {
                        "No installed ${cat.defaultTitle} apps yet — future installs can auto-add"
                    }
                } else {
                    tvCategoryHint.text = "${cat.defaultTitle} preset selected — matching apps will be pre-selected"
                }
            }
        }

        val chipViews = mutableListOf<Pair<String?, TextView>>()

        fun refreshChipStyles() {
            chipViews.forEach { (catId, chip) ->
                val isSelected = (catId == selectedCategoryId)
                chip.setBackgroundResource(if (isSelected) R.drawable.bg_nova_chip_active else R.drawable.bg_nova_chip_inactive)
                chip.setTextColor(
                    if (isSelected) Color.WHITE
                    else ContextCompat.getColor(context, R.color.nova_text_brown_primary)
                )
            }
        }

        fun addCategoryChip(catId: String?, label: String) {
            val chip = TextView(context).apply {
                text = label
                textSize = 12f
                gravity = Gravity.CENTER
                setPadding((12 * density).toInt(), (5 * density).toInt(), (12 * density).toInt(), (5 * density).toInt())
                isClickable = true
                isFocusable = true
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    (30 * density).toInt()
                ).apply {
                    marginEnd = (6 * density).toInt()
                }
                layoutParams = lp
                setOnClickListener {
                    selectedCategoryId = catId
                    val cat = SmartFolderCategory.fromIdOrName(catId)
                    val currentText = input.text.toString().trim()
                    if (cat != null) {
                        if (currentText.isEmpty() || currentText == lastAutoFilledTitle) {
                            input.setText(cat.defaultTitle)
                            input.setSelection(input.text.length)
                            lastAutoFilledTitle = cat.defaultTitle
                        }
                        if (!isEditing) {
                            cbAutoAddCategory.isChecked = true
                        }
                    } else {
                        if (!isEditing && currentText == lastAutoFilledTitle) {
                            input.setText("")
                            lastAutoFilledTitle = null
                        }
                    }
                    refreshChipStyles()
                    updatePresetState()
                }
            }
            chipViews.add(catId to chip)
            chipRow.addView(chip)
        }

        addCategoryChip(null, "Custom")
        for (cat in SmartFolderCategory.PRESET_CATEGORIES) {
            val matchCount = nonFolderApps.count { SmartCategoryHelper.matchesCategory(it, cat.id) }
            val chipLabel = if (matchCount > 0) "${cat.defaultTitle} ($matchCount)" else cat.defaultTitle
            addCategoryChip(cat.id, chipLabel)
        }

        refreshChipStyles()
        updatePresetState()

        container.addView(input)
        container.addView(tvCategoryHint)
        container.addView(cbAutoAddCategory)

        val cbKeepInMainDrawer = CheckBox(context).apply {
            text = "Keep apps in main app drawer"
            isChecked = initialKeepInMainDrawer
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (8 * density).toInt()
            }
            layoutParams = lp
        }
        container.addView(cbKeepInMainDrawer)

        val builder = AlertDialog.Builder(context)
            .setTitle(if (isEditing) "Edit folder" else "New drawer folder")
            .setView(container)
            .setPositiveButton(if (isEditing) "Save" else "Add") { _, _ ->
                val chosenCat = SmartFolderCategory.fromIdOrName(selectedCategoryId)
                val defaultFallback = chosenCat?.defaultTitle ?: "Folder"
                val title = input.text.toString().trim().ifEmpty { defaultFallback }
                val autoAdd = chosenCat != null && cbAutoAddCategory.isChecked
                onSave(title, cbKeepInMainDrawer.isChecked, chosenCat?.id, autoAdd)
            }
            .setNegativeButton("Cancel", null)

        if (isEditing && onDelete != null) {
            builder.setNeutralButton("Delete") { _, _ ->
                onDelete()
            }
        }

        builder.show()
    }
}
