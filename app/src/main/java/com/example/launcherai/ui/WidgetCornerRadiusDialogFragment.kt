package com.example.launcherai.ui

import android.app.Dialog
import android.content.Context
import android.graphics.Outline
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.Window
import android.widget.SeekBar
import androidx.fragment.app.DialogFragment
import com.example.launcherai.databinding.DialogWidgetCornerRadiusBinding

/**
 * Dialog for configuring Widget Corner Radius (0dp to 24dp) with live visual card preview.
 */
class WidgetCornerRadiusDialogFragment : DialogFragment() {

    private var _binding: DialogWidgetCornerRadiusBinding? = null
    private val binding get() = _binding!!

    private var currentRadiusDp: Int = 16
    private var onRadiusSavedListener: ((Int) -> Unit)? = null

    fun setOnRadiusSavedListener(listener: (Int) -> Unit) {
        this.onRadiusSavedListener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.apply {
            requestFeature(Window.FEATURE_NO_TITLE)
            setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogWidgetCornerRadiusBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireContext().getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
        currentRadiusDp = prefs.getInt("key_widget_corner_radius", 16)

        binding.sbWidgetRadius.progress = currentRadiusDp
        updateRadiusUI(currentRadiusDp)

        binding.sbWidgetRadius.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentRadiusDp = progress
                updateRadiusUI(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.btnRadiusMinus.setOnClickListener {
            if (currentRadiusDp > 0) {
                currentRadiusDp--
                binding.sbWidgetRadius.progress = currentRadiusDp
                updateRadiusUI(currentRadiusDp)
            }
        }

        binding.btnRadiusPlus.setOnClickListener {
            if (currentRadiusDp < 24) {
                currentRadiusDp++
                binding.sbWidgetRadius.progress = currentRadiusDp
                updateRadiusUI(currentRadiusDp)
            }
        }

        binding.btnRadiusCancel.setOnClickListener {
            dismiss()
        }

        binding.btnRadiusSave.setOnClickListener {
            prefs.edit().putInt("key_widget_corner_radius", currentRadiusDp).apply()
            onRadiusSavedListener?.invoke(currentRadiusDp)
            dismiss()
        }
    }

    private fun updateRadiusUI(radiusDp: Int) {
        binding.tvRadiusValue.text = "${radiusDp}dp"
        val radiusPx = radiusDp * resources.displayMetrics.density

        binding.previewWidgetCard.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, radiusPx)
            }
        }
        binding.previewWidgetCard.clipToOutline = radiusPx > 0f
        binding.previewWidgetCard.invalidateOutline()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.92).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "WidgetCornerRadiusDialogFragment"
        fun newInstance(): WidgetCornerRadiusDialogFragment = WidgetCornerRadiusDialogFragment()
    }
}
