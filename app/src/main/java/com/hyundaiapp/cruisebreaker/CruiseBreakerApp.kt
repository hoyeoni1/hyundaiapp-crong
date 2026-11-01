package com.hyundaiapp.cruisebreaker

import android.app.Application

class CruiseBreakerApp : Application() {
    val container by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        // 화면(S1·신호 모니터)용 신호 수신. 해제 감지·안내는 동의 후 RecordingService가 시작한다(PRD 5.4)
        container.signals.start()
    }
}
