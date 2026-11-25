package com.example.launcherai.ui

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.example.launcherai.R

/**
 * Interactive visual Folder Grid Picker Dialog matching Nova Launcher's FancyPrefGridView
 * (Columns horizontal stepper above visual block grid preview, Rows vertical stepper on right).
 */
class FolderGridDialogFragment : DialogFragment() {

    private var onGridSavedListener: ((cols: Int, rows: Int) -> Unit)? = null
    private var currentCols = 4
    private var currentRows = 3

    fun setOnGridSavedListener(listener: (cols: Int, rows: Int) -> Unit) {
        onGridSavedListener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.requestFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return dialog
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.90).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_folder_grid, container, false)
        val prefs = requireContext().getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
        currentCols = prefs.getInt(FolderSettingsActivity.KEY_FOLDER_GRID_COLS, 4).coerceIn(MIN_COLS, MAX_COLS)
        currentRows = prefs.getInt(FolderSettingsActivity.KEY_FOLDER_GRID_ROWS, 3).coerceIn(MIN_ROWS, MAX_ROWS)

        val preview = view.findViewById<FolderGridBlockPreviewView>(R.id.folderGridPreview)
        val tvCols = view.findViewById<TextView>(R.id.tvFolderColsCount)
        val tvRows = view.findViewById<TextView>(R.id.tvFolderRowsCount)

        fun refresh() {
            tvCols.text = currentCols.toString()
            tvRows.text = currentRows.toString()
            preview.setGrid(currentRows, currentCols)
        }
        refresh()

        view.findViewById<ImageButton>(R.id.btnFolderColsMinus).setOnClickListener {
            if (currentCols > MIN_COLS) {
                currentCols--
                refresh()
            }
        }
        view.findViewById<ImageButton>(R.id.btnFolderColsPlus).setOnClickListener {
            if (currentCols < MAX_COLS) {
                currentCols++
                refresh()
            }
        }
        view.findViewById<ImageButton>(R.id.btnFolderRowsMinus).setOnClickListener {
            if (currentRows > MIN_ROWS) {
                currentRows--
                refresh()
            }
        }
        view.findViewById<ImageButton>(R.id.btnFolderRowsPlus).setOnClickListener {
            if (currentRows < MAX_ROWS) {
                currentRows++
                refresh()
            }
        }
        view.findViewById<Button>(R.id.btnFolderGridCancel).setOnClickListener { dismiss() }
        view.findViewById<Button>(R.id.btnFolderGridOk).setOnClickListener {
            prefs.edit()
                .putInt(FolderSettingsActivity.KEY_FOLDER_GRID_COLS, currentCols)
                .putInt(FolderSettingsActivity.KEY_FOLDER_GRID_ROWS, currentRows)
                .apply()
            onGridSavedListener?.invoke(currentCols, currentRows)
            dismiss()
        }
        return view
    }

    companion object {
        const val TAG = "FolderGridDialogFragment"
        private const val MIN_COLS = 3
        private const val MAX_COLS = 6
        private const val MIN_ROWS = 2
        private const val MAX_ROWS = 7

        fun newInstance() = FolderGridDialogFragment()
    }
}
