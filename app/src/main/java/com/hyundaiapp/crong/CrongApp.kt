package com.hyundaiapp.crong

import android.app.Application

class CrongApp : Application() {
    val container by lazy { AppContainer(this) }

    // 차량·위치 신호 수신은 동의 후 RecordingService(AppContainer.startRecording)가 시작한다(이슈 #9).
    // 디버그 빌드의 신호 모니터는 화면에 들어갈 때 따로 시작한다

    override fun onCreate() {
        super.onCreate()
        // 동의 철회 뒤 서버 삭제를 못 끝내고 앱이 죽었으면 이어서 보낸다(PRD 5.6)
        container.resumePendingDeletions()
    }
}
