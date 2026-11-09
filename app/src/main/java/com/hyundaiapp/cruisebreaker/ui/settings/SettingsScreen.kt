package com.hyundaiapp.cruisebreaker.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import com.hyundaiapp.cruisebreaker.ui.components.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hyundaiapp.cruisebreaker.ui.SettingsUiState
import com.hyundaiapp.cruisebreaker.ui.components.CarIcons
import com.hyundaiapp.cruisebreaker.ui.components.ScreenHeader
import com.hyundaiapp.cruisebreaker.ui.components.SectionLabel
import com.hyundaiapp.cruisebreaker.ui.theme.CarColors
import com.hyundaiapp.cruisebreaker.ui.theme.CarDimens
import com.hyundaiapp.cruisebreaker.ui.theme.CruiseBreakerTheme

/** S4 설정(정차 중) */
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onVoiceEnabledChange: (Boolean) -> Unit,
    onAutoRecordEnabledChange: (Boolean) -> Unit,
    onConsentClick: () -> Unit,
    onBack: () -> Unit,
    onWithdrawConsent: () -> Unit,
    overlaySettings: @Composable () -> Unit = {},
) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = CarDimens.ScreenPadding, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        ScreenHeader("설정", onBack = onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            SectionLabel("안내")
            ToggleSetting("음성 안내", state.voiceEnabled, onVoiceEnabledChange)
            overlaySettings()
            SectionLabel("기록")
            ToggleSetting("자동 기록", state.autoRecordEnabled, onAutoRecordEnabledChange, description = "시동을 걸 때 기록을 자동으로 시작해요.\n끄더라도 진행 중인 기록은 계속돼요.")
            SectionLabel("동의")
            Surface(
                color = CarColors.Surface,
                contentColor = CarColors.OnSurface,
                shape = RoundedCornerShape(CarDimens.CardRadius),
                modifier = Modifier.fillMaxWidth().heightIn(min = 88.dp).clickable(onClick = onConsentClick),
            ) {
                Row(Modifier.padding(horizontal = 32.dp, vertical = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("위치정보·개인정보 동의 내용", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Icon(CarIcons.ChevronRight, contentDescription = null, tint = CarColors.OnSurfaceVariant, modifier = Modifier.size(30.dp))
                }
            }
            // 철회는 확인 창에서 한 번 더 확인한다(CruiseBreakerNav).
            Surface(
                color = CarColors.Surface,
                contentColor = CarColors.OnSurface,
                shape = RoundedCornerShape(CarDimens.CardRadius),
                modifier = Modifier.fillMaxWidth().heightIn(min = 88.dp).clickable(onClick = onWithdrawConsent),
            ) {
                Row(Modifier.padding(horizontal = 32.dp, vertical = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("동의 철회", style = MaterialTheme.typography.titleMedium, color = CarColors.Warning, modifier = Modifier.weight(1f))
                    Icon(CarIcons.ChevronRight, contentDescription = null, tint = CarColors.OnSurfaceVariant, modifier = Modifier.size(30.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** 전체 줄을 하나의 Switch로 읽고 누른다. */
@Composable
internal fun ToggleSetting(title: String, checked: Boolean, onChange: (Boolean) -> Unit, description: String? = null) {
    Surface(
        color = CarColors.Surface,
        contentColor = CarColors.OnSurface,
        shape = RoundedCornerShape(CarDimens.CardRadius),
        modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp).toggleable(
            value = checked, role = Role.Switch, onValueChange = onChange,
        ),
    ) {
        Row(Modifier.padding(horizontal = 32.dp, vertical = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                if (description != null) Text(description, style = MaterialTheme.typography.bodyMedium, color = CarColors.OnSurfaceVariant)
            }
            Switch(
                checked = checked,
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
}

@Preview(name = "S4 설정", widthDp = 1280, heightDp = 720)
@Composable
private fun SettingsPreview() {
    CruiseBreakerTheme { Surface { SettingsScreen(SettingsUiState(), {}, {}, {}, {}, {}) } }
}
