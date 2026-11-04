package com.hyundaiapp.cruisebreaker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
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
                // Pleos 에뮬레이터에서 아래 시스템 바(140px)가 앱 창 아래를 덮는데, 창은 바까지 그리고 inset도 주지 않는다.
                // 설정의 화면 높이(screenHeightDp)만 바를 뺀 값이라 그 높이로 맞춰 아래 고정 버튼이 바에 가리지 않게 한다(11-04 확인)
                Box(Modifier.fillMaxWidth().heightIn(max = LocalConfiguration.current.screenHeightDp.dp)) {
                    CruiseBreakerNav((application as CruiseBreakerApp).container, onExit = { finish() })
                }
            }
        }
    }
}
