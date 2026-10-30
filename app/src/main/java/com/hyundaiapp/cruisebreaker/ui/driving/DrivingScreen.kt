package com.hyundaiapp.cruisebreaker.ui.driving

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyundaiapp.cruisebreaker.ui.DrivingUiState
import com.hyundaiapp.cruisebreaker.ui.PreviewData
import com.hyundaiapp.cruisebreaker.ui.theme.CruiseBreakerTheme

// 임시 화면(동작 확인용). 디자인은 길민종이 교체한다.
// 주행 중 화면: 터치 요소·애니메이션·여러 줄 텍스트 금지. 평소에는 작은 상태 표시, 안내 시 큰 배너 한 줄
@Composable
fun DrivingScreen(state: DrivingUiState) {
    Column(Modifier.fillMaxSize().padding(32.dp)) {
        Text(
            if (state.recording) "● 기록 중" else "○ 차량 연결 대기",
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            "크루즈 ${if (state.cruiseEngaged) "작동" else "꺼짐"} · ${state.speedKph?.let { "%.0f".format(it) } ?: "-"} km/h",
            style = MaterialTheme.typography.bodyLarge,
        )
        state.banner?.let {
            Text(it, fontSize = 56.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 48.dp), maxLines = 1)
        }
    }
}

@Preview(widthDp = 1280, heightDp = 720)
@Composable
private fun DrivingPreview() {
    CruiseBreakerTheme { Surface { DrivingScreen(PreviewData.drivingWithBanner) } }
}
