package com.example.launcherai.ui

import android.content.Context
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog

/**
 * Shared Nova-style Create / Edit Drawer Folder dialog (DialogAddDrawerGroupFragment parity).
 * Reused by both App Drawer ("+ Folder") and DrawerGroupsActivity.
 */
object DrawerFolderDialogHelper {

    fun showFolderEditorDialog(
        context: Context,
        initialTitle: String = "",
        initialKeepInMainDrawer: Boolean = false,
        isEditing: Boolean = false,
        onSave: (title: String, keepInMainDrawer: Boolean) -> Unit,
        onDelete: (() -> Unit)? = null
    ) {
        val density = context.resources.displayMetrics.density
        val padH = (20 * density).toInt()
        val padV = (12 * density).toInt()

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padH, padV, padH, (4 * density).toInt())
        }

        val input = EditText(context).apply {
            hint = "Folder name"
            setText(initialTitle)
            setSelection(text.length)
            isSingleLine = true
        }
        container.addView(input)

        val cbKeepInMainDrawer = CheckBox(context).apply {
            text = "Keep apps in main app drawer"
            isChecked = initialKeepInMainDrawer
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (12 * density).toInt()
            }
            layoutParams = lp
        }
        container.addView(cbKeepInMainDrawer)

        val builder = AlertDialog.Builder(context)
            .setTitle(if (isEditing) "Edit folder" else "New drawer folder")
            .setView(container)
            .setPositiveButton(if (isEditing) "Save" else "Add") { _, _ ->
                val title = input.text.toString().trim().ifEmpty { "Folder" }
                onSave(title, cbKeepInMainDrawer.isChecked)
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
