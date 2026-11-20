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
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.example.launcherai.R

class PaddingDialogFragment : DialogFragment() {

    private var onPaddingSavedListener: ((horizStep: Int, vertStep: Int) -> Unit)? = null

    private var horizontalStep = 0
    private var verticalStep = 0

    fun setOnPaddingSavedListener(listener: (horizStep: Int, vertStep: Int) -> Unit) {
        this.onPaddingSavedListener = listener
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
        val view = inflater.inflate(R.layout.dialog_padding, container, false)

        val prefs = requireContext().getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
        horizontalStep = prefs.getInt("key_desktop_padding_horizontal", 0)
        verticalStep = prefs.getInt("key_desktop_padding_vertical", 0)

        val preview = view.findViewById<PaddingPreviewView>(R.id.paddingPreviewView)
        val sbHoriz = view.findViewById<SeekBar>(R.id.sbPaddingHorizontal)
        val tvHorizVal = view.findViewById<TextView>(R.id.tvPaddingHorizontalValue)
        val sbVert = view.findViewById<SeekBar>(R.id.sbPaddingVertical)
        val tvVertVal = view.findViewById<TextView>(R.id.tvPaddingVerticalValue)

        val btnCancel = view.findViewById<Button>(R.id.btnPaddingCancel)
        val btnOk = view.findViewById<Button>(R.id.btnPaddingOk)

        fun stepToLabel(step: Int): String = when (step) {
            1 -> "Small"
            2 -> "Medium"
            3 -> "Large"
            else -> "None"
        }

        fun updateUI() {
            sbHoriz.progress = horizontalStep
            sbVert.progress = verticalStep
            tvHorizVal.text = stepToLabel(horizontalStep)
            tvVertVal.text = stepToLabel(verticalStep)
            preview.setPaddingSteps(horizontalStep, verticalStep)
        }

        updateUI()

        sbHoriz.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                horizontalStep = progress
                tvHorizVal.text = stepToLabel(horizontalStep)
                preview.setPaddingSteps(horizontalStep, verticalStep)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        sbVert.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                verticalStep = progress
                tvVertVal.text = stepToLabel(verticalStep)
                preview.setPaddingSteps(horizontalStep, verticalStep)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnCancel.setOnClickListener {
            dismiss()
        }

        btnOk.setOnClickListener {
            prefs.edit()
                .putInt("key_desktop_padding_horizontal", horizontalStep)
                .putInt("key_desktop_padding_vertical", verticalStep)
                .apply()
            onPaddingSavedListener?.invoke(horizontalStep, verticalStep)
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
        const val TAG = "PaddingDialogFragment"
        fun newInstance() = PaddingDialogFragment()
    }
}
