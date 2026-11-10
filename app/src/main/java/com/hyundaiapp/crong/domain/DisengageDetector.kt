package com.hyundaiapp.crong.domain

import com.hyundaiapp.core.CruiseMode
import com.hyundaiapp.core.LocationState
import com.hyundaiapp.core.VehicleSignals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * 해제 이벤트 감지(F1)·통과 기록. 담당: 이민하. 규칙: docs/disengage-rules.md "차 쪽 규칙", [DisengageRules].
 *
 * 입력: [VehicleSignals.cruise](→ [CruiseStateInterpreter]), [VehicleSignals.drive](차속), [VehicleSignals.location](구간)
 * 출력: [events](업로드·S3 요약), [passes](해제율 분모)
 *
 * 원인은 나누지 않고 사실만 기록한다(브레이크·정체 구분 없음). 시간은 [clock]으로만 읽어서 테스트에서 바꿀 수 있게 한다.
 * 상태는 [start]에 넘긴 scope의 디스패처(앱은 Main) 한 스레드에서만 바뀐다.
 */
class DisengageDetector(
    private val signals: VehicleSignals,
    private val rules: DisengageRules,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val _events = MutableSharedFlow<DisengageEvent>(extraBufferCapacity = 16)
    private val _passes = MutableSharedFlow<CruisePass>(extraBufferCapacity = 64)

    val events: SharedFlow<DisengageEvent> = _events.asSharedFlow()
    val passes: SharedFlow<CruisePass> = _passes.asSharedFlow()

    private var previousMode: CruiseMode? = null

    /** 작동을 시작한 시각. 작동 중이 아니면 null */
    private var engagedSinceMs: Long? = null

    /** 이번 작동 중 이미 통과로 센 구간. GPS가 칸 경계에서 흔들려 A→B→A가 돼도 A는 한 번만 센다(PR #3 리뷰) */
    private val passedThisEngagement = mutableSetOf<SegmentKey>()

    /** 수집 쪽이 늦어 버퍼가 차서 버려진 기록 수. 0이 아니면 업로드 구조를 다시 봐야 한다 */
    var dropped = 0
        private set

    /**
     * 기록을 시작한다(scope가 끝나면 함께 멈춤). 동의 철회 뒤 다시 시작할 수 있으므로 이전 상태를 지우고 시작한다
     * — 남은 "작동 중" 상태 때문에 재시작 직후 첫 값을 해제로 보는 일이 없게.
     */
    fun start(scope: CoroutineScope) {
        previousMode = null
        engagedSinceMs = null
        passedThisEngagement.clear()
        scope.launch { signals.cruise.collect { onCruise(it.mode) } }
        scope.launch { signals.location.collect { onLocation(it) } }
    }

    private fun onCruise(mode: CruiseMode?) {
        val now = clock()
        val previous = previousMode
        previousMode = mode
        when {
            // 연결 직후 첫 값이 비작동이면 previous가 null이라 해제로 보지 않는다(PRD 5.4)
            CruiseStateInterpreter.isDisengage(previous, mode) -> {
                val since = engagedSinceMs
                if (since != null && now - since >= rules.minEngagedMs && fastEnough()) recordDisengage(now)
                engagedSinceMs = null
                passedThisEngagement.clear()
            }
            CruiseStateInterpreter.isEngaged(mode) && engagedSinceMs == null -> {
                engagedSinceMs = now
                signals.location.value?.let { recordPass(now, keyOf(it)) }
            }
        }
    }

    private fun onLocation(location: LocationState?) {
        if (location == null || engagedSinceMs == null) return
        recordPass(clock(), keyOf(location))
    }

    /** 차속을 모르면(권한 없음) 속도 조건은 건너뛴다 */
    private fun fastEnough(): Boolean {
        val speed = signals.drive.value.speedKph ?: return true
        return speed >= rules.minSpeedKph
    }

    private fun recordDisengage(now: Long) {
        val location = signals.location.value ?: return
        val key = keyOf(location)
        // 해제한 구간은 반드시 통과로도 센다(해제율이 1을 넘지 않게)
        recordPass(now, key)
        if (!_events.tryEmit(DisengageEvent(now, key, signals.drive.value.speedKph, location.point))) dropped++
    }

    private fun recordPass(now: Long, key: SegmentKey) {
        if (!passedThisEngagement.add(key)) return
        if (!_passes.tryEmit(CruisePass(now, key))) dropped++
    }

    private fun keyOf(location: LocationState) = SegmentKey.of(location.point, location.headingDeg)
}
