package com.hyundaiapp.core

import kotlinx.coroutines.flow.StateFlow

/**
 * 두 앱이 공통으로 쓰는 차량·위치 신호. 초안 — 황준용이 11-01까지 확정한다.
 *
 * 값을 아직 못 받았으면 null. 근거 실측은 docs/vehicle-data-findings.md 6.2절.
 */
interface VehicleSignals {
    val evBattery: StateFlow<EvBatteryState>
    val cruise: StateFlow<CruiseState>
    val driverInput: StateFlow<DriverInputState>
    val location: StateFlow<GeoPoint?>

    fun start()
    fun stop()
}

data class EvBatteryState(
    val socPercent: Float? = null,
    val chargePortOpen: Boolean? = null,
    // getEvChargeSwitchState는 조회만 가능(리스너 없음) → 구현에서 주기 조회
    val chargeSwitchOn: Boolean? = null,
)

data class CruiseState(
    // CruiseControlState 이름 그대로(예: "ACTIVATED", "ENABLED"). 실차 해제 값이 확인되기 전까지 해석은 앱에서 한다
    val rawState: String? = null,
    val setSpeedKph: Float? = null,
)

data class DriverInputState(
    val brakePressed: Boolean? = null,
    // 에뮬레이터 콘솔 값과 부호가 반대로 들어옴
    val steeringAngleDeg: Float? = null,
)

data class GeoPoint(val latitude: Double, val longitude: Double)
