package com.shapun.clickomater.ui.fragment

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.core.view.drawToBitmap
import androidx.core.widget.addTextChangedListener
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import com.shapun.clickomater.databinding.FragmentTaskInfoBinding
import com.shapun.clickomater.model.TaskData
import com.shapun.clickomater.service.ClickomaterService
import com.shapun.clickomater.ui.activity.MainActivity
import com.shapun.clickomater.ui.activity.TransparentActivity
import com.shapun.clickomater.util.Utils
import com.shapun.clickomater.util.Utils.writeBitmap
import org.json.JSONArray
import java.io.File
import java.nio.file.Paths
import kotlin.io.path.absolutePathString
import kotlin.io.path.readText
import kotlin.io.path.writeText

class TaskInfoDialog: BottomSheetDialogFragment() {

    private var mImageUri: Uri? = null
    private lateinit var mTaskData: TaskData
    private lateinit var binding: FragmentTaskInfoBinding
    private val mPickImage = registerForActivityResult(ActivityResultContracts.PickVisualMedia()){
        if(it != null){
            mImageUri = it
            binding.imgIcon.setImageURI(it)
            binding.imgRemove.visibility = View.VISIBLE
            binding.btnSave.visibility = View.VISIBLE
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTaskInfoBinding.inflate(layoutInflater, container, false)
        mTaskData =
            Gson().fromJson(requireArguments().getString(KEY_DATA)!!, TaskData::class.java)
        binding.edittextName.setText(mTaskData.name)
        binding.btnSave.visibility = View.GONE
        mImageUri = if(savedInstanceState == null){
            Uri.fromFile(File(mTaskData.imagePath))
        }else{
            savedInstanceState.getString("uri",null)?.let{Uri.parse(it)}
        }
        if(mImageUri==null)binding.imgRemove.visibility = View.GONE
        binding.imgIcon.setImageURI(mImageUri)
        binding.cardViewImg.setOnClickListener {
            mPickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        binding.edittextName.addTextChangedListener { editable ->
            val text = editable.toString()
            if (text == mTaskData.name && mImageUri == Uri.fromFile(File(mTaskData.imagePath))) {
                binding.btnSave.visibility = View.GONE
            } else {
                binding.btnSave.visibility = View.VISIBLE
            }
            if(text.trim().isBlank()){
                binding.textInputLayoutName.error = "Please enter name."
                binding.btnSave.isEnabled = false
            }else{
                binding.textInputLayoutName.error = null
                binding.btnSave.isEnabled = true
            }
            var imgTxt = ""
            text.split(" ").filter { it.trim().isNotEmpty() }
                .forEachIndexed { index, s ->
                    if (index > 1) return@forEachIndexed
                    imgTxt += s.first()
                }
            binding.tvIconText.text = imgTxt
        }
        binding.edittextName.text = binding.edittextName.text
        binding.btnCreateShortCut.setOnClickListener {
            Utils.createShortcut(requireContext(),
                mTaskData.uuid,
                mTaskData.name,
                BitmapFactory.decodeFile(mTaskData.imagePath),
                Intent(requireContext(),TransparentActivity::class.java).also{
                    it.action = Intent.ACTION_VIEW
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    it.putExtra(ClickomaterService.KEY_DATA_PATH,mTaskData.dataPath)
                }

            )
        }
        binding.btnEditDelay.setOnClickListener {
            EditTaskDelayDialogFragment.newInstance(Paths.get(mTaskData.dataPath).readText()).show(childFragmentManager,"EditTaskDelayDialogFragment")
        }
        binding.btnStart.setOnClickListener {
            ClickomaterService.dispatchGestures(requireContext(), File(mTaskData.dataPath))
            dismiss()
        }
        binding.btnDelete.setOnClickListener {
            (activity as MainActivity).deleteProject(mTaskData.uuid)
            dismiss()
        }
        binding.btnSave.setOnClickListener {
            mTaskData.name = binding.edittextName.text.toString()
            val bitmap = binding.cardViewImg.drawToBitmap()
            val imgPath = Paths.get(mTaskData.imagePath)
            imgPath.writeBitmap(bitmap)
            (activity as MainActivity).updateProject(mTaskData)
            dismiss()
        }
        binding.imgRemove.setOnClickListener {
            mImageUri = null
            binding.imgIcon.setImageBitmap(null)
            it.visibility = View.GONE
            if (it.toString() == mTaskData.name && mImageUri == null) {
                binding.btnSave.visibility = View.GONE
            } else {
                binding.btnSave.visibility = View.VISIBLE
            }
        }
        binding.btnRepeat.visibility = View.GONE
        /*
        binding.btnRepeat.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Repeat Count")
                .setView(NumberPicker(requireContext()).also {
                    it.maxValue = Integer.MAX_VALUE
                })
                .setPositiveButton("Ok"){

                }
        }
         */
        return binding.root
    }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mImageUri?.let{outState.putString("uri",it.toString())}
    }

    fun updateData(data: JSONArray) {
        val dataPath = Paths.get(mTaskData.dataPath)
        dataPath.writeText(data.toString())
    }

    companion object {
        const val KEY_DATA = "data"
        fun newInstance(taskData: TaskData) = TaskInfoDialog().also {
            it.arguments = bundleOf("data" to Gson().toJson(taskData))
        }
    }
}