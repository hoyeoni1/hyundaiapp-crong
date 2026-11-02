package com.hyundaiapp.cruisebreaker.ui.driving

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hyundaiapp.cruisebreaker.ui.DrivingUiState
import com.hyundaiapp.cruisebreaker.ui.PreviewData
import com.hyundaiapp.cruisebreaker.ui.components.CarIcons
import com.hyundaiapp.cruisebreaker.ui.components.formatWhole
import com.hyundaiapp.cruisebreaker.ui.theme.CarColors
import com.hyundaiapp.cruisebreaker.ui.theme.CruiseBreakerTheme

/**
 * S1 주행 화면(주행 중, DO). PRD 5.5 / 앱 개발 규정 2.3:
 * 터치 요소·애니메이션·자동 스크롤·여러 줄 텍스트 금지. 콜백 없이 상태만 받는다.
 * 평소에는 "기록 중" 작은 표시만, 안내가 있으면 주황 배너 한 줄.
 */
@Composable
fun DrivingScreen(state: DrivingUiState) {
    Column(
        Modifier.fillMaxSize().background(CarColors.Background).padding(horizontal = 48.dp, vertical = 40.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            RecordingChip(state.recording)
            Text(
                "크루즈 ${if (state.cruiseEngaged) "작동" else "꺼짐"} · ${formatWhole(state.speedKph?.toDouble())} km/h",
                style = MaterialTheme.typography.bodyLarge,
                color = CarColors.OnSurfaceVariant,
                maxLines = 1,
            )
        }
        val banner = state.banner
        if (banner != null) {
            Banner(banner)
        } else {
            Text(
                if (state.recording) "앞쪽에 안내할 구간이 없어요" else "차량 신호를 기다리는 중이에요",
                style = MaterialTheme.typography.titleLarge,
                color = CarColors.OnSurfaceVariant,
                maxLines = 1,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
        Text("크루즈브레이커", style = MaterialTheme.typography.bodyMedium, color = CarColors.Disabled)
    }
}

@Composable
private fun RecordingChip(recording: Boolean) {
    Row(
        Modifier.clip(RoundedCornerShape(999.dp)).background(CarColors.Surface).padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            CarIcons.Dot,
            contentDescription = null,
            tint = if (recording) CarColors.Primary else CarColors.Disabled,
            modifier = Modifier.size(18.dp),
        )
        Text(if (recording) "기록 중" else "차량 연결 대기", style = MaterialTheme.typography.bodyLarge)
    }
}

/** 안내 배너. 한 줄 고정(넘치면 말줄임), 움직임 없음 */
@Composable
private fun Banner(text: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(CarColors.Warning)
            .padding(horizontal = 56.dp, vertical = 48.dp)
            .semantics { liveRegion = LiveRegionMode.Assertive },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        Icon(CarIcons.Warning, contentDescription = null, tint = CarColors.OnAccent, modifier = Modifier.size(88.dp))
        Text(
            text,
            style = MaterialTheme.typography.displayMedium,
            color = CarColors.OnAccent,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview(name = "S1 평소", widthDp = 1280, heightDp = 720)
@Composable
private fun DrivingPreview() {
    CruiseBreakerTheme { Surface { DrivingScreen(PreviewData.driving) } }
}

@Preview(name = "S1 안내 배너", widthDp = 1280, heightDp = 720)
@Composable
private fun DrivingBannerPreview() {
    CruiseBreakerTheme { Surface { DrivingScreen(PreviewData.drivingWithBanner) } }
}

@Preview(name = "S1 연결 대기", widthDp = 1280, heightDp = 720)
@Composable
private fun DrivingWaitingPreview() {
    CruiseBreakerTheme { Surface { DrivingScreen(DrivingUiState()) } }
}
