package com.hyundaiapp.core.internal

import android.util.Log

/** `adb logcat -s HyCore` 로 core 로그만 본다. */
internal object Logs {
    private const val TAG = "HyCore"
    fun i(msg: String) = Log.i(TAG, msg)
    fun w(msg: String) = Log.w(TAG, msg)
}
