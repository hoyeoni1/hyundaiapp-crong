package com.hyundaiapp.cruisebreaker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.hyundaiapp.cruisebreaker.ui.CruiseBreakerNav
import com.hyundaiapp.cruisebreaker.ui.theme.CruiseBreakerTheme

// DO 선언 앱: 주행 중에도 화면이 보인다 → 주행 중 화면 규정(PRD 5.5) 준수
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 동의를 마친 상태면 기록 서비스를 켠다(앞에 있을 때만 포그라운드 서비스를 시작할 수 있음)
        RecordingService.startIfAllowed(this)
        setContent {
            CruiseBreakerTheme {
                CruiseBreakerNav((application as CruiseBreakerApp).container)
            }
        }
    }
}
