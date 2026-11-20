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
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.example.launcherai.R
import com.example.launcherai.data.repository.WorkspaceGridConfig

class DesktopGridDialogFragment : DialogFragment() {

    private var onGridSavedListener: ((cols: Int, rows: Int, subgrid: Boolean) -> Unit)? = null

    private var currentCols = 5
    private var currentRows = 6
    private var subgridEnabled = true

    fun setOnGridSavedListener(listener: (cols: Int, rows: Int, subgrid: Boolean) -> Unit) {
        this.onGridSavedListener = listener
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
        val view = inflater.inflate(R.layout.dialog_desktop_grid, container, false)

        val prefs = requireContext().getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
        currentCols = prefs.getInt("key_grid_cols", 5)
        currentRows = prefs.getInt("key_grid_rows", 6)
        subgridEnabled = prefs.getBoolean("key_subgrid_positioning", true)

        val gridPreview = view.findViewById<GridPreviewView>(R.id.gridPreviewView)
        val tvColsCount = view.findViewById<TextView>(R.id.tvColsCount)
        val btnColsMinus = view.findViewById<ImageButton>(R.id.btnColsMinus)
        val btnColsPlus = view.findViewById<ImageButton>(R.id.btnColsPlus)

        val tvRowsCount = view.findViewById<TextView>(R.id.tvRowsCount)
        val btnRowsMinus = view.findViewById<ImageButton>(R.id.btnRowsMinus)
        val btnRowsPlus = view.findViewById<ImageButton>(R.id.btnRowsPlus)

        val rowSubgrid = view.findViewById<LinearLayout>(R.id.rowSubgrid)
        val cbSubgrid = view.findViewById<CheckBox>(R.id.cbSubgrid)

        val btnCancel = view.findViewById<Button>(R.id.btnGridCancel)
        val btnOk = view.findViewById<Button>(R.id.btnGridOk)

        fun updateUI() {
            tvColsCount.text = currentCols.toString()
            tvRowsCount.text = currentRows.toString()
            gridPreview.setGrid(currentRows, currentCols)
            cbSubgrid.isChecked = subgridEnabled
        }

        updateUI()

        btnColsMinus.setOnClickListener {
            if (currentCols > WorkspaceGridConfig.MIN_COLS) {
                currentCols--
                updateUI()
            }
        }

        btnColsPlus.setOnClickListener {
            if (currentCols < WorkspaceGridConfig.MAX_COLS) {
                currentCols++
                updateUI()
            }
        }

        btnRowsMinus.setOnClickListener {
            if (currentRows > WorkspaceGridConfig.MIN_ROWS) {
                currentRows--
                updateUI()
            }
        }

        btnRowsPlus.setOnClickListener {
            if (currentRows < WorkspaceGridConfig.MAX_ROWS) {
                currentRows++
                updateUI()
            }
        }

        rowSubgrid.setOnClickListener {
            subgridEnabled = !subgridEnabled
            cbSubgrid.isChecked = subgridEnabled
        }

        cbSubgrid.setOnCheckedChangeListener { _, isChecked ->
            subgridEnabled = isChecked
        }

        btnCancel.setOnClickListener {
            dismiss()
        }

        btnOk.setOnClickListener {
            prefs.edit()
                .putBoolean("key_subgrid_positioning", subgridEnabled)
                .apply()
            onGridSavedListener?.invoke(currentCols, currentRows, subgridEnabled)
            dismiss()
        }

        return view
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.92).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    companion object {
        const val TAG = "DesktopGridDialogFragment"
        fun newInstance() = DesktopGridDialogFragment()
    }
}
