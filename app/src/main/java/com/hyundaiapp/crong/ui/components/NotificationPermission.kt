package com.hyundaiapp.crong.ui.components

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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

data class NotificationPermissionState(val granted: Boolean, val openSettings: Boolean, val request: () -> Unit)

internal enum class NotificationAction { NONE, REQUEST, SETTINGS }

internal fun notificationAction(sdk: Int, granted: Boolean, requested: Boolean, showRationale: Boolean, inFlight: Boolean = false, unresolvedResults: Int = 0): NotificationAction = when {
    sdk < 33 || granted -> NotificationAction.NONE
    inFlight -> NotificationAction.REQUEST
    (requested || unresolvedResults >= 2) && !showRationale -> NotificationAction.SETTINGS
    else -> NotificationAction.REQUEST
}

/** 첫 창을 선택 없이 닫은 경우(false + rationale 없음)는 거부 이력으로 확정하지 않는다. */
internal fun notificationDecisionSeen(previous: Boolean, granted: Boolean, showRationale: Boolean): Boolean =
    previous || granted || showRationale

/** 창 닫기와 시스템의 즉시 거부는 구분할 수 없어 한 번은 재요청하고, 반복되면 설정 경로를 제공한다. */
internal fun notificationUnresolvedResults(previous: Int, granted: Boolean, showRationale: Boolean): Int =
    if (granted || showRationale) 0 else (previous + 1).coerceAtMost(2)

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
    var unresolvedResults by remember { mutableStateOf(prefs.getInt("notifications_unresolved_results", 0)) }
    var rationale by remember { mutableStateOf(needsRationale()) }
    var inFlight by rememberSaveable { mutableStateOf(false) }
    val refresh = {
        granted = isGranted()
        rationale = needsRationale()
        // 설정에서 허용한 결과도 반영해 지난 미확인 횟수가 다음 요청에 남지 않게 한다.
        if (granted && unresolvedResults != 0) {
            unresolvedResults = 0
            prefs.edit().putInt("notifications_unresolved_results", 0).apply()
        }
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        refresh()
        requested = notificationDecisionSeen(requested, granted, rationale)
        unresolvedResults = notificationUnresolvedResults(unresolvedResults, granted, rationale)
        prefs.edit().putBoolean("notifications_requested", requested)
            .putInt("notifications_unresolved_results", unresolvedResults).apply()
        inFlight = false
    }
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val action = notificationAction(Build.VERSION.SDK_INT, granted, requested, rationale, inFlight, unresolvedResults)
    return NotificationPermissionState(granted, action == NotificationAction.SETTINGS) {
        // 시스템 설정에서 바뀐 직후에도 최신 값으로 판단한다.
        if (!inFlight) when (notificationAction(Build.VERSION.SDK_INT, isGranted(), requested, needsRationale(), unresolvedResults = unresolvedResults)) {
            NotificationAction.NONE -> refresh()
            NotificationAction.REQUEST -> {
                inFlight = true
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
