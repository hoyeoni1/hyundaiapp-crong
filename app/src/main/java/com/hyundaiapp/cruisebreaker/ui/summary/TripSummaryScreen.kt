package com.hyundaiapp.cruisebreaker.ui.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hyundaiapp.cruisebreaker.ui.PreviewData
import com.hyundaiapp.cruisebreaker.ui.TripSummaryUiState
import com.hyundaiapp.cruisebreaker.ui.components.PrimaryButton
import com.hyundaiapp.cruisebreaker.ui.components.ScreenHeader
import com.hyundaiapp.cruisebreaker.ui.components.StatTile
import com.hyundaiapp.cruisebreaker.ui.theme.CarColors
import com.hyundaiapp.cruisebreaker.ui.theme.CruiseBreakerTheme

/** S3 최근 주행 요약(정차 중). 안내 이벤트와 생성한 해제 기록을 보여 준다. */
@Composable
fun TripSummaryScreen(
    state: TripSummaryUiState,
    onShowRisks: () -> Unit,
    onDone: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 1000.dp
        Column(Modifier.fillMaxSize().padding(horizontal = if (wide) 48.dp else 32.dp, vertical = 32.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            ScreenHeader("최근 주행 요약", onBack = onDone)
            // 세 숫자는 같은 크기·같은 흰색으로 나란히 둔다. 칸은 내용 높이만큼만(화면을 채우려 늘리지 않는다)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                if (wide) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    SummaryTiles(state) { Modifier.weight(1f) }
                } else SummaryTiles(state) { Modifier.fillMaxWidth() }
            }
            PrimaryButton("해제 다발 구간 목록으로", onShowRisks, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun SummaryTiles(state: TripSummaryUiState, tileModifier: () -> Modifier) {
    val big = MaterialTheme.typography.displayLarge
    StatTile("안내 횟수", state.announcements.toString(), tileModifier(), unit = "회", valueStyle = big)
    StatTile("안내한 구간", state.riskSegmentsPassed.toString(), tileModifier(), unit = "곳", valueStyle = big)
    StatTile("내가 보탠 기록", state.myContributions.toString(), tileModifier(), unit = "건", valueStyle = big)
}

@Preview(name = "S3 주행 요약", widthDp = 1280, heightDp = 720)
@Composable
private fun TripSummaryPreview() {
    CruiseBreakerTheme { Surface { TripSummaryScreen(PreviewData.tripSummary, {}, {}) } }
}
