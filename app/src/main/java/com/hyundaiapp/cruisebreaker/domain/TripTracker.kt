package com.hyundaiapp.cruisebreaker.domain

import com.hyundaiapp.core.Gear
import com.hyundaiapp.core.VehicleSignals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * 한 주행 = 기어 D로 출발 ~ P로 바뀔 때까지(docs/disengage-rules.md, 11-01). 담당: 이민하.
 * 중간에 R·N으로 가도 주행은 이어진다. 안내 "한 주행에 1회"와 S3 요약의 기준.
 */
class TripTracker(private val signals: VehicleSignals) {
    private val _tripNumber = MutableStateFlow(0)
    private val _driving = MutableStateFlow(false)

    /** 주행이 시작될 때마다 1씩 늘어난다. 0 = 아직 주행 없음 */
    val tripNumber: StateFlow<Int> = _tripNumber.asStateFlow()

    /** 지금 주행 중인지 */
    val driving: StateFlow<Boolean> = _driving.asStateFlow()

    fun start(scope: CoroutineScope) {
        scope.launch {
            signals.drive.map { it.gear }.distinctUntilChanged().collect { gear ->
                when {
                    gear == Gear.DRIVE && !_driving.value -> {
                        _driving.value = true
                        _tripNumber.value += 1
                    }
                    gear == Gear.PARK && _driving.value -> _driving.value = false
                }
            }
        }
    }
}
