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
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Text("최근 주행에서 생성한 안내와 기록이에요", style = MaterialTheme.typography.bodyLarge, color = CarColors.OnSurfaceVariant)
                val metrics: @Composable (Modifier) -> Unit = { modifier ->
                    StatTile("안내 횟수", state.announcements.toString(), modifier, unit = "회", valueStyle = MaterialTheme.typography.displayLarge, caption = "배너 안내 기준이에요")
                }
                if (wide) Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    metrics(Modifier.weight(1f))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        SummaryDetails(state)
                    }
                } else {
                    metrics(Modifier.fillMaxWidth())
                    SummaryDetails(state)
                }
            }
            PrimaryButton("위험 구간 목록으로", onShowRisks, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun SummaryDetails(state: TripSummaryUiState) {
    StatTile("안내한 구간", state.riskSegmentsPassed.toString(), Modifier.fillMaxWidth(), unit = "곳", valueColor = CarColors.Warning,
        valueStyle = MaterialTheme.typography.displaySmall, caption = "같은 구간은 한 번씩 세요")
    StatTile("내가 보탠 기록", state.myContributions.toString(), Modifier.fillMaxWidth(), unit = "건", valueColor = CarColors.Primary,
        valueStyle = MaterialTheme.typography.displaySmall, caption = "이번 주행에서 생성한 기록이에요")
}

@Preview(name = "S3 주행 요약", widthDp = 1280, heightDp = 720)
@Composable
private fun TripSummaryPreview() {
    CruiseBreakerTheme { Surface { TripSummaryScreen(PreviewData.tripSummary, {}, {}) } }
}
