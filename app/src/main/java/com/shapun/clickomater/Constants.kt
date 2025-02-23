package com.shapun.clickomater

import android.content.Context
import java.io.File
import java.nio.file.Path

object Constants {
    const val KEY_IS_PREMIUM = "is_premium"
    const val DEFAULT_PREFERENCE = "default_preference"
    const val ONE_TIME_PREMIUM = "one_time_premium"
    private const val TASK_LIST_FILE_NAME = "task_list.json"

    fun getTaskListPath(context: Context): Path {
        return File(context.cacheDir, TASK_LIST_FILE_NAME ).toPath()
    }
}