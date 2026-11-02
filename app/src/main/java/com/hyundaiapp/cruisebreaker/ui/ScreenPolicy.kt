package com.hyundaiapp.cruisebreaker.ui

import com.hyundaiapp.core.DriveState
import com.hyundaiapp.core.Gear

/** P가 확인된 동안만 정차 화면을 연다. 차속 권한이 없으면 기어로 판단한다. */
internal fun canShowParkedScreens(drive: DriveState): Boolean =
    drive.gear == Gear.PARK && (drive.speedKph == null || drive.speedKph == 0f)

/** 동의 후 신호 대기·D/R/N 상태에서는 개발 메뉴·설정 대신 터치 없는 S1만 보여 준다. */
internal fun visibleScreen(requested: Screen, sessionStarted: Boolean, drive: DriveState): Screen =
    if (sessionStarted && !canShowParkedScreens(drive)) Screen.DRIVING else requested

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
