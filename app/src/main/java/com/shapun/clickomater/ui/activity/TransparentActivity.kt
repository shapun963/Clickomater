package com.shapun.clickomater.ui.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.shapun.clickomater.service.ClickomaterService
import java.io.File

class TransparentActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        finish()
        overridePendingTransition(0,0)
        ClickomaterService.dispatchGestures(this,
            File(intent.getStringExtra(ClickomaterService.KEY_DATA_PATH)!!)
        )
    }
}