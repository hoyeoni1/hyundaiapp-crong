package com.hyundaiapp.crong

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import com.hyundaiapp.crong.domain.Announcement

/**
 * 앱이 앞에 없을 때(내비가 화면을 차지할 때) 안내를 눈에 띄게 보여 주는 두 가지 길(11-05 장호연 요청 "길 가는데 내비 위에 팝업처럼").
 *
 * 1. [bringToFront]: 주행 화면(S1, 큰 배너)을 앞으로 올린다. Android 10+는 뒤에 있는 앱의 화면 띄우기를 막지만
 *    Pleos 에뮬레이터는 경고 로그("Background activity launch blocked")만 남기고 실제로는 올려 준다(11-05 실측).
 *    배너가 끝나면 [MainActivity]가 스스로 뒤로 가서 내비로 돌아간다.
 * 2. [show]: 1이 안 되는 환경을 위한 헤드업 알림(채널 중요도 HIGH, 분류 `navigation`, 10초 뒤 자동 삭제).
 *    `car_*` 분류는 시스템 권한(SEND_CATEGORY_CAR_NOTIFICATIONS)이 필요해 쓸 수 없다. 알림 권한이 없으면 띄우지 않는다.
 * 운전과 직접 관련된 안내라 앱 개발 규정의 헤드업 제한(미디어 앱 조항) 대상이 아니다. 음성은 [com.hyundaiapp.crong.domain.RiskSpeaker]가 따로 읽는다.
 */
class RiskNotification(context: Context) {
    private val appContext = context.applicationContext
    private val manager = appContext.getSystemService(NotificationManager::class.java)

    init {
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "해제 다발 구간 안내", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "주행 중 500m 앞 해제 다발 구간을 내비 위에 알려요"
                setSound(null, null)
                enableVibration(false)
            },
        )
    }

    /** 주행 화면을 앞으로 올린다. 플랫폼이 막으면 아무 일도 없다(예외 없음, 로그만) */
    fun bringToFront() {
        runCatching {
            appContext.startActivity(
                Intent(appContext, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            )
        }.onFailure { Log.w(TAG, "bring to front failed: $it") }
    }

    fun show(announcement: Announcement, timeoutMs: Long) {
        if (!canPost()) return
        val open = PendingIntent.getActivity(
            appContext, 0, Intent(appContext, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = Notification.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setLargeIcon(BitmapFactory.decodeResource(appContext.resources, android.R.drawable.ic_dialog_alert))
            .setContentTitle(announcement.bannerText)
            .setContentText("크롱 · 해제율 ${(announcement.risk.rate * 100).toInt()}%")
            .setCategory(Notification.CATEGORY_NAVIGATION)
            .setPriority(Notification.PRIORITY_HIGH)
            .setContentIntent(open)
            .setAutoCancel(true)
            .setTimeoutAfter(timeoutMs)
            .build()
        runCatching { manager.notify(NOTIFICATION_ID, notification) }.onFailure { Log.w(TAG, "heads-up failed: $it") }
    }

    fun hide() = manager.cancel(NOTIFICATION_ID)

    private fun canPost(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            appContext.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private companion object {
        const val TAG = "CruiseRecord"
        const val CHANNEL_ID = "risk-announce"
        const val NOTIFICATION_ID = 2
    }
}
