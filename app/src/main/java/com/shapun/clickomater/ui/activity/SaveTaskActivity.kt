package com.shapun.clickomater.ui.activity

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.view.WindowCompat
import androidx.core.view.drawToBitmap
import androidx.core.widget.addTextChangedListener
import com.google.android.gms.ads.AdRequest
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.shapun.clickomater.Constants
import com.shapun.clickomater.databinding.ActivitySaveTaskBinding
import com.shapun.clickomater.model.TaskData
import com.shapun.clickomater.service.ClickomaterService
import com.shapun.clickomater.ui.fragment.EditTaskDelayDialogFragment
import com.shapun.clickomater.util.Utils
import com.shapun.clickomater.util.Utils.writeBitmap
import org.json.JSONArray
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.lang.Long.min
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import kotlin.io.path.writeText
import kotlin.math.max

class SaveTaskActivity : AppCompatActivity() {
    private var mImageUri: Uri? = null
    private lateinit var mJsonData: JSONArray
    private lateinit var binding: ActivitySaveTaskBinding
    private lateinit var projectList: MutableList<TaskData>
    private val mPickImage = registerForActivityResult(ActivityResultContracts.PickVisualMedia()){
        if(it != null){
            mImageUri = it
            binding.imgIcon.setImageURI(it)
            binding.imgRemove.visibility = View.VISIBLE
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //theme.applyStyle(com.google.android.material.R.style.Base_ThemeOverlay_Material3_BottomSheetDialog,true)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivitySaveTaskBinding.inflate(layoutInflater)
        val dataDir = File(cacheDir, "data").toPath()
        val imgDir = File(cacheDir, "img").toPath()
        if(!dataDir.exists())Files.createDirectories(dataDir)
        if(!imgDir.exists())Files.createDirectories(imgDir)
        val projectListFile = Constants.getTaskListPath(this)
        mImageUri = savedInstanceState?.getString("uri",null)?.let{Uri.parse(it)}
        binding.imgIcon.setImageURI(mImageUri)
        if(mImageUri == null) binding.imgRemove.visibility = View.GONE
        projectList = try {
            Gson().fromJson(
                FileReader(projectListFile.toFile()),
                object: TypeToken<MutableList<TaskData>>(){}.type
            )
        }catch (e: Exception){
            mutableListOf()
        }
        mJsonData = JSONArray(intent.getStringExtra(KEY_DATA))
        binding.root.setOnClickListener {
            finish()
        }
        binding.edittextName.addTextChangedListener { editable ->
            val text = editable.toString()
            if(text.trim().isBlank()){
                binding.textInputLayoutName.error = "Please enter name."
                binding.btnSave.isEnabled = false
            }else{
                binding.textInputLayoutName.error = null
                binding.btnSave.isEnabled = true
            }
            if(projectList.stream().anyMatch{it.name == text }){
                binding.textInputLayoutName.error =
                    "\"${text}\" is already used in other project."
            }else{
                binding.textInputLayoutName.error = null
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
        binding.cardViewImg.setOnClickListener {
            mPickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        binding.btnEditDelay.setOnClickListener {
            EditTaskDelayDialogFragment.newInstance(mJsonData.toString()).show(supportFragmentManager,"EditTaskDelayDialogFragment")
        }
        binding.btnSave.setOnClickListener {
            val uuid = UUID.randomUUID().toString()
            val bitmap = binding.cardViewImg.drawToBitmap()
            val imgPath = Paths.get(imgDir.absolutePathString(),"$uuid.png")
            imgPath.writeBitmap(bitmap)
            val dataPath = Paths.get(dataDir.absolutePathString(),"$uuid.json")
            if(binding.cbRemoveInitialDelay.isChecked){
                mJsonData.getJSONObject(0)?.put("delay",0)
            }
            when(binding.chipGroup.checkedChipIds[0]){
                binding.chip300ms.id->{
                    trimDelay(300L)
                }
                binding.chip500ms.id->{
                    trimDelay(500L)
                }
                binding.chip1000ms.id->{
                    trimDelay(1000L)
                }
            }
            dataPath.writeText(mJsonData.toString())
            projectList.add(0,
                TaskData(
                    uuid,
                    binding.edittextName.text.toString(),
                    dataPath.absolutePathString(),
                    imgPath.absolutePathString(),
                    System.currentTimeMillis()
                ))
            FileWriter(projectListFile.toFile()).use {
                GsonBuilder().setPrettyPrinting().create().toJson(projectList,it)
            }

            if(binding.cbShortcutWidget.isChecked){
                Utils.createShortcut(applicationContext,
                    uuid,
                    binding.edittextName.text.toString(),
                    BitmapFactory.decodeFile(imgPath.absolutePathString()),
                    Intent(applicationContext,TransparentActivity::class.java).also{
                        it.action = Intent.ACTION_VIEW
                        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        it.putExtra(ClickomaterService.KEY_DATA_PATH,dataPath.absolutePathString())
                    }
                )
                val shortcutInfo = ShortcutInfoCompat.Builder(this, uuid)
                    .setShortLabel(binding.edittextName.text.toString())
                    .setIcon(IconCompat.createWithBitmap(BitmapFactory.decodeFile(imgPath.absolutePathString())))
                    .setIntent(Intent(this,TransparentActivity::class.java).also{
                        it.action = Intent.ACTION_VIEW
                        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        it.putExtra(ClickomaterService.KEY_DATA_PATH,dataPath.absolutePathString())
                    })
                    .build()
                ShortcutManagerCompat.requestPinShortcut(this,shortcutInfo,null)
            }
            finish()
        }
        binding.chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if(checkedIds.first() == binding.chipCustom.id){
                EditTaskDelayDialogFragment.newInstance(mJsonData.toString()).show(supportFragmentManager,"EditTaskDelayDialogFragment")
            }
        }
        binding.imgRemove.setOnClickListener {
            mImageUri = null
            binding.imgIcon.setImageBitmap(null)
            it.visibility = View.GONE
        }
        BottomSheetBehavior.from(binding.root.getChildAt(0)).addBottomSheetCallback(object:
            BottomSheetBehavior.BottomSheetCallback(){
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if(newState == BottomSheetBehavior.STATE_HIDDEN){
                    finish()
                }
            }
            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        })
        if(Utils.isPremiumUser(this).not()) {
            val adRequest = AdRequest.Builder().build()
            binding.adView.loadAd(adRequest)
        }
        setContentView(binding.root)
    }

    private fun trimDelay(duration: Long){
        for (i in 0 until mJsonData.length()) {
            val delay = mJsonData.getJSONObject(i).getLong("delay")
            mJsonData.getJSONObject(i).put("delay", max(delay - duration, min(delay, 100L)))
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mImageUri?.let{outState.putString("uri",it.toString())}
    }

    fun updateData(data: JSONArray){
        mJsonData = data
    }

    companion object {
        private const val KEY_DATA = "data"
        fun newInstance(context: Context, data: String) =
            Intent(context, SaveTaskActivity::class.java).also {
                it.putExtra(KEY_DATA, data)
            }
    }
}