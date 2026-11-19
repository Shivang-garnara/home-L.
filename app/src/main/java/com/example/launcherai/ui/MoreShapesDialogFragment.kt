package com.example.launcherai.ui

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import androidx.fragment.app.DialogFragment
import com.example.launcherai.R

/**
 * Pale lavender modal dialog matching Nova Launcher reference 07.
 * Displays a 4-column grid of icon shape choices.
 */
class MoreShapesDialogFragment : DialogFragment() {

    var onShapeSelectedListener: ((String) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_more_shapes, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val grid = view.findViewById<GridLayout>(R.id.gridMoreShapes)
        if (grid != null) {
            for (i in 0 until grid.childCount) {
                val child = grid.getChildAt(i)
                val shapeTag = child.tag?.toString() ?: continue
                child.setOnClickListener {
                    val prefs = requireContext().getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
                    prefs.edit().putString("key_icon_shape", shapeTag).apply()
                    onShapeSelectedListener?.invoke(shapeTag)
                    dismiss()
                }
            }
        }
    }

    companion object {
        const val TAG = "MoreShapesDialogFragment"
        fun newInstance() = MoreShapesDialogFragment()
    }
}
