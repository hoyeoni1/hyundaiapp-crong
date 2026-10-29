package com.hyundaiapp.cruisebreaker

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

// 뼈대. 화면은 길민종이 교체한다
class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(TextView(this).apply {
            textSize = 28f
            setPadding(48, 48, 48, 48)
            text = getString(R.string.app_name)
        })
    }
}
