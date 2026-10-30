package com.hyundaiapp.cruisebreaker

import android.content.Context
import com.hyundaiapp.cruisebreaker.data.CruiseEventRepository
import com.hyundaiapp.cruisebreaker.data.FakeCruiseEventRepository
import com.hyundaiapp.cruisebreaker.data.FakeSegmentRiskRepository
import com.hyundaiapp.cruisebreaker.data.SegmentRiskRepository
import com.hyundaiapp.cruisebreaker.domain.DisengageDetector
import com.hyundaiapp.cruisebreaker.domain.DisengageRules
import com.hyundaiapp.cruisebreaker.domain.RiskAnnouncer
import com.hyundaiapp.core.CoreFeature
import com.hyundaiapp.core.PleosVehicleSignals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * 앱 전체에서 하나만 쓰는 객체 모음(간단한 수동 DI).
 * 서버가 준비되면 Fake 저장소를 실제 구현으로 바꾼다(황준용).
 */
class AppContainer(context: Context) {
    /** 화면과 무관하게 앱 수명 동안 도는 작업(해제 감지 등) */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // 매니페스트 퍼미션과 맞출 것: CAR_DRIVE → DRIVE, CAR_SPEED → SPEED, NAVI_ROUTE → NAVI
    val signals = PleosVehicleSignals(context, setOf(CoreFeature.DRIVE, CoreFeature.SPEED, CoreFeature.NAVI))

    val risks: SegmentRiskRepository = FakeSegmentRiskRepository()
    val events: CruiseEventRepository = FakeCruiseEventRepository()

    private val rules = DisengageRules()
    val disengageDetector = DisengageDetector(signals, rules)
    val announcer = RiskAnnouncer(rules)
}
