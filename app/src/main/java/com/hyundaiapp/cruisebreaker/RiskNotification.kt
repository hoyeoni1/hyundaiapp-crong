package com.hyundaiapp.cruisebreaker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.hyundaiapp.cruisebreaker.domain.Announcement

/**
 * 앱이 앞에 없을 때(내비가 화면을 차지할 때) 안내를 내비 위에 헤드업 알림으로 띄운다(11-05 장호연 요청).
 * 차량 경고 분류(`CATEGORY_CAR_WARNING`) + 중요도 HIGH라 시스템이 화면 위에 잠깐 보여 주고,
 * [DisengageRules.bannerMs]와 같은 시간이 지나면 스스로 사라진다. 운전과 직접 관련된 안내라 앱 개발 규정의 헤드업 제한(미디어 앱) 대상이 아니다.
 * 알림 권한이 없으면 띄우지 않는다(음성·S1 배너는 그대로).
 */
class RiskNotification(context: Context) {
    private val appContext = context.applicationContext
    private val manager = appContext.getSystemService(NotificationManager::class.java)

    init {
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "해제 다발 구간 안내", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "주행 중 500m 앞 해제 다발 구간을 내비 위에 알려요"
                // 음성은 TTS가 따로 읽으므로 알림음은 끈다
                setSound(null, null)
                enableVibration(false)
            },
        )
    }

    fun show(announcement: Announcement, timeoutMs: Long) {
        if (!canPost()) return
        val open = PendingIntent.getActivity(
            appContext, 0, Intent(appContext, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = Notification.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(announcement.bannerText)
            .setContentText("크루즈브레이커 · 해제율 ${(announcement.risk.rate * 100).toInt()}%")
            // car_* 분류는 시스템 권한(SEND_CATEGORY_CAR_NOTIFICATIONS)이 필요해 SecurityException(11-05 실측) → 내비게이션 분류
            .setCategory(Notification.CATEGORY_NAVIGATION)
            .setPriority(Notification.PRIORITY_HIGH)
            .setContentIntent(open)
            .setAutoCancel(true)
            .setTimeoutAfter(timeoutMs)
            .setOnlyAlertOnce(false)
            .build()
        // 알림이 막혀도 기록·음성은 계속돼야 하므로 실패는 로그만 남긴다
        runCatching { manager.notify(NOTIFICATION_ID, notification) }.onFailure { Log.w("CruiseRecord", "heads-up failed: $it") }
    }

    fun hide() = manager.cancel(NOTIFICATION_ID)

    private fun canPost(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            appContext.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private companion object {
        const val CHANNEL_ID = "risk-announce"
        const val NOTIFICATION_ID = 2
    }
}
