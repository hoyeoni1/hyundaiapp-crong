package com.hyundaiapp.crong.ui.risk

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.hyundaiapp.crong.ui.components.Text
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
import com.hyundaiapp.crong.ui.PreviewData
import com.hyundaiapp.crong.ui.RiskListUiState
import com.hyundaiapp.crong.ui.RiskRowUi
import com.hyundaiapp.crong.ui.components.CarCard
import com.hyundaiapp.crong.ui.components.CarIcons
import com.hyundaiapp.crong.ui.components.EmptyState
import com.hyundaiapp.crong.ui.components.HeaderIconButton
import com.hyundaiapp.crong.ui.components.MeterBar
import com.hyundaiapp.crong.ui.components.ScreenHeader
import com.hyundaiapp.crong.ui.components.formatKm
import com.hyundaiapp.crong.ui.components.formatPercent
import com.hyundaiapp.crong.ui.theme.CarColors
import com.hyundaiapp.crong.ui.theme.CrongTheme

/** PRD 5.3 위험 구간 기준(해제율 ≥ 30%). 화면에서는 주황 표시에만 쓴다 */
private const val RISK_RATE = 0.30

/**
 * S2 위험 구간 목록(정차 중). 왼쪽 목록, 오른쪽 고른 구간 상세.
 * 고른 구간은 화면 안에서만 쓰는 값이라 UiState가 아니라 여기서 기억한다.
 * 조회 실패 알림은 표시하지 않는다. 보조 안내의 한계는 S5에서 설명한다(#25 결정).
 */
@Composable
fun RiskListScreen(
    state: RiskListUiState,
    onSummaryClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onBack: () -> Unit,
    /** 디버그 빌드 개발 메뉴. 제목을 길게 누르면 연다(화면에는 보이지 않음) */
    onDevMenu: (() -> Unit)? = null,
) {
    val listState = rememberLazyListState()
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = state.items.firstOrNull { it.id == selectedId } ?: state.items.firstOrNull()

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 1000.dp
        Column(Modifier.fillMaxSize().padding(horizontal = if (wide) 48.dp else 32.dp, vertical = 32.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            ScreenHeader("근처 해제 다발 구간", onBack = onBack, inlineActions = true, onTitleLongPress = onDevMenu) {
                HeaderIconButton(CarIcons.Chart, "최근 주행 요약", onSummaryClick)
                HeaderIconButton(CarIcons.Settings, "설정", onSettingsClick)
            }
            when {
                state.loading && selected == null -> EmptyState("해제 다발 구간을 불러오는 중이에요", loading = true, modifier = Modifier.weight(1f))
                selected == null -> EmptyState("근처에 해제 다발 구간이 없어요", modifier = Modifier.weight(1f))
                wide -> Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    LazyColumn(Modifier.weight(3f), state = listState, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(state.items, key = { it.id }) { row -> RiskRow(row, row.id == selected.id) { selectedId = row.id } }
                    }
                    LazyColumn(Modifier.weight(2f)) { item { RiskDetail(selected, Modifier.fillMaxWidth()) } }
                }
                else -> LazyColumn(Modifier.weight(1f), state = listState, verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
        // 선택은 흰 테두리. 앰버(대표색)는 위험 구간 주황과 거의 같아서 선택에는 쓰지 않는다
        border = BorderStroke(2.dp, if (selected) CarColors.OnSurface else CarColors.Surface),
        modifier = Modifier.fillMaxWidth().semantics { this.selected = selected }.clickable(onClick = onClick),
    ) {
        Row(
            Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(row.roadName, style = MaterialTheme.typography.titleMedium)
                Text(formatKm(row.distanceM), style = MaterialTheme.typography.bodyMedium, color = CarColors.OnSurfaceVariant)
            }
            // 목록에서 비교할 값은 해제율 하나라 오른쪽에 크게 둔다. 기준 이상만 주황
            Column(horizontalAlignment = Alignment.End) {
                Text("해제율", style = MaterialTheme.typography.bodyMedium, color = CarColors.OnSurfaceVariant)
                Text(formatPercent(row.rate), style = MaterialTheme.typography.titleLarge, color = rateColor(row.rate))
            }
            Icon(if (selected) CarIcons.Check else CarIcons.ChevronRight, contentDescription = if (selected) "선택됨" else "구간 상세 선택", tint = if (selected) CarColors.OnSurface else CarColors.OnSurfaceVariant, modifier = Modifier.size(28.dp))

        }
    }
}

@Composable
private fun RiskDetail(row: RiskRowUi, modifier: Modifier) {
    CarCard(modifier, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("선택한 구간", style = MaterialTheme.typography.labelMedium, color = CarColors.OnSurfaceVariant)
            Text(row.roadName, style = MaterialTheme.typography.titleLarge, maxLines = Int.MAX_VALUE)
        }
        LabeledMeter("해제율", row.rate, if (row.rate >= RISK_RATE) CarColors.Warning else CarColors.OnSurfaceVariant)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("크루즈 통과", style = MaterialTheme.typography.bodyMedium, color = CarColors.OnSurfaceVariant)
            Row {
                Text("${row.passVehicles}", style = MaterialTheme.typography.displaySmall, modifier = Modifier.alignByBaseline())
                Text("차량·일", style = MaterialTheme.typography.bodyLarge, color = CarColors.OnSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp).alignByBaseline())
            }
        }
        Text("시연용 시드 데이터 포함", style = MaterialTheme.typography.bodySmall, color = CarColors.OnSurfaceVariant)
    }
}

private fun rateColor(rate: Double) = if (rate >= RISK_RATE) CarColors.Warning else CarColors.OnSurface

@Composable
private fun LabeledMeter(label: String, ratio: Double, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = CarColors.OnSurfaceVariant, modifier = Modifier.alignByBaseline())
            Text(formatPercent(ratio), style = MaterialTheme.typography.displaySmall, color = rateColor(ratio), modifier = Modifier.alignByBaseline())
        }
        MeterBar(ratio.toFloat(), color)
    }
}

@Preview(name = "S2 해제 다발 구간", widthDp = 1280, heightDp = 720)
@Composable
private fun RiskListPreview() {
    CrongTheme { Surface { RiskListScreen(PreviewData.riskList, {}, {}, onBack = {}) } }
}

@Preview(name = "S2 비어 있음", widthDp = 1280, heightDp = 720)
@Composable
private fun RiskListEmptyPreview() {
    CrongTheme { Surface { RiskListScreen(RiskListUiState(loading = false), {}, {}, onBack = {}) } }
}
