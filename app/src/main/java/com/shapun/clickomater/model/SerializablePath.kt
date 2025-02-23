package com.shapun.clickomater.model

import android.graphics.Path
import com.google.gson.annotations.SerializedName
import org.json.JSONArray
import org.json.JSONObject

class SerializablePath: Path() {
     private val data = mutableListOf<Point>()

    override fun moveTo(x: Float, y:Float){
        super.moveTo(x,y)
        data.clear()
        data.add(Point(x,y))
    }
    override fun lineTo(x: Float, y:Float){
        super.lineTo(x,y)
        data.add(Point(x,y))
    }

   val  path get() = Path().apply {
       data.forEachIndexed {  index, it ->
           if(index == 0){
               moveTo(it.x,it.y)
           }else{
               lineTo(it.x,it.y)
           }
       }
   }

    fun toJsonArray() = JSONArray().apply {
            data.forEach{put(JSONObject().put(KEY_X,it.x).put(KEY_Y,it.y))}
    }

    data class Point(val x:Float,val y:Float)

    companion object{
        const val KEY_X = "x"
        const val KEY_Y = "y"
        fun from(jsonArray: JSONArray): SerializablePath{
            val serializablePath = SerializablePath()
            for (index in 0 until jsonArray.length()) {
                val point = jsonArray.getJSONObject(index)
                val x = point.getDouble(KEY_X).toFloat()
                val y = point.getDouble(KEY_Y).toFloat()
                if(index == 0){
                    serializablePath.moveTo(x,y)
                }else{
                    serializablePath.lineTo(x,y)
                }
            }
            return  serializablePath
        }
    }
}

