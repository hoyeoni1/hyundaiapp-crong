package com.hyundaiapp.cruisebreaker.ui.onboarding

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
import com.hyundaiapp.cruisebreaker.ui.components.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hyundaiapp.cruisebreaker.ui.OnboardingUiState
import com.hyundaiapp.cruisebreaker.ui.components.ConsentRow
import com.hyundaiapp.cruisebreaker.ui.components.OnboardingStartAction
import com.hyundaiapp.cruisebreaker.ui.components.ScreenHeader
import com.hyundaiapp.cruisebreaker.ui.components.SecondaryButton
import com.hyundaiapp.cruisebreaker.ui.theme.CarColors
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
    notificationsNeedSettings: Boolean = false,
    onBack: () -> Unit,
    reviewingConsent: Boolean = false,
) {
    val canStart = state.locationAgreed && state.privacyAgreed
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 1000.dp
        val intro: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Text("크루즈브레이커", style = MaterialTheme.typography.bodyLarge, color = CarColors.Primary)
                Text("다른 차들이 크루즈를 끈\u00A0곳을 미리 알려 드려요", style = MaterialTheme.typography.headlineMedium)
                // 11-07 장호연 "글이 너무 많다" → 핵심만. 저장 방식(150m 구간·시간 단위)은 [내용 보기] 약관에 있다(TermsText)
                Text("시동을 걸면 자동으로 기록해요.\n설정에서 끌 수 있어요.", style = MaterialTheme.typography.bodyLarge, color = CarColors.OnSurfaceVariant)
            }
        }
        val consents: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ConsentRow(
                    "위치정보 이용 (필수)", state.locationAgreed, onAgreeLocation,
                    onShowTerms = { onShowTerms(Terms.LOCATION) },
                )
                ConsentRow(
                    "개인정보 수집·이용 (필수)", state.privacyAgreed, onAgreePrivacy,
                    onShowTerms = { onShowTerms(Terms.PRIVACY) },
                )
                ConsentRow(
                    "차속 읽기 권한 (선택)",
                    state.speedPermissionGranted,
                    onRequestSpeedPermission,
                    // 요청 전에 이유를 먼저 보여 준다(PRD 8: AAOS가 "위치 정보" 창으로 띄움)
                    description = "거절해도 주행 기록은 계속돼요.\n" +
                        "저속 해제를 거르는 데 써요. 다음 창엔 '위치 정보'로 보여요.",
                    agreeLabel = "권한 요청",
                    agreedLabel = "허용됨",
                )
                ConsentRow(
                    "주행 기록 알림 (선택)", notificationsGranted, onRequestNotifications,
                    description = "기록 중임을 알림으로 보여 줘요.",
                    agreeLabel = if (notificationsNeedSettings) "알림 설정" else "알림 허용", agreedLabel = "허용됨",
                )
            }
        }
        Column(Modifier.fillMaxSize().padding(horizontal = if (wide) 48.dp else 32.dp, vertical = 32.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            ScreenHeader(if (reviewingConsent) "동의 내용" else "첫 실행 동의", onBack = onBack)
            if (wide) Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                Column(Modifier.weight(2f).verticalScroll(rememberScrollState())) { intro() }
                Column(Modifier.weight(3f).verticalScroll(rememberScrollState())) { consents() }
            } else Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(24.dp)) { intro(); consents() }
            Text(
                // 보조 안내 고지는 남긴다(#25 결정: 오류 알림 대신 S5 고지 + 약관 4절 책임 제한)
                "보조 안내예요. 항상 직접 도로를 살피세요.",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = CarColors.OnSurface,
                modifier = Modifier.fillMaxWidth(),
            )
            if (reviewingConsent) SecondaryButton("설정으로 돌아가기", onBack, Modifier.fillMaxWidth())
            else OnboardingStartAction(canStart, "필수 항목에 동의해 주세요", onStart)
        }
    }
}

@Preview(name = "S5 첫 실행", widthDp = 1280, heightDp = 720)
@Composable
private fun OnboardingPreview() {
    CruiseBreakerTheme { Surface { OnboardingScreen(OnboardingUiState(locationAgreed = true), {}, {}, {}, {}, {}, onBack = {}) } }
}
