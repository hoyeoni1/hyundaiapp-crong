package com.hyundaiapp.cruisebreaker

import android.app.Application
import android.util.Log
import kotlinx.coroutines.launch

class CruiseBreakerApp : Application() {
    val container by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        // 주행 중 기록은 화면과 무관하게 계속한다. 단 내비가 앞에 오면 프로세스가 얼 수 있음(PRD 5.4, 이민하 실험)
        container.signals.start()
        // 감지기보다 먼저 구독해야 첫 이벤트를 놓치지 않는다(같은 Main 디스패처에서 순서대로 시작)
        // 확인: adb logcat -s CruiseRecord
        container.appScope.launch {
            container.disengageDetector.events.collect {
                Log.i(TAG, "disengage ${it.segment} speed=${it.speedKph}")
                container.events.upload(it)
            }
        }
        container.appScope.launch {
            container.disengageDetector.passes.collect {
                Log.i(TAG, "pass ${it.segment}")
                container.events.upload(it)
            }
        }
        container.disengageDetector.start(container.appScope)

        container.trips.start(container.appScope)
        container.appScope.launch {
            container.announcer.announcements.collect { Log.i(TAG, "announce ${it.risk.key} ${it.bannerText}") }
        }
        container.speaker.start(container.appScope, container.announcer.announcements)
        container.announcer.start(container.appScope, container.signals, container.risks, container.trips)
    }

    private companion object {
        const val TAG = "CruiseRecord"
    }
}
