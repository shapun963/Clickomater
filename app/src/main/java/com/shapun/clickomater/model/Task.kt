package com.shapun.clickomater.model

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

class Task() {

    private var mJsonArray = JSONArray()
    private var _mLastActionTime = -1L
    //lastActionValue should only be set when its first used
    var lastActionTime: Long
        get() = if (_mLastActionTime == -1L) {
            synchronized(this) {
                System.currentTimeMillis()
            }
        } else {
            _mLastActionTime
        }
        set(value) {
            _mLastActionTime = value
        }

    private constructor(jsonArray: JSONArray) : this() {
        mJsonArray = jsonArray
    }

    enum class Action(val value: Int) {
        ACTION_TOUCH(1),
        ACTION_BACK(2),
        ACTION_HOME(3),
        ACTION_RECENT(4),
        ACTION_OPEN_NOTIFICATION_PANEL(5),
        ACTION_OPEN_APP(6),
        ACTION_REPEAT_START(7),
        ACTION_REPEAT_END(8),
        ACTION_INTENT(9)
    }

    fun startRepeat(repeatCount: Int,delay: Long=0){
        //Delay should be zero as no delay is needed
        mJsonArray.put(JSONObject()
            .put("delay", System.currentTimeMillis() - lastActionTime - delay)
            .put("repeat_count",repeatCount)
            .put("action",Action.ACTION_REPEAT_START.value))
        lastActionTime = System.currentTimeMillis()
    }

    fun endRepeat(){
        addAction(Action.ACTION_REPEAT_END)
    }

    fun addBackAction(){
        addAction(Action.ACTION_BACK)
    }
    fun addHomeAction(){
        addAction(Action.ACTION_HOME)
    }
    fun addRecentAction(){
        addAction(Action.ACTION_RECENT)
    }

    fun addOpenNotificationPanelAction(){
        addAction(Action.ACTION_OPEN_NOTIFICATION_PANEL)
    }

    fun addOpenApp(packageName: String,delay: Long){
        mJsonArray.put(JSONObject()
            .put("delay", System.currentTimeMillis() - lastActionTime - delay)
            .put("action",Action.ACTION_OPEN_APP.value)
            .put("data",packageName)
        )
        lastActionTime = System.currentTimeMillis()
    }

    fun addTouchAction(data: List<SerializableStrokeData>,delay: Long=0) {
        Log.d(TAG, "jhh " +(System.currentTimeMillis() - lastActionTime - delay).toString())
        mJsonArray.put(JSONObject()
            .put("delay", System.currentTimeMillis() - lastActionTime - delay)
            .put("action",Action.ACTION_TOUCH.value)
            .put("data", JSONArray().apply {
                data.forEach{put(it.toJsonObject())}
            })
        )
        lastActionTime = System.currentTimeMillis()
    }

    private fun addAction(action: Action,delay: Long=0){
        mJsonArray.put(JSONObject()
            .put("delay", System.currentTimeMillis() - lastActionTime - delay)
            .put("action",action.value))
        lastActionTime = System.currentTimeMillis()
    }

    fun size() = mJsonArray.length()
    fun isEmpty() = size() == 0
    fun toJson()= mJsonArray.toString()

       companion object{
        const val TAG = "Task"
        fun fromJson(json: String): Task{
            return Task(JSONArray(json))
        }
    }
}