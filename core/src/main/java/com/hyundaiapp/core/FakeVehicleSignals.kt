package com.hyundaiapp.core

import kotlinx.coroutines.flow.MutableStateFlow

/** 실제 구현이 나오기 전 개발·UI 미리보기·테스트용. 값은 테스트 코드에서 직접 넣는다. */
class FakeVehicleSignals : VehicleSignals {
    override val evBattery = MutableStateFlow(EvBatteryState())
    override val cruise = MutableStateFlow(CruiseState())
    override val driverInput = MutableStateFlow(DriverInputState())
    // 에뮬레이터 기본 위치(판교 부근)
    override val location = MutableStateFlow<GeoPoint?>(GeoPoint(37.3948, 127.1112))

    override fun start() = Unit
    override fun stop() = Unit
}
