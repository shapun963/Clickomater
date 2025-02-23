package com.shapun.clickomater.model

import com.google.gson.annotations.SerializedName

data class TaskData(
    @SerializedName("uuid") val uuid: String,
    @SerializedName("name") var name: String,
    @SerializedName("data_path") val dataPath: String,
    @SerializedName("image_path") val imagePath: String,
    @SerializedName("created_time") val createdTime: Long
)
