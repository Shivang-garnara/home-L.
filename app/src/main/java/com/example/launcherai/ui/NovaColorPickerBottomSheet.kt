package com.example.launcherai.ui

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import com.example.launcherai.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * Nova Launcher-parity Custom Color Picker BottomSheet (ColorPickerBottomSheet).
 * Provides Hue, Saturation, and Brightness gradient sliders, live preview swatch,
 * editable Hex input, quick palette swatches, and Default/Cancel/Apply actions.
 */
class NovaColorPickerBottomSheet : BottomSheetDialogFragment() {

    private var titleText: String = "Custom color"
    private var initialColor: Int = 0xFF202534.toInt()
    private var defaultColor: Int = 0xFF202534.toInt()
    private var onColorSelected: ((Int) -> Unit)? = null

    private val hsv = FloatArray(3)
    private var isUpdatingFromCode = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_color_picker, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Color.colorToHSV(initialColor or 0xFF000000.toInt(), hsv)

        val tvTitle = view.findViewById<TextView>(R.id.tvColorPickerTitle)
        val tvHexLabel = view.findViewById<TextView>(R.id.tvColorPickerHexLabel)
        val viewPreview = view.findViewById<View>(R.id.viewColorPreviewSwatch)
        val etHex = view.findViewById<EditText>(R.id.etColorHexInput)

        val trackHue = view.findViewById<View>(R.id.trackHue)
        val trackSat = view.findViewById<View>(R.id.trackSaturation)
        val trackBri = view.findViewById<View>(R.id.trackBrightness)

        val sbHue = view.findViewById<SeekBar>(R.id.sbHue)
        val sbSat = view.findViewById<SeekBar>(R.id.sbSaturation)
        val sbBri = view.findViewById<SeekBar>(R.id.sbBrightness)

        val layoutSwatches = view.findViewById<LinearLayout>(R.id.layoutQuickSwatches)
        val btnReset = view.findViewById<TextView>(R.id.btnColorReset)
        val btnCancel = view.findViewById<TextView>(R.id.btnColorCancel)
        val btnApply = view.findViewById<TextView>(R.id.btnColorApply)

        tvTitle.text = titleText
        val density = resources.displayMetrics.density
        val trackRadius = 7f * density

        val hueColors = IntArray(7) { i ->
            Color.HSVToColor(floatArrayOf(i * 60f, 1f, 1f))
        }
        trackHue.background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, hueColors).apply {
            cornerRadius = trackRadius
        }

        fun currentColor(): Int = Color.HSVToColor(hsv) or 0xFF000000.toInt()

        fun refreshAll(updateHexField: Boolean = true) {
            isUpdatingFromCode = true
            val col = currentColor()
            val hex = String.format("%06X", col and 0xFFFFFF)

            viewPreview.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(col)
                setStroke((1.5f * density).toInt(), 0x33000000)
            }
            tvHexLabel.text = "#$hex"
            if (updateHexField && etHex.text.toString() != hex) {
                etHex.setText(hex)
                etHex.setSelection(hex.length)
            }

            sbHue.progress = hsv[0].toInt().coerceIn(0, 360)
            sbSat.progress = (hsv[1] * 100f).toInt().coerceIn(0, 100)
            sbBri.progress = (hsv[2] * 100f).toInt().coerceIn(0, 100)

            val satStart = Color.HSVToColor(floatArrayOf(hsv[0], 0f, hsv[2]))
            val satEnd = Color.HSVToColor(floatArrayOf(hsv[0], 1f, hsv[2]))
            trackSat.background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(satStart, satEnd)
            ).apply { cornerRadius = trackRadius }

            val briStart = Color.BLACK
            val briEnd = Color.HSVToColor(floatArrayOf(hsv[0], hsv[1], 1f))
            trackBri.background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(briStart, briEnd)
            ).apply { cornerRadius = trackRadius }

            isUpdatingFromCode = false
        }

        sbHue.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && !isUpdatingFromCode) {
                    hsv[0] = progress.toFloat().coerceIn(0f, 360f)
                    refreshAll()
                }
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        sbSat.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && !isUpdatingFromCode) {
                    hsv[1] = (progress / 100f).coerceIn(0f, 1f)
                    refreshAll()
                }
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        sbBri.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && !isUpdatingFromCode) {
                    hsv[2] = (progress / 100f).coerceIn(0f, 1f)
                    refreshAll()
                }
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        etHex.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isUpdatingFromCode) return
                val raw = s?.toString()?.trim()?.removePrefix("#") ?: return
                if (raw.length == 6) {
                    try {
                        val parsed = Integer.parseInt(raw, 16) or 0xFF000000.toInt()
                        Color.colorToHSV(parsed, hsv)
                        refreshAll(updateHexField = false)
                    } catch (_: Exception) {
                    }
                }
            }
        })

        val quickPalette = intArrayOf(
            0xFFFFFFFF.toInt(),
            0xFF202534.toInt(),
            0xFF000000.toInt(),
            0xFFD54526.toInt(),
            0xFF2196F3.toInt(),
            0xFF4CAF50.toInt(),
            0xFF9C27B0.toInt(),
            0xFFFF9800.toInt()
        )
        val swatchSize = (28 * density).toInt()
        val swatchMargin = (4 * density).toInt()
        for (color in quickPalette) {
            val dot = View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(swatchSize, swatchSize).apply {
                    setMargins(swatchMargin, 0, swatchMargin, 0)
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(color)
                    setStroke((1 * density).toInt(), 0x33000000)
                }
                setOnClickListener {
                    Color.colorToHSV(color, hsv)
                    refreshAll()
                }
            }
            layoutSwatches.addView(dot)
        }

        btnReset.setOnClickListener {
            Color.colorToHSV(defaultColor or 0xFF000000.toInt(), hsv)
            refreshAll()
        }

        btnCancel.setOnClickListener { dismiss() }

        btnApply.setOnClickListener {
            onColorSelected?.invoke(currentColor())
            dismiss()
        }

        refreshAll()
    }

    companion object {
        const val TAG = "NovaColorPickerBottomSheet"

        fun newInstance(
            title: String = "Custom color",
            initialColor: Int,
            defaultColor: Int = initialColor,
            onColorSelected: (Int) -> Unit
        ): NovaColorPickerBottomSheet {
            return NovaColorPickerBottomSheet().apply {
                this.titleText = title
                this.initialColor = initialColor
                this.defaultColor = defaultColor
                this.onColorSelected = onColorSelected
            }
        }
    }
}
