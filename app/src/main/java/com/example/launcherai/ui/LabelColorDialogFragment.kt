package com.example.launcherai.ui

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.DialogFragment
import com.example.launcherai.R

/**
 * Color picker dialog for desktop icon labels matching Nova Launcher 8.x.
 * Provides preset swatches, custom HEX input, and enforces Alpha = 255.
 */
class LabelColorDialogFragment : DialogFragment() {

    private var onColorSavedListener: ((color: Int) -> Unit)? = null
    private var selectedColor: Int = Color.WHITE

    fun setOnColorSavedListener(listener: (color: Int) -> Unit) {
        this.onColorSavedListener = listener
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
        val view = inflater.inflate(R.layout.dialog_label_color_picker, container, false)

        val prefs = requireContext().getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
        selectedColor = prefs.getInt("key_label_color", Color.WHITE)

        val tvPreviewLabel = view.findViewById<TextView>(R.id.tvDialogColorPreviewLabel)
        val etCustomHex = view.findViewById<EditText>(R.id.etCustomHex)
        val btnHexSet = view.findViewById<Button>(R.id.btnHexPreview)
        val btnDefault = view.findViewById<Button>(R.id.btnColorDefault)
        val btnCancel = view.findViewById<Button>(R.id.btnColorCancel)
        val btnOk = view.findViewById<Button>(R.id.btnColorOk)

        data class SwatchItem(
            val container: FrameLayout,
            val circle: View,
            val check: ImageView,
            val color: Int
        )

        val swatches = listOf(
            SwatchItem(
                view.findViewById(R.id.swatchWhite),
                view.findViewById(R.id.circleWhite),
                view.findViewById(R.id.checkWhite),
                0xFFFFFFFF.toInt()
            ),
            SwatchItem(
                view.findViewById(R.id.swatchCharcoal),
                view.findViewById(R.id.circleCharcoal),
                view.findViewById(R.id.checkCharcoal),
                0xFF212121.toInt()
            ),
            SwatchItem(
                view.findViewById(R.id.swatchGray),
                view.findViewById(R.id.circleGray),
                view.findViewById(R.id.checkGray),
                0xFFB0B0B0.toInt()
            ),
            SwatchItem(
                view.findViewById(R.id.swatchRed),
                view.findViewById(R.id.circleRed),
                view.findViewById(R.id.checkRed),
                0xFFF44336.toInt()
            ),
            SwatchItem(
                view.findViewById(R.id.swatchOrange),
                view.findViewById(R.id.circleOrange),
                view.findViewById(R.id.checkOrange),
                0xFFFF9800.toInt()
            ),
            SwatchItem(
                view.findViewById(R.id.swatchYellow),
                view.findViewById(R.id.circleYellow),
                view.findViewById(R.id.checkYellow),
                0xFFFFEB3B.toInt()
            ),
            SwatchItem(
                view.findViewById(R.id.swatchGreen),
                view.findViewById(R.id.circleGreen),
                view.findViewById(R.id.checkGreen),
                0xFF4CAF50.toInt()
            ),
            SwatchItem(
                view.findViewById(R.id.swatchBlue),
                view.findViewById(R.id.circleBlue),
                view.findViewById(R.id.checkBlue),
                0xFF2196F3.toInt()
            ),
            SwatchItem(
                view.findViewById(R.id.swatchPurple),
                view.findViewById(R.id.circlePurple),
                view.findViewById(R.id.checkPurple),
                0xFF9C27B0.toInt()
            ),
            SwatchItem(
                view.findViewById(R.id.swatchPink),
                view.findViewById(R.id.circlePink),
                view.findViewById(R.id.checkPink),
                0xFFE91E63.toInt()
            )
        )

        // Setup circle drawables with proper borders
        val density = resources.displayMetrics.density
        val strokePx = (1.5f * density).toInt()

        swatches.forEach { item ->
            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(item.color)
                setStroke(strokePx, 0x44000000.toInt())
            }
            item.circle.background = drawable
        }

        fun updateUI() {
            // Enforce Alpha = 255
            val enforcedColor = (selectedColor and 0x00FFFFFF) or 0xFF000000.toInt()
            selectedColor = enforcedColor

            // Update live preview label
            tvPreviewLabel.setTextColor(selectedColor)
            val isDark = ColorUtils.calculateLuminance(selectedColor) < 0.5
            if (isDark) {
                tvPreviewLabel.setShadowLayer(3f, 0f, 1f, 0x44FFFFFF.toInt())
            } else {
                tvPreviewLabel.setShadowLayer(4f, 0f, 2f, 0x88000000.toInt())
            }

            // Update swatch selection checks
            swatches.forEach { item ->
                val isSelected = (item.color and 0x00FFFFFF) == (selectedColor and 0x00FFFFFF)
                item.check.visibility = if (isSelected) View.VISIBLE else View.GONE
            }

            // Update hex text without triggering recursive text changed
            val hexString = String.format("%06X", selectedColor and 0x00FFFFFF)
            if (etCustomHex.text.toString().uppercase() != hexString) {
                etCustomHex.setText(hexString)
                etCustomHex.setSelection(hexString.length)
            }
        }

        swatches.forEach { item ->
            item.container.setOnClickListener {
                selectedColor = item.color
                updateUI()
            }
        }

        btnHexSet.setOnClickListener {
            val hexText = etCustomHex.text.toString().trim()
            try {
                val parsed = Integer.parseInt(hexText, 16)
                selectedColor = parsed or 0xFF000000.toInt()
                updateUI()
            } catch (e: Exception) {
                Toast.makeText(context, "Invalid Hex Color", Toast.LENGTH_SHORT).show()
            }
        }

        btnDefault.setOnClickListener {
            selectedColor = Color.WHITE
            updateUI()
        }

        btnCancel.setOnClickListener {
            dismiss()
        }

        btnOk.setOnClickListener {
            val finalColor = (selectedColor and 0x00FFFFFF) or 0xFF000000.toInt()
            prefs.edit().putInt("key_label_color", finalColor).apply()
            onColorSavedListener?.invoke(finalColor)
            dismiss()
        }

        updateUI()
        return view
    }

    companion object {
        const val TAG = "LabelColorDialogFragment"
        fun newInstance(): LabelColorDialogFragment = LabelColorDialogFragment()
    }
}
