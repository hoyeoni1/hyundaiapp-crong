package com.hyundaiapp.crong.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 한 주행의 숫자(S3 주행 후 요약) */
data class TripSnapshot(
    /** 안내받은 위험 구간 수. 같은 구간은 한 주행에 1번만 안내하므로 서로 다른 구간 수와 같다 */
    val riskSegments: Int = 0,
    val announcements: Int = 0,
    /** 내가 남긴 해제 기록 수(다른 운전자를 위한 기여) */
    val disengages: Int = 0,
)

/**
 * 주행별 요약 집계. 담당: 이민하. 새 주행([TripTracker.tripNumber])이 시작되면 0으로 돌아가고,
 * P로 멈춘 뒤에는 마지막 주행 값을 그대로 둔다(정차 중 S3에서 보여 줌).
 */
class TripStats {
    private val _current = MutableStateFlow(TripSnapshot())
    val current: StateFlow<TripSnapshot> = _current.asStateFlow()

    private val segments = mutableSetOf<SegmentKey>()

    /** 주행 요약을 지운다(동의 철회) */
    fun reset() {
        segments.clear()
        _current.value = TripSnapshot()
    }

    fun start(scope: CoroutineScope, trips: TripTracker, announcer: RiskAnnouncer, detector: DisengageDetector) {
        scope.launch {
            trips.tripNumber.collect { reset() }
        }
        scope.launch {
            announcer.announcements.collect { a ->
                segments += a.risk.key
                _current.update { it.copy(riskSegments = segments.size, announcements = it.announcements + 1) }
            }
        }
        scope.launch {
            detector.events.collect { _current.update { it.copy(disengages = it.disengages + 1) } }
        }
    }
}
