package com.hyundaiapp.cruisebreaker

import android.content.Context
import com.hyundaiapp.cruisebreaker.data.AppPrefs
import com.hyundaiapp.cruisebreaker.data.CruiseEventRepository
import com.hyundaiapp.cruisebreaker.data.FakeCruiseServer
import com.hyundaiapp.cruisebreaker.data.SegmentRiskRepository
import com.hyundaiapp.cruisebreaker.data.SharedPrefsAppPrefs
import com.hyundaiapp.cruisebreaker.data.VehicleId
import com.hyundaiapp.cruisebreaker.domain.DisengageDetector
import com.hyundaiapp.cruisebreaker.domain.DisengageRules
import com.hyundaiapp.cruisebreaker.domain.RiskAnnouncer
import com.hyundaiapp.cruisebreaker.domain.RiskSpeaker
import com.hyundaiapp.cruisebreaker.domain.TripStats
import com.hyundaiapp.cruisebreaker.domain.TripTracker
import com.hyundaiapp.core.CoreFeature
import com.hyundaiapp.core.PleosVehicleSignals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * 앱 전체에서 하나만 쓰는 객체 모음(간단한 수동 DI).
 * 서버가 준비되면 [FakeCruiseServer]를 실제 구현으로 바꾼다(황준용). 업로드에는 [vehicleId]를 붙인다.
 */
class AppContainer(context: Context) {
    /** 화면과 무관하게 앱 수명 동안 도는 작업(해제 감지 등) */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // 매니페스트 퍼미션과 맞출 것: CAR_DRIVE → DRIVE, CAR_SPEED → SPEED, NAVI_ROUTE → NAVI
    val signals = PleosVehicleSignals(context, setOf(CoreFeature.DRIVE, CoreFeature.SPEED, CoreFeature.NAVI))

    /** 가명 차량 ID(설치마다 UUID). 실제 업로드 구현에서 쓴다 */
    val vehicleId: String = VehicleId.get(context)

    /** S4 설정·S5 동의 */
    val prefs: AppPrefs = SharedPrefsAppPrefs(context)

    private val server = FakeCruiseServer()
    val risks: SegmentRiskRepository = server
    val events: CruiseEventRepository = server

    private val rules = DisengageRules()
    val disengageDetector = DisengageDetector(signals, rules)
    val trips = TripTracker(signals)
    val tripStats = TripStats()
    val announcer = RiskAnnouncer(rules)
    val speaker = RiskSpeaker(context)
}
