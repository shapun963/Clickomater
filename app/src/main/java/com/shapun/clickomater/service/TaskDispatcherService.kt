package com.shapun.clickomater.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityService.GestureResultCallback
import android.accessibilityservice.GestureDescription
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.*
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import com.google.android.material.button.MaterialButton
import com.shapun.clickomater.databinding.DispatchTouchInfoBinding
import com.shapun.clickomater.model.SerializableStrokeData
import com.shapun.clickomater.model.Task
import com.shapun.clickomater.util.Utils
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class TaskDispatcherService(
    private val service: AccessibilityService,
    private val taskSequence: JSONArray
) {

    private var btnStop: MaterialButton? = null
    private var executor: ExecutorService? = null
    private lateinit var binding: DispatchTouchInfoBinding
    private var currentIndex = 0

    fun dispatch() {
        val windowParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        binding = DispatchTouchInfoBinding.inflate(LayoutInflater.from(service))
        (binding.root.background as ColorDrawable).alpha = 255 / 4
        getWindowManager().addView(binding.root, windowParams)
        addStopButton()
        executor = Executors.newSingleThreadExecutor()
        executor?.execute {
            while (currentIndex < taskSequence.length()) {
                val action = taskSequence.getJSONObject(currentIndex)
                executeAction(action)
                currentIndex += 1
            }
            onComplete()
        }
    }

    private fun executeAction(action: JSONObject) {
        sleep(action.getLong("delay"))
            when (action.getInt("action")) {
                Task.Action.ACTION_TOUCH.value -> {
                    dispatchTouch(action)
                }
                Task.Action.ACTION_BACK.value -> {
                    service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
                }
                Task.Action.ACTION_HOME.value -> {
                    service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
                }
                Task.Action.ACTION_RECENT.value -> {
                    service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
                }
                Task.Action.ACTION_OPEN_NOTIFICATION_PANEL.value -> {
                    service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
                }
                Task.Action.ACTION_OPEN_APP.value -> {
                    Utils.openApp(service, action.getString("data"), true)
                }
                Task.Action.ACTION_REPEAT_START.value -> {
                    val repeatCount = action.getInt("repeat_count")
                    var endIndex = currentIndex + 1
                    repeat(repeatCount) {
                        var index = currentIndex + 1
                        var action1 = taskSequence.getJSONObject(index)
                        while (action1.getInt("action") != Task.Action.ACTION_REPEAT_END.value) {
                            if(Thread.currentThread().isInterrupted)break
                            executeAction(taskSequence.getJSONObject(index))
                            index += 1
                            action1 = taskSequence.getJSONObject(index)
                        }
                        endIndex = index
                    }
                    //move the postion to next of action_repeat_end
                    currentIndex = endIndex
                }
            }
    }

    fun stop(){
        executor!!.shutdownNow()
        ContextCompat.getMainExecutor(service).execute {
            try {
                getWindowManager().removeView(binding.root)
                getWindowManager().removeView(btnStop)
            }catch(e: Exception){
                Log.d(TAG,e.toString())
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    private fun addStopButton(){
        val windowParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        windowParams.gravity = Gravity.CENTER_HORIZONTAL or Gravity.TOP
        btnStop = MaterialButton(service)
        btnStop?.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT)
        btnStop?.text = "Stop"
        btnStop?.setTextColor(Color.WHITE)
        btnStop?.backgroundTintList = ColorStateList.valueOf(Color.RED)
        btnStop?.setPadding(Utils.dpToPx(service,8))
        getWindowManager().addView(btnStop,windowParams)
        btnStop?.setOnClickListener {
            stop()
        }
        btnStop?.setOnTouchListener(object : View.OnTouchListener {
            private var downX = 0f
            private var downY = 0f
            private var initWinLocX = 0
            private var initWinLocY = 0
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = event.rawX
                        downY = event.rawY
                        initWinLocX = windowParams.x
                        initWinLocY = windowParams.y
                    }
                    MotionEvent.ACTION_MOVE -> {
                        windowParams.x = initWinLocX + (event.rawX - downX).toInt()
                        windowParams.y = initWinLocY + (event.rawY - downY).toInt()
                        getWindowManager().updateViewLayout(btnStop, windowParams)
                    }
                    MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                        if (windowParams.x == initWinLocX && windowParams.y == initWinLocY) v.performClick()
                    }
                    else -> {}
                }
                return true
            }
        })
    }
    private fun dispatchTouch(action: JSONObject) {
        ContextCompat.getMainExecutor(service).execute {
            val strokeDataList = action.getJSONArray("data")
            val strokeDescBuilder = GestureDescription.Builder()
            for (strokeDataListIndex in 0 until strokeDataList.length()) {
                strokeDescBuilder.addStroke(
                    SerializableStrokeData.from(
                        strokeDataList.getJSONObject(strokeDataListIndex)
                    ).strokeData
                )
            }
            service.dispatchGesture(
                strokeDescBuilder.build(),
                object : GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        super.onCompleted(gestureDescription)
                    }
                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        super.onCancelled(gestureDescription)
                        Utils.toast(service, "Some clicks were not dispatched")
                    }
                },
                null
            )
        }
    }
    private fun sleep(millis:  Long){
        if(millis == 0L)return
        if(millis <0){
            Log.d(TAG,"Sleep time is negative")
            return
        }
        try {
            Thread.sleep(millis)
        } catch (e: InterruptedException) {
            stop()
        }
    }

    private fun onComplete() {
        ContextCompat.getMainExecutor(service).execute {
            Utils.toast(service, "Successfully completed")
            stop()
        }
    }

    private fun getWindowManager() =
        service.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    companion object {
        const val TAG = "TaskDispatcherService"
    }
}