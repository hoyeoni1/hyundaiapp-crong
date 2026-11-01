package com.hyundaiapp.cruisebreaker

import android.app.Application

class CruiseBreakerApp : Application() {
    val container by lazy { AppContainer(this) }

    // 차량·위치 신호 수신은 동의 후 RecordingService(AppContainer.startRecording)가 시작한다(이슈 #9).
    // 디버그 빌드의 신호 모니터는 화면에 들어갈 때 따로 시작한다
}
