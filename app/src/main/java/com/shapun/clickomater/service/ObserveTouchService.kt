package com.shapun.clickomater.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.graphics.drawable.ColorDrawable
import android.provider.Settings
import android.util.Log
import android.view.*
import androidx.core.content.ContextCompat
import com.shapun.clickomater.R
import com.shapun.clickomater.databinding.CreateIntentBinding
import com.shapun.clickomater.databinding.RecordTaskFloatingViewBinding
import com.shapun.clickomater.databinding.RepeatCountPickerBinding
import com.shapun.clickomater.model.SerializablePath
import com.shapun.clickomater.model.SerializableStrokeData
import com.shapun.clickomater.model.Task
import com.shapun.clickomater.ui.activity.AppSelectorActivity
import com.shapun.clickomater.ui.activity.SaveTaskActivity
import com.shapun.clickomater.util.Utils
import java.util.*
import kotlin.concurrent.schedule

@SuppressLint("ClickableViewAccessibility")
class ObserveTouchService(private val service: ClickomaterService) {
    private var mRepeatMode = false
    private var downTime: Long = 0
    private var mStartFromHomeScreen = false
    private lateinit var controlsBinding: RecordTaskFloatingViewBinding
    private lateinit var controlsParams: WindowManager.LayoutParams
    private var x: Float = 0f
    private var y: Float = 0f
    private var pathMap = HashMap<Int, StrokeData>()
    private val view = View(service)
    private val windowParams: WindowManager.LayoutParams = WindowManager.LayoutParams()
    private val mStrokes: MutableList<SerializableStrokeData> =
        mutableListOf()
    private var mTaskSequence: Task? = null

    data class StrokeData(val path: SerializablePath, var startTime: Long, var endTime: Long)

    init {
        windowParams.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        windowParams.format = PixelFormat.TRANSLUCENT
        windowParams.width = WindowManager.LayoutParams.MATCH_PARENT
        windowParams.height = WindowManager.LayoutParams.MATCH_PARENT
        windowParams.flags = windowParams.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        view.setBackgroundColor(0x20008DCD)
        setListener()
        initControls()
    }

    fun startFromHomeScreen(value: Boolean){
        mStartFromHomeScreen = value
    }

