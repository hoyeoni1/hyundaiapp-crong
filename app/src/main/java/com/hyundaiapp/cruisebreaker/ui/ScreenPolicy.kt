package com.hyundaiapp.cruisebreaker.ui

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
