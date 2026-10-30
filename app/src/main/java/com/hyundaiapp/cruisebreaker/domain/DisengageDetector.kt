package com.hyundaiapp.cruisebreaker.domain

import com.hyundaiapp.core.VehicleSignals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 해제 이벤트 감지(F1)·정체 거르기(F2)·통과 기록. 담당: 이민하.
 *
 * 입력: [VehicleSignals.cruise](→ [CruiseStateInterpreter]), [VehicleSignals.drive](차속·브레이크),
 *       [VehicleSignals.location](구간), [VehicleSignals.route](혼잡 구간)
 * 출력: [events](업로드·S3 요약), [passes](해제율 분모)
 *
 * 규칙은 PRD 5.3과 [DisengageRules]. 시간은 [clock]으로만 읽어서 테스트에서 바꿀 수 있게 한다.
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

    /** 앱 수명 동안 한 번 부른다(scope가 끝나면 함께 멈춤). */
    fun start(scope: CoroutineScope) {
        // TODO(이민하): PRD 5.3 구현
        //  - 크루즈 작동이 minEngagedMs 이상 유지된 뒤 CruiseStateInterpreter.isDisengage → 후보
        //  - 차속 ≥ minSpeedKph, 직전 brakeWindowMs 안 브레이크 → BRAKE
        //  - 정체 제외: route.congestion(HEAVY/SLOW) 안이거나 30초 평균 차속 < congestionAvgKph
        //  - 크루즈 작동 중 새 SegmentKey에 들어서면 passes.emit
    }
}
