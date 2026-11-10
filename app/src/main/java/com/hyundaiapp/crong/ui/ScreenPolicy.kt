package com.hyundaiapp.crong.ui

import com.hyundaiapp.core.DriveState
import com.hyundaiapp.core.Gear

/** P가 확인된 동안만 정차 화면을 연다. 차속 권한이 없으면 기어로 판단한다. */
internal fun canShowParkedScreens(drive: DriveState): Boolean =
    drive.gear == Gear.PARK && (drive.speedKph == null || drive.speedKph == 0f)

/** 플랫폼이 정차 UI를 허용할 때만 연다. 기어 미수신은 허용 상태를 덮어쓰지 않는다. */
internal fun visibleScreen(requested: Screen, drive: DriveState, uxRestricted: Boolean?): Screen {
    val knownMoving = drive.gear in setOf(Gear.DRIVE, Gear.REVERSE, Gear.NEUTRAL) ||
        (drive.speedKph != null && drive.speedKph != 0f)
    return if (uxRestricted != false || knownMoving) Screen.DRIVING else requested
}

/**
 * 플랫폼 UX 제한 값을 [UX_RESTRICTION_GRACE_MS] 안에 못 받으면(Car 서비스 미연결·콘솔 하트비트 없음) 제한 없음으로 본다.
 * 주행 여부는 그래도 차량 신호(기어·차속)로 막는다([visibleScreen]의 knownMoving). 심사위원 에뮬레이터에서 앱이 먹통으로 보이지 않게(11-04 검토 #5).
 */
internal fun resolveUxRestricted(reported: Boolean?, graceElapsed: Boolean): Boolean? = reported ?: if (graceElapsed) false else null

internal const val UX_RESTRICTION_GRACE_MS = 5_000L

/** D를 거친 다음 P로 돌아왔을 때 한 번만 요약을 연다. 신호 대기는 주행 종료로 보지 않는다. */
internal class TripEndNavigation {
    private var drove = false

    fun shouldShowSummary(sessionStarted: Boolean, drive: DriveState): Boolean {
        if (!sessionStarted) {
            drove = false
            return false
        }
        if (drive.gear == Gear.DRIVE) drove = true
        if (drove && canShowParkedScreens(drive)) {
            drove = false
            return true
        }
        return false
    }
}

/** 개발 미리보기도 다른 정차 화면과 같은 플랫폼 제한을 따른다. 주행·UX 미확인 상태에는 터치가 없다. */
internal fun canShowDrivingPreviewBack(debug: Boolean, requested: Screen, drive: DriveState, uxRestricted: Boolean?): Boolean =
    debug && requested == Screen.DRIVING && visibleScreen(Screen.DEV_MENU, drive, uxRestricted) == Screen.DEV_MENU
