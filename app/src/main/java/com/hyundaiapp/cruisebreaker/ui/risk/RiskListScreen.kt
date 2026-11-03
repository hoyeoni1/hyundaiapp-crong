package com.hyundaiapp.cruisebreaker.ui.risk

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hyundaiapp.cruisebreaker.ui.PreviewData
import com.hyundaiapp.cruisebreaker.ui.RiskListUiState
import com.hyundaiapp.cruisebreaker.ui.RiskRowUi
import com.hyundaiapp.cruisebreaker.ui.components.CarCard
import com.hyundaiapp.cruisebreaker.ui.components.CarIcons
import com.hyundaiapp.cruisebreaker.ui.components.EmptyState
import com.hyundaiapp.cruisebreaker.ui.components.MeterBar
import com.hyundaiapp.cruisebreaker.ui.components.ScreenHeader
import com.hyundaiapp.cruisebreaker.ui.components.SecondaryButton
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
 * 오류·조회 기준은 로직에서 확정한 값만 받는다. 화면에서 위치나 서버 결과를 추정하지 않는다.
 */
@Composable
fun RiskListScreen(
    state: RiskListUiState,
    onSummaryClick: () -> Unit,
    onSettingsClick: () -> Unit,
    errorMessage: String? = null,
    locationNotice: String? = null,
    onRetry: (() -> Unit)? = null,
) {
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = state.items.firstOrNull { it.id == selectedId } ?: state.items.firstOrNull()

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 1000.dp
        Column(Modifier.fillMaxSize().padding(horizontal = if (wide) 48.dp else 32.dp, vertical = 32.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            ScreenHeader("근처 위험 구간") {
                SecondaryButton("최근 주행 요약", onSummaryClick, height = CarDimens.TouchTargetSmall)
                SecondaryButton("설정", onSettingsClick, height = CarDimens.TouchTargetSmall)
            }
            if (locationNotice != null) Text(locationNotice, style = MaterialTheme.typography.bodyMedium, color = CarColors.OnSurfaceVariant)
            if (selected != null && (errorMessage != null || state.loading)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(if (state.loading) "최신 정보를 확인하고 있어요" else "갱신하지 못했어요. 이전 정보를 표시해요",
                        style = MaterialTheme.typography.bodyMedium, color = CarColors.OnSurfaceVariant, modifier = Modifier.weight(1f))
                    if (!state.loading && onRetry != null) SecondaryButton("다시 시도", onRetry, height = CarDimens.TouchTargetSmall)
                }
            }
            when {
                state.loading && selected == null -> EmptyState("위험 구간을 불러오는 중이에요", loading = true, modifier = Modifier.weight(1f))
                errorMessage != null && selected == null -> EmptyState("위험 구간을 불러오지 못했어요", errorMessage, onRetry = onRetry,
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()))
                selected == null -> EmptyState("근처에 위험 구간이 없어요", onRetry = onRetry, modifier = Modifier.weight(1f))
                wide -> Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    LazyColumn(Modifier.weight(3f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(state.items, key = { it.id }) { row -> RiskRow(row, row.id == selected.id) { selectedId = row.id } }
                    }
                    LazyColumn(Modifier.weight(2f)) { item { RiskDetail(selected, Modifier.fillMaxWidth()) } }
                }
                else -> LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.items, key = { it.id }) { row ->
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            RiskRow(row, row.id == selected.id) { selectedId = row.id }
                            if (row.id == selected.id) RiskDetail(row, Modifier.fillMaxWidth())
                        }
                    }
                }
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
                Text(row.roadName, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${formatKm(row.distanceM)} · 해제율 ${formatPercent(row.rate)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CarColors.OnSurfaceVariant,
                )
            }
            Icon(if (selected) CarIcons.Check else CarIcons.ChevronRight, contentDescription = if (selected) "선택됨" else "구간 상세 선택", tint = CarColors.Primary, modifier = Modifier.size(28.dp))

        }
    }
}

@Composable
private fun RiskDetail(row: RiskRowUi, modifier: Modifier) {
    CarCard(modifier, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text("선택한 구간", style = MaterialTheme.typography.bodyMedium, color = CarColors.OnSurfaceVariant)
        Text(row.roadName, style = MaterialTheme.typography.titleLarge, maxLines = Int.MAX_VALUE)
        LabeledMeter("해제율", row.rate, if (row.rate >= RISK_RATE) CarColors.Warning else CarColors.Primary)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("크루즈 통과", style = MaterialTheme.typography.bodyLarge)
            Text("${row.passVehicles} 차량·일", style = MaterialTheme.typography.labelLarge)
        }
        Text("최근 28일 · 같은 방향 · 통과 20 차량·일 이상에서 해제율 30% 이상", style = MaterialTheme.typography.bodyMedium, color = CarColors.OnSurfaceVariant)
        Text("차량·일: 같은 차가 하루에 여러 번 지나도 1회로 집계해요", style = MaterialTheme.typography.bodyMedium, color = CarColors.OnSurfaceVariant)
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
