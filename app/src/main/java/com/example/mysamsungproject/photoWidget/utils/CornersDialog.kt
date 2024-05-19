package com.example.mysamsungproject.photoWidget.utils

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.example.mysamsungproject.R

class CornersDialog: DialogFragment() {
    interface OnApplyListener {
        fun onApply(value: Int)
    }

    private var listener: OnApplyListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_corners, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val seekBar = view.findViewById<SeekBar>(R.id.seekBar)
        val applyButton = view.findViewById<Button>(R.id.button5)
        val cancelButton = view.findViewById<Button>(R.id.button4)

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val curCornerText = view.findViewById<TextView>(R.id.cur_corner_text)
                curCornerText.text = (progress*10).toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        applyButton.setOnClickListener {
            listener?.onApply(seekBar.progress)
            dismiss()
        }

        cancelButton.setOnClickListener {
            dismiss()
        }
    }

    fun setOnApplyListener(listener: OnApplyListener) {
        this.listener = listener
    }
}