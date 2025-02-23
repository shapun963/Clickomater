package com.shapun.clickomater.ui.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.shapun.clickomater.R

class AccessibilityPermissionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MaterialAlertDialogBuilder(this)
            .setTitle("Accessibility permission needed")
            .setMessage(getString(R.string.accessibility_service_description))
            .setPositiveButton("Grant"){_,_->
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                finish();
            }
            .setNegativeButton("Cancel",null)
            .setOnDismissListener{finish()}
            .show()
    }
}