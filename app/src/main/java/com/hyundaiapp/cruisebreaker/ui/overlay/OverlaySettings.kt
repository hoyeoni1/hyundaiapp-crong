package com.hyundaiapp.cruisebreaker.ui.overlay

import android.content.Context
import android.content.Intent
import android.content.ActivityNotFoundException
import android.net.Uri
import android.provider.Settings
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.hyundaiapp.cruisebreaker.ui.components.Text
import com.hyundaiapp.cruisebreaker.ui.settings.ToggleSetting

/** AppPrefs와 같은 저장소이므로 동의 철회 시 표시 설정·좌표·영역도 함께 삭제된다. */
internal class OverlayPrefs(context: Context) {
    val storage = context.getSharedPreferences("cruisebreaker", Context.MODE_PRIVATE)
    var enabled: Boolean
        get() = storage.getBoolean("overlay_enabled", false)
        set(value) { storage.edit().putBoolean("overlay_enabled", value).apply() }
    val x get() = storage.getFloat("overlay_x", .85f).takeIf { it.isFinite() } ?: .85f
    val y get() = storage.getFloat("overlay_y", .1f).takeIf { it.isFinite() } ?: .1f
    fun position(x: Float, y: Float) { storage.edit().putFloat("overlay_x", x).putFloat("overlay_y", y).apply() }
    fun reset() { storage.edit().remove("overlay_x").remove("overlay_y").apply() }
}

internal fun openOverlayPermission(context: Context): Boolean {
    for (action in listOf(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Settings.ACTION_APPLICATION_DETAILS_SETTINGS)) {
        try {
            context.startActivity(Intent(action, Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return true
        } catch (_: ActivityNotFoundException) { /* Pleos: 앱 상세 설정으로 대체 */ }
        catch (_: SecurityException) { /* 다른 설정 경로 시도 */ }
    }
    return false
}

@Composable
internal fun OverlaySettingsRoute() {
    val context = LocalContext.current
    val prefs = remember(context) { OverlayPrefs(context) }
    var enabled by remember { mutableStateOf(prefs.enabled) }
    var granted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var unavailable by remember { mutableStateOf(false) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                enabled = prefs.enabled
                granted = Settings.canDrawOverlays(context)
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    OverlaySettings(enabled, granted, unavailable,
        onEnabledChange = { value ->
            enabled = value
            prefs.enabled = value
            if (value && !granted) unavailable = !openOverlayPermission(context)
        },
        onPermission = { unavailable = !openOverlayPermission(context) },
        onReset = prefs::reset,
    )
}

@Composable
internal fun OverlaySettings(
    enabled: Boolean,
    granted: Boolean,
    unavailable: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onPermission: () -> Unit,
    onReset: () -> Unit,
) {
    ToggleSetting("내비 위 목록 카드", enabled, onEnabledChange,
        "가까운 구간을 최대 3개 표시해요. 정차 중 길게 눌러 위치를 옮길 수 있어요.")
    if (enabled && !granted) {
        Text(if (unavailable) "Pleos 설정 → 앱 → 크루즈브레이커에서 ‘다른 앱 위에 표시’를 켜 주세요." else "‘다른 앱 위에 표시’ 권한을 허용한 뒤 앱으로 돌아와 주세요.")
        TextButton(onClick = onPermission, modifier = Modifier.heightIn(min = 64.dp)) { Text("표시 권한 설정") }
    }
    if (enabled) TextButton(onClick = onReset, modifier = Modifier.heightIn(min = 64.dp)) { Text("카드 위치 초기화") }
}
