package com.hyundaiapp.cruisebreaker.ui.risk

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hyundaiapp.cruisebreaker.ui.PreviewData
import com.hyundaiapp.cruisebreaker.ui.RiskListUiState
import com.hyundaiapp.cruisebreaker.ui.RiskRowUi
import com.hyundaiapp.cruisebreaker.ui.components.CarCard
import com.hyundaiapp.cruisebreaker.ui.components.CarIcons
import com.hyundaiapp.cruisebreaker.ui.components.MeterBar
import com.hyundaiapp.cruisebreaker.ui.components.PillKind
import com.hyundaiapp.cruisebreaker.ui.components.ScreenHeader
import com.hyundaiapp.cruisebreaker.ui.components.SecondaryButton
import com.hyundaiapp.cruisebreaker.ui.components.StatusPill
import com.hyundaiapp.cruisebreaker.ui.components.formatKm
import com.hyundaiapp.cruisebreaker.ui.components.formatPercent
import com.hyundaiapp.cruisebreaker.ui.theme.CarColors
import com.hyundaiapp.cruisebreaker.ui.theme.CarDimens
import com.hyundaiapp.cruisebreaker.ui.theme.CruiseBreakerTheme

/** PRD 5.3 위험 구간 기준(해제율 ≥ 30%). 화면에서는 주황 표시에만 쓴다 */
private const val RISK_RATE = 0.30

/**
 * S2 위험 구간 목록(정차 중). 왼쪽 목록, 오른쪽 고른 구간 상세.
 * 고른 구간은 화면 안에서만 쓰는 값이라 UiState가 아니라 여기서 기억한다.
 */
@Composable
fun RiskListScreen(
    state: RiskListUiState,
    onSummaryClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = state.items.firstOrNull { it.id == selectedId } ?: state.items.firstOrNull()

    Column(
        Modifier.fillMaxSize().padding(horizontal = CarDimens.ScreenPadding, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        ScreenHeader("근처 위험 구간") {
            SecondaryButton("오늘 주행 요약", onSummaryClick, height = CarDimens.TouchTargetSmall)
            OutlinedIconButton(
                onClick = onSettingsClick,
                shape = RoundedCornerShape(CarDimens.ButtonRadius),
                border = BorderStroke(2.dp, CarColors.Outline),
                modifier = Modifier.size(CarDimens.TouchTargetSmall),
            ) {
                Icon(CarIcons.Settings, contentDescription = "설정", modifier = Modifier.size(30.dp))
            }
        }
        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.size(64.dp))
            }
            selected == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("근처에 위험 구간이 없어요", style = MaterialTheme.typography.titleLarge, color = CarColors.OnSurfaceVariant)
            }
            else -> Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Column(Modifier.weight(3f).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(state.items, key = { it.id }) { row ->
                            RiskRow(row, row.id == selected.id) { selectedId = row.id }
                        }
                    }
                    Text(
                        "해제율 = 크루즈 해제 수 ÷ 크루즈 통과 수 · ${formatPercent(RISK_RATE)} 이상 주황",
                        style = MaterialTheme.typography.bodySmall,
                        color = CarColors.OnSurfaceVariant,
                    )
                }
                RiskDetail(selected, Modifier.weight(2f).fillMaxSize())
            }
        }
    }
}

@Composable
private fun RiskRow(row: RiskRowUi, selected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (selected) CarColors.SurfaceVariant else CarColors.Surface,
        contentColor = CarColors.OnSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(2.dp, if (selected) CarColors.Primary else CarColors.Surface),
        modifier = Modifier.fillMaxWidth().semantics { this.selected = selected }.clickable(onClick = onClick),
    ) {
        Row(
            Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(row.roadName, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${formatKm(row.distanceM)} · 통과 ${row.passVehicles}대",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CarColors.OnSurfaceVariant,
                )
            }
            StatusPill(
                "해제율 ${formatPercent(row.rate)}",
                if (row.rate >= RISK_RATE) PillKind.WARNING else PillKind.NEUTRAL,
            )
        }
    }
}

@Composable
private fun RiskDetail(row: RiskRowUi, modifier: Modifier) {
    CarCard(modifier, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text("선택한 구간", style = MaterialTheme.typography.bodyMedium, color = CarColors.OnSurfaceVariant)
        Text(row.roadName, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
        LabeledMeter("해제율", row.rate, if (row.rate >= RISK_RATE) CarColors.Warning else CarColors.Primary)
        LabeledMeter("브레이크로 해제", row.brakeRatio, CarColors.Primary)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("크루즈 통과 차량", style = MaterialTheme.typography.bodyLarge)
            Text("${row.passVehicles}대", style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.weight(1f))
        Text("시연용 시드 데이터 포함", style = MaterialTheme.typography.bodySmall, color = CarColors.OnSurfaceVariant)
    }
}

@Composable
private fun LabeledMeter(label: String, ratio: Double, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(formatPercent(ratio), style = MaterialTheme.typography.labelLarge)
        }
        MeterBar(ratio.toFloat(), color)
    }
}

@Preview(name = "S2 위험 구간", widthDp = 1280, heightDp = 720)
@Composable
private fun RiskListPreview() {
    CruiseBreakerTheme { Surface { RiskListScreen(PreviewData.riskList, {}, {}) } }
}

@Preview(name = "S2 비어 있음", widthDp = 1280, heightDp = 720)
@Composable
private fun RiskListEmptyPreview() {
    CruiseBreakerTheme { Surface { RiskListScreen(RiskListUiState(loading = false), {}, {}) } }
}
