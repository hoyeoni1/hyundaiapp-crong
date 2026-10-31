package com.hyundaiapp.cruisebreaker.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hyundaiapp.cruisebreaker.ui.SettingsUiState
import com.hyundaiapp.cruisebreaker.ui.components.CarCard
import com.hyundaiapp.cruisebreaker.ui.components.CarIcons
import com.hyundaiapp.cruisebreaker.ui.components.ScreenHeader
import com.hyundaiapp.cruisebreaker.ui.components.SelectableButton
import com.hyundaiapp.cruisebreaker.ui.theme.CarColors
import com.hyundaiapp.cruisebreaker.ui.theme.CarDimens
import com.hyundaiapp.cruisebreaker.ui.theme.CruiseBreakerTheme

/** 안내 거리 선택지(m). 이민하와 확정 전 초안 — PRD 5.3 안내 시점은 0.8~1.2km */
val ANNOUNCE_DISTANCE_PRESETS_M = listOf(500, 1_000, 2_000)

/** S4 설정(정차 중) */
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onVoiceEnabledChange: (Boolean) -> Unit,
    onAnnounceDistanceChange: (Int) -> Unit,
    onConsentClick: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = CarDimens.ScreenPadding, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        ScreenHeader("설정", onBack = onBack)
        Surface(
            color = CarColors.Surface,
            shape = RoundedCornerShape(CarDimens.CardRadius),
            modifier = Modifier.fillMaxWidth().toggleable(
                value = state.voiceEnabled,
                role = Role.Switch,
                onValueChange = onVoiceEnabledChange,
            ),
        ) {
            Row(
                Modifier.padding(horizontal = 32.dp, vertical = 28.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("음성 안내", style = MaterialTheme.typography.titleLarge)
                    Text("끄면 주행 화면 배너만 보여요", style = MaterialTheme.typography.bodyMedium, color = CarColors.OnSurfaceVariant)
                }
                Switch(
                    checked = state.voiceEnabled,
                    onCheckedChange = null,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = CarColors.Primary,
                        checkedThumbColor = CarColors.OnAccent,
                        uncheckedTrackColor = CarColors.Track,
                        uncheckedThumbColor = CarColors.OnSurfaceVariant,
                        uncheckedBorderColor = CarColors.Outline,
                    ),
                    modifier = Modifier.scale(1.5f).padding(horizontal = 12.dp),
                )
            }
        }
        CarCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 32.dp, vertical = 28.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("안내 거리", style = MaterialTheme.typography.titleLarge)
                Text("위험 구간에 들어가기 얼마 전에 알려 줄지", style = MaterialTheme.typography.bodyMedium, color = CarColors.OnSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ANNOUNCE_DISTANCE_PRESETS_M.forEach { m ->
                    SelectableButton(
                        text = if (m >= 1_000) "${m / 1_000}km" else "${m}m",
                        selected = state.announceDistanceM == m,
                        onClick = { onAnnounceDistanceChange(m) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        Surface(
            color = CarColors.Surface,
            shape = RoundedCornerShape(CarDimens.CardRadius),
            modifier = Modifier.fillMaxWidth().heightIn(min = 88.dp).clickable(onClick = onConsentClick),
        ) {
            Row(Modifier.padding(horizontal = 32.dp, vertical = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("위치정보·개인정보 동의 내용", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Icon(CarIcons.ChevronRight, contentDescription = null, tint = CarColors.OnSurfaceVariant, modifier = Modifier.size(30.dp))
            }
        }
    }
}

@Preview(name = "S4 설정", widthDp = 1280, heightDp = 720)
@Composable
private fun SettingsPreview() {
    CruiseBreakerTheme { Surface { SettingsScreen(SettingsUiState(), {}, {}, {}, {}) } }
}
