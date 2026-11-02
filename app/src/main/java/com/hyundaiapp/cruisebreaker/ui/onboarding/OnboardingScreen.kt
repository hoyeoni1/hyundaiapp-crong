package com.hyundaiapp.cruisebreaker.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hyundaiapp.cruisebreaker.ui.OnboardingUiState
import com.hyundaiapp.cruisebreaker.ui.components.ConsentRow
import com.hyundaiapp.cruisebreaker.ui.components.OnboardingStartAction
import com.hyundaiapp.cruisebreaker.ui.theme.CarColors
import com.hyundaiapp.cruisebreaker.ui.theme.CarDimens
import com.hyundaiapp.cruisebreaker.ui.theme.CruiseBreakerTheme

enum class Terms { LOCATION, PRIVACY }

/**
 * S5 첫 실행 동의(정차 중). 앱 개발 규정: [동의] 버튼을 누르는 것만 동의로 본다.
 * 차속 권한(CAR_SPEED)은 AAOS가 "위치 정보" 대화상자로 띄우므로(PRD 8) 요청 전에 이유를 먼저 보여 준다.
 */
@Composable
fun OnboardingScreen(
    state: OnboardingUiState,
    onAgreeLocation: () -> Unit,
    onAgreePrivacy: () -> Unit,
    onRequestSpeedPermission: () -> Unit,
    onShowTerms: (Terms) -> Unit,
    onStart: () -> Unit,
    notificationsGranted: Boolean = false,
    onRequestNotifications: () -> Unit = {},
) {
    val canStart = state.locationAgreed && state.privacyAgreed
    Row(
        Modifier.fillMaxSize().padding(horizontal = CarDimens.ScreenPadding, vertical = 40.dp),
        horizontalArrangement = Arrangement.spacedBy(40.dp),
    ) {
        Column(Modifier.weight(2f).fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically)) {
            Text("크루즈브레이커", style = MaterialTheme.typography.bodyLarge, color = CarColors.Primary, fontWeight = FontWeight.Medium)
            Text("다른 차들이\n크루즈를 끈 곳을\n미리 알려 드려요", style = MaterialTheme.typography.headlineMedium)
            Text(
                "정확한 좌표 대신\n약 150m 구간 번호만 보내고,\n시각은 시간 단위로만 저장해요.",
                style = MaterialTheme.typography.bodyLarge,
                color = CarColors.OnSurfaceVariant,
            )
            // 자동 기록은 동의 후 기본 켜짐, S4에서 끈다(docs/background-freeze-experiment.md 결정 3·4)
            Text(
                "시동을 걸면 앱을 열지 않아도\n주행을 자동으로 기록합니다.",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        Column(
            Modifier.weight(3f).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ConsentRow(
                    "위치정보 이용 (필수)", state.locationAgreed, onAgreeLocation,
                    onShowTerms = { onShowTerms(Terms.LOCATION) },
                )
                ConsentRow(
                    "개인정보 수집·이용 (필수)", state.privacyAgreed, onAgreePrivacy,
                    onShowTerms = { onShowTerms(Terms.PRIVACY) },
                )
                ConsentRow(
                    "차속 읽기 권한",
                    state.speedPermissionGranted,
                    onRequestSpeedPermission,
                    description = "다음 창에 '위치 정보'로 표시돼요.\n느린 속도의 해제를 거르려고 차속만 읽어요.",
                    agreeLabel = "권한 요청",
                    agreedLabel = "허용됨",
                )
                ConsentRow(
                    "주행 기록 알림 (선택)", notificationsGranted, onRequestNotifications,
                    description = "기록 중에는 '주행 기록 중' 알림을 표시해요.\n허용하지 않아도 주행 기록은 계속돼요.",
                    agreeLabel = "알림 허용", agreedLabel = "허용됨",
                )
            }
            OnboardingStartAction(
                enabled = canStart,
                hint = "필수 항목에 동의해 주세요",
                onStart = onStart,
            )
        }
    }
}

@Preview(name = "S5 첫 실행", widthDp = 1280, heightDp = 720)
@Composable
private fun OnboardingPreview() {
    CruiseBreakerTheme { Surface { OnboardingScreen(OnboardingUiState(locationAgreed = true), {}, {}, {}, {}, {}) } }
}
