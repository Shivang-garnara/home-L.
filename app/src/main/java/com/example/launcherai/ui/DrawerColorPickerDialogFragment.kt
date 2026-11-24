package com.example.launcherai.ui

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
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
import androidx.fragment.app.DialogFragment
import com.example.launcherai.R

/**
 * Universal color picker dialog for App Drawer Background and Fast Scroll Accent Color
 * matching Nova Launcher's FancyPrefColorView.
 */
class DrawerColorPickerDialogFragment : DialogFragment() {

    private var dialogTitle: String = "Choose Color"
    private var initialColor: Int = 0xFFF7F2EE.toInt()
    private var selectedColor: Int = 0xFFF7F2EE.toInt()
    private var onColorSavedListener: ((color: Int) -> Unit)? = null

    fun setConfig(title: String, initialColor: Int, listener: (color: Int) -> Unit) {
        this.dialogTitle = title
        this.initialColor = initialColor
        this.selectedColor = initialColor
        this.onColorSavedListener = listener
    }

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
        val view = inflater.inflate(R.layout.dialog_drawer_color_picker, container, false)

        val tvTitle = view.findViewById<TextView>(R.id.tvDrawerColorDialogTitle)
        tvTitle.text = dialogTitle

        val viewPreview = view.findViewById<View>(R.id.viewColorPreview)
        val etCustomHex = view.findViewById<EditText>(R.id.etCustomHex)
        val btnHexSet = view.findViewById<Button>(R.id.btnHexPreview)
        val btnCancel = view.findViewById<Button>(R.id.btnColorCancel)
        val btnOk = view.findViewById<Button>(R.id.btnColorOk)

        data class Swatch(
            val container: FrameLayout,
            val circle: View,
            val check: ImageView,
            val color: Int
        )

        val swatches = listOf(
            Swatch(view.findViewById(R.id.swatchSurface), view.findViewById(R.id.circleSurface), view.findViewById(R.id.checkSurface), 0xFFF7F2EE.toInt()),
            Swatch(view.findViewById(R.id.swatchWhite), view.findViewById(R.id.circleWhite), view.findViewById(R.id.checkWhite), 0xFFFFFFFF.toInt()),
            Swatch(view.findViewById(R.id.swatchCharcoal), view.findViewById(R.id.circleCharcoal), view.findViewById(R.id.checkCharcoal), 0xFF2C2523.toInt()),
            Swatch(view.findViewById(R.id.swatchBlack), view.findViewById(R.id.circleBlack), view.findViewById(R.id.checkBlack), 0xFF000000.toInt()),
            Swatch(view.findViewById(R.id.swatchOrange), view.findViewById(R.id.circleOrange), view.findViewById(R.id.checkOrange), 0xFFD54526.toInt()),
            Swatch(view.findViewById(R.id.swatchRed), view.findViewById(R.id.circleRed), view.findViewById(R.id.checkRed), 0xFFF44336.toInt()),
            Swatch(view.findViewById(R.id.swatchGreen), view.findViewById(R.id.circleGreen), view.findViewById(R.id.checkGreen), 0xFF4CAF50.toInt()),
            Swatch(view.findViewById(R.id.swatchBlue), view.findViewById(R.id.circleBlue), view.findViewById(R.id.checkBlue), 0xFF2196F3.toInt()),
            Swatch(view.findViewById(R.id.swatchPurple), view.findViewById(R.id.circlePurple), view.findViewById(R.id.checkPurple), 0xFF9C27B0.toInt()),
            Swatch(view.findViewById(R.id.swatchGray), view.findViewById(R.id.circleGray), view.findViewById(R.id.checkGray), 0xFFB0B0B0.toInt())
        )

        fun updateUI() {
            // Update preview card
            val previewDrawable = GradientDrawable().apply {
                cornerRadius = 12f * resources.displayMetrics.density
                setColor(selectedColor or 0xFF000000.toInt())
                setStroke((1.5f * resources.displayMetrics.density).toInt(), 0x33000000.toInt())
            }
            viewPreview.background = previewDrawable

            // Update swatch checkmarks
            swatches.forEach { swatch ->
                val bg = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(swatch.color)
                    setStroke((1.5f * resources.displayMetrics.density).toInt(), 0x33000000.toInt())
                }
                swatch.circle.background = bg
                swatch.check.visibility = if ((swatch.color and 0x00FFFFFF) == (selectedColor and 0x00FFFFFF)) View.VISIBLE else View.GONE
            }

            // Update hex
            val hex = String.format("%06X", selectedColor and 0x00FFFFFF)
            if (etCustomHex.text.toString() != hex) {
                etCustomHex.setText(hex)
            }
        }

        swatches.forEach { swatch ->
            swatch.container.setOnClickListener {
                selectedColor = swatch.color
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

        btnCancel.setOnClickListener {
            dismiss()
        }

        btnOk.setOnClickListener {
            val finalColor = (selectedColor and 0x00FFFFFF) or 0xFF000000.toInt()
            onColorSavedListener?.invoke(finalColor)
            dismiss()
        }

        updateUI()
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
        const val TAG = "DrawerColorPickerDialogFragment"
        fun newInstance(
            title: String = "Choose Color",
            initialColor: Int = 0xFFF7F2EE.toInt(),
            listener: ((Int) -> Unit)? = null
        ): DrawerColorPickerDialogFragment {
            return DrawerColorPickerDialogFragment().apply {
                this.dialogTitle = title
                this.initialColor = initialColor
                this.selectedColor = initialColor
                this.onColorSavedListener = listener
            }
        }
    }
}