    private fun dispatchGestures() {
        if(mStrokes.isEmpty())return
        try {
            service.dispatchGesture(
                GestureDescription.Builder()
                    .apply { mStrokes.forEach { addStroke(it.strokeData) } }
                    .build(),
                object : AccessibilityService.GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        super.onCompleted(gestureDescription)
                        startRecordingTouch()
                    }
                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        super.onCancelled(gestureDescription)
                        startRecordingTouch()
                        Utils.toast(service, "Some touches were interrupted")
                    }
                },
                null
            )
        }catch(e: Exception){
            startRecordingTouch()
            Utils.toast(service, "Some touches were interrupted")
            Log.d(TAG, "dispatchGestures: $e")
        }
    }

    private fun startRecordingTouch() {
        Log.i(TAG, "Recording started")
        view.isEnabled = true
        view.background = ColorDrawable(Utils.getColorPrimary(service)).also{it.alpha=20}
        windowParams.flags =
            windowParams.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
        windowManager.updateViewLayout(view, windowParams)
        controlsBinding.root.visibility = View.VISIBLE
    }

    private fun stopRecordingTouch() {
        Log.d(TAG, "Recording stopped")
        view.setBackgroundColor(0)
        view.isEnabled = false
        windowParams.flags = windowParams.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        windowManager.updateViewLayout(view, windowParams)
        controlsBinding.root.visibility = View.GONE
    }

    private fun setListener() {
        view.setOnTouchListener { _, event ->
            x = event.rawX
            y = event.rawY
            val id = event.getPointerId(event.actionIndex)
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downTime = System.currentTimeMillis()
                    pathMap.clear()
                    mStrokes.clear()
                    pathMap[id] = StrokeData(SerializablePath().apply { moveTo(x, y) }, 0, 0)
                }
                MotionEvent.ACTION_POINTER_DOWN -> {
                    pathMap[id] = StrokeData(
                        SerializablePath().apply { moveTo(x, y) },
                        System.currentTimeMillis() - downTime,
                        0
                    )
                }
                MotionEvent.ACTION_MOVE -> {
                    pathMap[id]?.path?.lineTo(x, y)
                }
                MotionEvent.ACTION_POINTER_UP -> {
                    pathMap[id]?.apply {
                        mStrokes.add(
                            SerializableStrokeData(
                                path,
                                startTime,
                                System.currentTimeMillis() - downTime
                            )
                        )
                    }
                }
                MotionEvent.ACTION_UP -> {
                    pathMap[id]?.apply {
                        mStrokes.add(
                            SerializableStrokeData(
                                path,
                                startTime,
                                System.currentTimeMillis() - downTime
                            )
                        )
                        mTaskSequence!!.addTouchAction(mStrokes)
                        stopRecordingTouch()
                        Timer().schedule(50){
                            ContextCompat.getMainExecutor(service).execute(::dispatchGestures)
                        }
                    }
                }
                MotionEvent.ACTION_CANCEL -> {
                    mStrokes.clear()
                }
            }
            true
        }
    }

    fun start() {
        mTaskSequence = Task()
        windowManager.addView(view, windowParams)
        windowManager.addView(controlsBinding.root, controlsParams)
        if(mStartFromHomeScreen){
            controlsBinding.imgHome.performClick()
        }
    }

    private fun initControls() {
        controlsBinding =
            RecordTaskFloatingViewBinding.inflate(service.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater)
        controlsParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        controlsParams.gravity = Gravity.CENTER
        controlsBinding.imgSave.setOnClickListener {
            if(mRepeatMode) {
                mTaskSequence?.endRepeat()
            }
            stopRecordingTouch()
            windowManager.removeView(view)
            windowManager.removeView(controlsBinding.root)
            if(mTaskSequence!!.isEmpty()){
                Utils.toast(service,"Task not saved as nothing was recorded.")
            }else {
                service.startActivity(
                    SaveTaskActivity.newInstance(
                        service,
                        mTaskSequence!!.toJson()
                    ).also {
                        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    })
            }
        }
        /*
        controlsBinding.imgExpandCollapse.setOnClickListener {
            if (controlsBinding.imgExpandCollapse.rotation == 0f) {
                controlsBinding.linMoreOptions.visibility = View.VISIBLE
                controlsBinding.imgExpandCollapse.animate().rotation(180f).start()
            } else {
                controlsBinding.linMoreOptions.visibility = View.GONE
                controlsBinding.imgExpandCollapse.animate().rotation(0f).start()
            }
        }
         */
        //controlsBinding.imgExpandCollapse.performClick()
        controlsBinding.imgOpenNotificationPanel.setOnClickListener {
            mTaskSequence!!.addOpenNotificationPanelAction()
            service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
        }
        controlsBinding.imgBack.setOnClickListener {
            mTaskSequence!!.addBackAction()
            service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
        }
        controlsBinding.imgHome.setOnClickListener {
            mTaskSequence!!.addHomeAction()
            service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
        }
        controlsBinding.imgRecent.setOnClickListener {
            mTaskSequence!!.addRecentAction()
            service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
        }

        controlsBinding.imgRepeat.setOnClickListener {
            if(mRepeatMode){
                mTaskSequence?.endRepeat()
                mRepeatMode = false
                controlsBinding.imgRepeat.setImageResource(R.drawable.ic_baseline_repeat_24)
            }else{
                val countPickerBinding = RepeatCountPickerBinding.inflate(LayoutInflater.from(service))
                windowManager.addView(countPickerBinding.root,windowParams)
                val startTime = System.currentTimeMillis()
                stopRecordingTouch()
                countPickerBinding.numberPicker.minValue = 2
                countPickerBinding.numberPicker.maxValue = 9999999
                countPickerBinding.btnSelect.setOnClickListener {
                    mTaskSequence?.startRepeat(countPickerBinding.numberPicker.value,System.currentTimeMillis()-startTime)
                    mRepeatMode = true
                    controlsBinding.imgRepeat.setImageResource(R.drawable.ic_baseline_repeat_on_24)
                    windowManager.removeView(countPickerBinding.root)
                    startRecordingTouch()
                }
                countPickerBinding.btnCancel.setOnClickListener {
                    startRecordingTouch()
                    windowManager.removeView(countPickerBinding.root)
                }
            }
        }
        controlsBinding.imgIntent.setOnClickListener {
            val intentBinding = CreateIntentBinding.inflate(LayoutInflater.from(service))
            val wp = WindowManager.LayoutParams()
            wp.copyFrom(windowParams)
            wp.flags = wp.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
            windowManager.addView(intentBinding.root,wp)
            val startTime = System.currentTimeMillis()
            stopRecordingTouch()
            intentBinding.btnSelect.setOnClickListener {
                Intent(intentBinding.autoCompleteEt.text.toString()).also {
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    service.startActivity(it)
                }
                windowManager.removeView(intentBinding.root)
                startRecordingTouch()
            }
            intentBinding.btnCancel.setOnClickListener {
                startRecordingTouch()
                windowManager.removeView(intentBinding.root)
            }
        }
        controlsBinding.imgOpenApp.setOnClickListener {
            val startTime = System.currentTimeMillis()
            stopRecordingTouch()
            val receiver = object: BroadcastReceiver(){
                override fun onReceive(context: Context, intent: Intent) {
                    service.unregisterReceiver(this)
                    val packageName = intent.getStringExtra(AppSelectorActivity.KEY_PACKAGE_NAME)
                    packageName?.let { it1 ->
                        mTaskSequence?.addOpenApp(it1,System.currentTimeMillis()-startTime)
                        Utils.openApp(service,it1)
                    }
                    startRecordingTouch()
                }
            }
            service.registerReceiver(receiver, IntentFilter().also{it.addAction(AppSelectorActivity.APP_SELECTED)})
            service.startActivity(Intent(service,AppSelectorActivity::class.java).also {
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
        val moveListener = object : View.OnTouchListener {
            private var downX = 0f
            private var downY = 0f
            private var initWinLocX = 0
            private var initWinLocY = 0
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = event.rawX
                        downY = event.rawY
                        initWinLocX = controlsParams.x
                        initWinLocY = controlsParams.y
                    }
                    MotionEvent.ACTION_MOVE -> {
                        controlsParams.x = initWinLocX + (event.rawX - downX).toInt()
                        controlsParams.y = initWinLocY + (event.rawY - downY).toInt()
                        windowManager.updateViewLayout(controlsBinding.root, controlsParams)
                    }
                    MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                        if (controlsParams.x == initWinLocX && controlsParams.y == initWinLocY) v.performClick()
                    }
                    else -> {}
                }
                return true
            }
        }
        controlsBinding.imgMove.setOnTouchListener(moveListener)
        controlsBinding.root.setOnTouchListener(moveListener)
    }

    private val windowManager get() = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    companion object{
        const val TAG = "ObserveTouchService"
    }
}