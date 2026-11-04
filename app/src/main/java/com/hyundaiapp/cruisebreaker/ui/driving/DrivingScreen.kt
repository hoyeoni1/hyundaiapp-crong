package com.hyundaiapp.cruisebreaker.ui.driving

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
import com.hyundaiapp.cruisebreaker.ui.components.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        Modifier.fillMaxSize().background(CarColors.Background).padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            RecordingChip(state.recording)
            // 크루즈 상태는 작동일 때만 파랑, 속도는 굵은 숫자 + 작은 단위
            Row {
                Text(
                    "크루즈 ${if (state.cruiseEngaged) "작동" else "꺼짐"}",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (state.cruiseEngaged) CarColors.Primary else CarColors.OnSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.padding(end = 24.dp).alignByBaseline(),
                )
                Text(formatWhole(state.speedKph?.toDouble()), style = MaterialTheme.typography.displaySmall, maxLines = 1, modifier = Modifier.alignByBaseline())
                Text("km/h", style = MaterialTheme.typography.bodyLarge, color = CarColors.OnSurfaceVariant, maxLines = 1,
                    modifier = Modifier.padding(start = 8.dp).alignByBaseline())
            }
        }
        val banner = state.banner
        if (banner != null) {
            Banner(banner)
        } else {
            IdleStatus(state, Modifier.align(Alignment.CenterHorizontally))
        }
        Text("크루즈브레이커", style = MaterialTheme.typography.bodyMedium, color = CarColors.Disabled)
    }
}

/** 안내가 없을 때도 지금 상태를 한 줄로 크게 보여 준다. 안내는 크루즈 작동 중에만 나온다 */
@Composable
private fun IdleStatus(state: DrivingUiState, modifier: Modifier) {
    val (icon, text, tint) = when {
        !state.recording -> Triple(CarIcons.Dot, "차량 신호를 기다리는 중", CarColors.Disabled)
        !state.cruiseEngaged -> Triple(CarIcons.Dot, "크루즈를 켜면 안내해요", CarColors.Disabled)
        else -> Triple(CarIcons.ShieldCheck, "안내할 구간 없음", CarColors.Primary)
    }
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(if (icon == CarIcons.Dot) 28.dp else 64.dp))
        Text(text, style = MaterialTheme.typography.displaySmall, color = if (icon == CarIcons.Dot) CarColors.OnSurfaceVariant else CarColors.OnSurface, maxLines = 1, softWrap = false)
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
        Text(if (recording) "차량 연결됨" else "차량 연결 대기", style = MaterialTheme.typography.bodyLarge)
    }
}

/** 안내 배너. 표준 안내는 폭에 맞는 동등한 문구를 골라 거리·해제 경고를 한 줄로 보존한다. */
@Composable
private fun Banner(text: String) {
    BoxWithConstraints(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(CarColors.Warning).padding(horizontal = 12.dp, vertical = 32.dp)) {
        val measurer = rememberTextMeasurer()
        val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
        val normal = MaterialTheme.typography.displayMedium
        val minimum = normal.copy(fontSize = 56.sp, lineHeight = 64.sp)
        val distance = Regex("^(\\d+(?:\\.\\d+)?m) 앞 크루즈 해제 잦은 구간$").matchEntire(text)?.groupValues?.get(1)
        val candidates = listOf(text) + if (distance != null) listOf("$distance 앞 해제 잦음", "$distance 해제주의") else emptyList()
        val chosen = candidates.firstOrNull { measurer.measure(it, minimum, softWrap = false).size.width <= widthPx } ?: text
        val style = if (measurer.measure(chosen, normal, softWrap = false).size.width <= widthPx) normal else minimum
        Text(chosen, style = style, color = CarColors.OnAccent, maxLines = 1, softWrap = false,
            modifier = Modifier.align(Alignment.Center).semantics { liveRegion = LiveRegionMode.Assertive })
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
