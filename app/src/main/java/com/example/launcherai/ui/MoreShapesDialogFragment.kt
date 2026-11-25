package com.example.launcherai.ui

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.example.launcherai.R
import com.example.launcherai.icons.IconShapeMasker

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

    override fun onStart() {
        super.onStart()
        val width = (resources.displayMetrics.widthPixels * 0.92).toInt()
        dialog?.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val prefKey = arguments?.getString(ARG_PREF_KEY) ?: "key_icon_shape"
        val defaultShape = arguments?.getString(ARG_DEFAULT_SHAPE) ?: "Round"
        val prefs = requireContext().getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
        val currentShape = prefs.getString(prefKey, defaultShape) ?: defaultShape

        val activeColor = ContextCompat.getColor(requireContext(), R.color.nova_accent_orange)
        val inactiveColor = Color.parseColor("#555555")
        val activeTextColor = activeColor
        val inactiveTextColor = Color.parseColor("#231A24")
        val density = resources.displayMetrics.density
        val iconSizePx = (44 * density).toInt()

        val grid = view.findViewById<GridLayout>(R.id.gridMoreShapes)
        if (grid != null) {
            for (i in 0 until grid.childCount) {
                val child = grid.getChildAt(i) as? ViewGroup ?: continue
                val shapeTag = child.tag?.toString() ?: continue
                val isSelected = currentShape.equals(shapeTag, ignoreCase = true)

                val color = if (isSelected) activeColor else inactiveColor
                val textColor = if (isSelected) activeTextColor else inactiveTextColor

                val iv = child.getChildAt(0) as? ImageView
                iv?.setImageDrawable(IconShapeMasker.createShapeThumbnail(requireContext(), shapeTag, iconSizePx, color))

                val tv = child.getChildAt(1) as? TextView
                tv?.setTextColor(textColor)

                child.setOnClickListener {
                    prefs.edit().putString(prefKey, shapeTag).apply()
                    onShapeSelectedListener?.invoke(shapeTag)
                    dismiss()
                }
            }
        }
    }

    companion object {
        const val TAG = "MoreShapesDialogFragment"
        private const val ARG_PREF_KEY = "arg_pref_key"
        private const val ARG_DEFAULT_SHAPE = "arg_default_shape"

        fun newInstance(prefKey: String = "key_icon_shape", defaultShape: String = "Round") =
            MoreShapesDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PREF_KEY, prefKey)
                    putString(ARG_DEFAULT_SHAPE, defaultShape)
                }
            }
    }
}
