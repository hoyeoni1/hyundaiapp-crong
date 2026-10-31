package com.hyundaiapp.cruisebreaker.ui.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hyundaiapp.cruisebreaker.ui.PreviewData
import com.hyundaiapp.cruisebreaker.ui.TripSummaryUiState
import com.hyundaiapp.cruisebreaker.ui.components.PrimaryButton
import com.hyundaiapp.cruisebreaker.ui.components.SecondaryButton
import com.hyundaiapp.cruisebreaker.ui.components.StatTile
import com.hyundaiapp.cruisebreaker.ui.theme.CarColors
import com.hyundaiapp.cruisebreaker.ui.theme.CarDimens
import com.hyundaiapp.cruisebreaker.ui.theme.CruiseBreakerTheme

/** S3 주행 후 요약(정차 중). 숫자 세 개를 크게 */
@Composable
fun TripSummaryScreen(
    state: TripSummaryUiState,
    onShowRisks: () -> Unit,
    onDone: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = CarDimens.ScreenPadding, vertical = 40.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("오늘 주행 요약", style = MaterialTheme.typography.headlineMedium)
            Text(
                if (state.riskSegmentsPassed > 0) "위험 구간을 미리 알고 지나갔어요" else "오늘은 위험 구간을 지나지 않았어요",
                style = MaterialTheme.typography.bodyLarge,
                color = CarColors.OnSurfaceVariant,
            )
        }
        Row(Modifier.height(300.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            BigStat("지난 위험 구간", state.riskSegmentsPassed, "곳", CarColors.Warning, Modifier.weight(1f))
            BigStat("음성 안내", state.announcements, "회", CarColors.OnSurface, Modifier.weight(1f))
            BigStat("내가 보탠 기록", state.myContributions, "건", CarColors.Primary, Modifier.weight(1f), "다른 운전자 안내에 쓰여요")
        }
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.End)) {
            SecondaryButton("위험 구간 보기", onShowRisks)
            PrimaryButton("확인", onDone)
        }
    }
}

@Composable
private fun BigStat(label: String, value: Int, unit: String, color: Color, modifier: Modifier, caption: String? = null) {
    StatTile(
        label = label,
        value = value.toString(),
        unit = unit,
        valueColor = color,
        valueStyle = MaterialTheme.typography.displayLarge,
        caption = caption,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxHeight(),
    )
}

@Preview(name = "S3 주행 요약", widthDp = 1280, heightDp = 720)
@Composable
private fun TripSummaryPreview() {
    CruiseBreakerTheme { Surface { TripSummaryScreen(PreviewData.tripSummary, {}, {}) } }
}
