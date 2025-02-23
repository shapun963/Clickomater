package com.shapun.clickomater.ui.fragment

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.shapun.clickomater.R
import com.shapun.clickomater.databinding.FragmentStartRecordingDialogBinding
import com.shapun.clickomater.service.ClickomaterService
import com.shapun.clickomater.util.Utils


class StartRecordingDialogFragment : BottomSheetDialogFragment() {
    lateinit var binding: FragmentStartRecordingDialogBinding
    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentStartRecordingDialogBinding.inflate(layoutInflater,container,false)

        binding.tvInfo.text = """Recording will start as soon as you click any of the button below.
            |Note: Default navigation buttons of mobile (home,back,recent) wont work you need to use buttons in floating window instead.""".trimMargin()
        binding.btnStartRecording.setOnClickListener {
            dismiss()
            ClickomaterService.startRecording(requireContext())
        }
        binding.linYtInfo.setOnClickListener{
            Utils.openUrl(requireContext(),"https://youtube.com/playlist?list=PLmPRwtpDPjJR3odRtLaUN1oHYd7F6fllr")
        }
        binding.btnGoHomeStartRecording.setOnClickListener {
            dismiss()
            ClickomaterService.startRecording(requireContext(),true)
        }
        return binding.root
    }

    companion object {
        @JvmStatic
        fun newInstance() = StartRecordingDialogFragment()
    }
}