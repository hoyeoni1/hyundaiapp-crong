package com.hyundaiapp.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * 단위 테스트·UI 미리보기용. 값은 테스트 코드에서 직접 넣는다.
 *
 * ```
 * val fake = FakeVehicleSignals()
 * fake.setEv { copy(chargePortOpen = true, socPercent = 40) }
 * ```
 */
class FakeVehicleSignals : VehicleSignals, NaviControl {
    override val evBattery = MutableStateFlow(EvBatteryState(socPercent = 50, chargePortOpen = false, capacityKwh = 77.4f))
    override val drive = MutableStateFlow(DriveState(gear = Gear.PARK, speedKph = 0f, brakePressed = false))
    override val cruise = MutableStateFlow(CruiseState())
    // 에뮬레이터 기본 위치(판교 부근)
    override val location = MutableStateFlow<LocationState?>(LocationState(GeoPoint(37.4127, 127.0947), headingDeg = 0.0))
    override val route = MutableStateFlow(RouteState())
    override val sources = MutableStateFlow(
        SourceStatus(vehicle = SourceState.READY, speed = SourceState.READY, navi = SourceState.READY),
    )
    override val chargerOperators = MutableStateFlow(
        listOf(ChargerOperatorInfo("HD", "현대자동차(E-pit)"), ChargerOperatorInfo("ME", "환경부")),
    )

    /** 테스트에서 경로 요청이 나갔는지 확인용 */
    val requestedRoutes = mutableListOf<RouteDestination>()

    var started = false
        private set

    fun setEv(block: EvBatteryState.() -> EvBatteryState) = evBattery.update(block)
    fun setDrive(block: DriveState.() -> DriveState) = drive.update(block)
    fun setCruise(block: CruiseState.() -> CruiseState) = cruise.update(block)
    fun setLocation(lat: Double, lon: Double, headingDeg: Double = 0.0) {
        location.value = LocationState(GeoPoint(lat, lon), headingDeg, receivedAtMs = System.currentTimeMillis())
    }

    override fun start() {
        started = true
    }

    override fun stop() {
        started = false
    }

    override fun onPermissionsChanged() = Unit

    override fun requestRoute(destination: RouteDestination) {
        requestedRoutes += destination
        route.value = RouteState(guiding = true)
    }

    override fun cancelRoute() {
        route.value = RouteState()
    }
}
