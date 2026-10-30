package com.hyundaiapp.cruisebreaker

import android.app.Application

class CruiseBreakerApp : Application() {
    val container by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        // 주행 중 기록은 화면과 무관하게 계속한다. 단 내비가 앞에 오면 프로세스가 얼 수 있음(PRD 5.4, 이민하 실험)
        container.signals.start()
        container.disengageDetector.start(container.appScope)
    }
}
