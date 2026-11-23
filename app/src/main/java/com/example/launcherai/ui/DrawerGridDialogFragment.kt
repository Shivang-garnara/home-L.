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
 * Drawer Grid configuration dialog matching Nova Launcher 8.9.9 (drawer_grid).
 * Allows independent configuration of app drawer rows & columns with live GridPreviewView.
 */
class DrawerGridDialogFragment : DialogFragment() {

    private var onGridSavedListener: ((cols: Int, rows: Int) -> Unit)? = null

    private var currentCols = 5
    private var currentRows = 6

    fun setOnGridSavedListener(listener: (cols: Int, rows: Int) -> Unit) {
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
        val view = inflater.inflate(R.layout.dialog_drawer_grid, container, false)

        val prefs = requireContext().getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
        currentCols = prefs.getInt("key_drawer_grid_cols", 5)
        currentRows = prefs.getInt("key_drawer_grid_rows", 6)

        val gridPreview = view.findViewById<GridPreviewView>(R.id.gridPreviewView)
        val tvColsCount = view.findViewById<TextView>(R.id.tvColsCount)
        val btnColsMinus = view.findViewById<ImageButton>(R.id.btnColsMinus)
        val btnColsPlus = view.findViewById<ImageButton>(R.id.btnColsPlus)

        val tvRowsCount = view.findViewById<TextView>(R.id.tvRowsCount)
        val btnRowsMinus = view.findViewById<ImageButton>(R.id.btnRowsMinus)
        val btnRowsPlus = view.findViewById<ImageButton>(R.id.btnRowsPlus)

        val btnCancel = view.findViewById<Button>(R.id.btnGridCancel)
        val btnOk = view.findViewById<Button>(R.id.btnGridOk)

        fun updateUI() {
            tvColsCount.text = currentCols.toString()
            tvRowsCount.text = currentRows.toString()
            gridPreview.setGrid(currentRows, currentCols)
        }

        updateUI()

        btnColsMinus.setOnClickListener {
            if (currentCols > MIN_COLS) {
                currentCols--
                updateUI()
            }
        }

        btnColsPlus.setOnClickListener {
            if (currentCols < MAX_COLS) {
                currentCols++
                updateUI()
            }
        }

        btnRowsMinus.setOnClickListener {
            if (currentRows > MIN_ROWS) {
                currentRows--
                updateUI()
            }
        }

        btnRowsPlus.setOnClickListener {
            if (currentRows < MAX_ROWS) {
                currentRows++
                updateUI()
            }
        }

        btnCancel.setOnClickListener {
            dismiss()
        }

        btnOk.setOnClickListener {
            prefs.edit()
                .putInt("key_drawer_grid_cols", currentCols)
                .putInt("key_drawer_grid_rows", currentRows)
                .apply()
            onGridSavedListener?.invoke(currentCols, currentRows)
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
        const val TAG = "DrawerGridDialogFragment"
        const val MIN_COLS = 2
        const val MAX_COLS = 8
        const val MIN_ROWS = 2
        const val MAX_ROWS = 12

        fun newInstance() = DrawerGridDialogFragment()
    }
}
