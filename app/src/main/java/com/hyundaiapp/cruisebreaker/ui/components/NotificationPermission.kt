package com.hyundaiapp.cruisebreaker.ui.components

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

data class NotificationPermissionState(val granted: Boolean, val openSettings: Boolean, val request: () -> Unit)

internal enum class NotificationAction { NONE, REQUEST, SETTINGS }

internal fun notificationAction(sdk: Int, granted: Boolean, requested: Boolean, showRationale: Boolean): NotificationAction = when {
    sdk < 33 || granted -> NotificationAction.NONE
    requested && !showRationale -> NotificationAction.SETTINGS
    else -> NotificationAction.REQUEST
}

/** 자동 팝업 없음. 재요청이 안 되면 사용자가 누른 때에만 알림 설정을 연다. */
@Composable
fun rememberNotificationPermission(): NotificationPermissionState {
    val context = LocalContext.current
    val activity = context.findActivity()
    val lifecycleOwner = LocalLifecycleOwner.current
    val prefs = remember(context) { context.getSharedPreferences("ui_permissions", Context.MODE_PRIVATE) }
    val isGranted = {
        Build.VERSION.SDK_INT < 33 ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }
    val needsRationale = {
        Build.VERSION.SDK_INT >= 33 && activity?.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) == true
    }
    var granted by remember { mutableStateOf(isGranted()) }
    var requested by remember { mutableStateOf(prefs.getBoolean("notifications_requested", false)) }
    var rationale by remember { mutableStateOf(needsRationale()) }
    val refresh = { granted = isGranted(); rationale = needsRationale() }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh() }
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val action = notificationAction(Build.VERSION.SDK_INT, granted, requested, rationale)
    return NotificationPermissionState(granted, action == NotificationAction.SETTINGS) {
        // 시스템 설정에서 바뀐 직후에도 최신 값으로 판단한다.
        when (notificationAction(Build.VERSION.SDK_INT, isGranted(), requested, needsRationale())) {
            NotificationAction.NONE -> refresh()
            NotificationAction.REQUEST -> {
                requested = true
                prefs.edit().putBoolean("notifications_requested", true).apply()
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            NotificationAction.SETTINGS -> {
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    context.startActivity(intent)
                } catch (_: ActivityNotFoundException) {
                    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> if (baseContext === this) null else baseContext.findActivity()
    else -> null
}
