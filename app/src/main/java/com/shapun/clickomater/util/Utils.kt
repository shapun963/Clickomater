package com.shapun.clickomater.util

import android.content.ActivityNotFoundException
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.TypedValue
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.shapun.clickomater.Constants
import com.shapun.clickomater.R
import com.shapun.clickomater.service.ClickomaterService
import com.shapun.clickomater.ui.activity.TransparentActivity
import java.io.IOException
import java.lang.String.valueOf
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.outputStream

object Utils {

    fun toast(context: Context?, obj: Any?) {
        Toast.makeText(context, obj.toString(), Toast.LENGTH_SHORT).show()
    }

    fun openUrl(context: Context,url: String) {
        try {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(url)
                )
            )
        } catch (e: ActivityNotFoundException) {
            toast(context, "Missing apk to perform  action.")
        }
    }

    fun longToast(context: Context?, obj: Any?) {
        Toast.makeText(context, obj.toString(), Toast.LENGTH_LONG).show()
    }

    fun queryName(resolver: ContentResolver, uri: Uri): String {
        val returnCursor = resolver.query(uri, null, null, null, null)!!
        val nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        returnCursor.moveToFirst()
        val name = returnCursor.getString(nameIndex)
        returnCursor.close()
        return name
    }

    fun setPadding(view: View, value: Int) {
        view.setPadding(value, value, value, value)
    }

    fun copy(ctx: Context, uri: Uri, outputPath: Path) {
        try {
            ctx.contentResolver.openInputStream(uri)
                .use { `is` -> Files.copy(`is`, outputPath, StandardCopyOption.REPLACE_EXISTING) }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun copy(ctx: Context, inputPath: Path, uri: Uri) {
        try {
            ctx.contentResolver.openOutputStream(uri).use { os -> Files.copy(inputPath, os) }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun Path.writeBitmap(bitmap: Bitmap, format: Bitmap.CompressFormat =Bitmap.CompressFormat.PNG, quality: Int =100) {
        outputStream().use { out ->
            bitmap.compress(format, quality, out)
            out.flush()
        }
    }

    fun getColorPrimary(context: Context):Int{
        val value = TypedValue()
        context.theme.resolveAttribute(android.R.attr.colorPrimary,value,true)
        return value.data
    }

    fun dpToPx(context: Context, input: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            input.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }
    fun openApp(context: Context,packageName: String,openNewInstance: Boolean = true){
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (intent == null) {
            toast(context, "App Not Found")
        } else {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if(openNewInstance)intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            context.startActivity(intent)
        }
    }
    fun createShortcut(context: Context,uuid: String,label: String,icon: Bitmap,intent: Intent ){
        val shortcutInfo = ShortcutInfoCompat.Builder(context, uuid)
            .setShortLabel(label)
            .setIcon(IconCompat.createWithBitmap(icon))
            .setIntent(intent)
            .build()
        ShortcutManagerCompat.requestPinShortcut(context,shortcutInfo,null)
    }
    fun isPremiumUser(context: Context):Boolean{
        val preferences = context.getSharedPreferences(
            Constants.DEFAULT_PREFERENCE,
            Context.MODE_PRIVATE
        )
        return preferences.getBoolean(Constants.KEY_IS_PREMIUM, false)
    }
}