package com.shapun.clickomater.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.BadTokenException
import android.view.accessibility.AccessibilityEvent
import com.shapun.clickomater.R
import com.shapun.clickomater.ui.activity.AccessibilityPermissionActivity
import com.shapun.clickomater.util.AccessibilityUtil
import org.json.JSONArray
import java.io.File
import java.io.FileReader


class ClickomaterService : AccessibilityService() {

    private var mVolumeDownClicked: Boolean = false
    private var mVolumeUpClicked: Boolean = false
    private var mTaskDispatcherService: TaskDispatcherService? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if(intent != null) {
            if (AccessibilityUtil.isServiceEnabled(this, ClickomaterService::class.java)) {
                //If accessibility service crashes OS will close accessibility service but this will not reflect in
                // normal accessibility permission check method call so this work around is used
                try {
                    val view = View(this)
                    getWindowManager().addView(
                        view,
                        WindowManager.LayoutParams(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY)
                    )
                    getWindowManager().removeView(view)
                } catch (e: BadTokenException) {
                    startActivity(Intent(this, AccessibilityPermissionActivity::class.java).also {
                        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                    return super.onStartCommand(intent, flags, startId)
                }
            } else {
                startActivity(Intent(this, AccessibilityPermissionActivity::class.java).also {
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
                return super.onStartCommand(intent, flags, startId)
            }
            if (ACTION_RECORD_TOUCH == intent.action) {
                ObserveTouchService(this).also {
                    it.startFromHomeScreen(intent.getBooleanExtra(KEY_START_FROM_HOME_PAGE, true))
                }.start()
            } else if (ACTION_DISPATCH_TOUCH == intent.action) {
                val dataFile = intent.getStringExtra(KEY_DATA_PATH)?.let { File(it) }
                FileReader(dataFile).use {
                    mTaskDispatcherService = TaskDispatcherService(this, JSONArray(it.readText()))
                    mTaskDispatcherService?.dispatch()
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        setTheme(R.style.Theme_Clickomater)
    }



    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        //TODO("Not yet implemented")
    }


    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if(event != null) {
            Log.d(TAG, event.toString())
            if (event.keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                    mVolumeUpClicked = event.action == KeyEvent.ACTION_DOWN
            }
            if (event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                mVolumeDownClicked = event.action == KeyEvent.ACTION_DOWN
            }
            if(mVolumeUpClicked and mVolumeDownClicked){
                mTaskDispatcherService?.stop();
            }
        }
        return super.onKeyEvent(event)
    }

    override fun onInterrupt() {
        TODO("Not yet implemented")
    }

    fun getWindowManager() = getSystemService(Context.WINDOW_SERVICE) as WindowManager

    companion object{
        const val ACTION_RECORD_TOUCH ="action_record_touch"
        const val ACTION_DISPATCH_TOUCH ="action_dispatch_touch"
        private const val KEY_START_FROM_HOME_PAGE = "start_from_home_page"
        const val KEY_DATA_PATH = "data_path"
        const val TAG = "ClickomaterService"
        fun startRecording(context: Context,startFromHomePage:Boolean=false){
            val intent = Intent(context,ClickomaterService::class.java)
            intent.action = ACTION_RECORD_TOUCH
            intent.putExtra(KEY_START_FROM_HOME_PAGE,startFromHomePage)
            context.startService(intent)
        }
        fun dispatchGestures(context: Context,dataFile: File){
            val intent = Intent(context,ClickomaterService::class.java)
            intent.action = ACTION_DISPATCH_TOUCH
            intent.putExtra(KEY_DATA_PATH,dataFile.toString())
            context.startService(intent)
        }
        fun dispatchGestureIntent(context: Context,dataFile: File): Intent{
            val intent = Intent(context,ClickomaterService::class.java)
            intent.action = ACTION_DISPATCH_TOUCH
            intent.putExtra(KEY_DATA_PATH,dataFile.toString())
            return intent
        }
    }
}