package com.shapun.clickomater.model

import android.accessibilityservice.GestureDescription
import org.json.JSONObject

class SerializableStrokeData(
    val path: SerializablePath,
    private val startTime: Long,
    private val duration: Long
) {
    val strokeData get() = GestureDescription.StrokeDescription(path, startTime, duration)

    fun toJsonObject() = JSONObject().apply {
            put(KEY_PATH, path.toJsonArray())
            put(KEY_START_TIME,startTime)
            put(KEY_DURATION,duration)
    }

    companion object{
        const val KEY_PATH = "path"
        const val KEY_START_TIME = "start_time"
        const val KEY_DURATION = "duration"
        fun from(jsonObject: JSONObject): SerializableStrokeData{
            return SerializableStrokeData(
                SerializablePath.from(jsonObject.getJSONArray(KEY_PATH)),
                jsonObject.getLong(KEY_START_TIME),
                jsonObject.getLong(KEY_DURATION)
            )
        }
    }
}