package com.hyundaiapp.cruisebreaker.ui

import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hyundaiapp.cruisebreaker.AppContainer
import com.hyundaiapp.cruisebreaker.BuildConfig
import com.hyundaiapp.cruisebreaker.ui.debug.SignalMonitorScreen
import com.hyundaiapp.cruisebreaker.ui.driving.DrivingScreen
import com.hyundaiapp.cruisebreaker.ui.driving.DrivingViewModel
import com.hyundaiapp.cruisebreaker.ui.onboarding.OnboardingScreen
import com.hyundaiapp.cruisebreaker.ui.onboarding.Terms
import com.hyundaiapp.cruisebreaker.ui.risk.RiskListScreen
import com.hyundaiapp.cruisebreaker.ui.settings.SettingsScreen
import com.hyundaiapp.cruisebreaker.ui.summary.TripSummaryScreen

/** PRD 5.5 화면 목록. 디버그 빌드는 개발 메뉴에서 시작한다 */
enum class Screen(val title: String) {
    DEV_MENU("개발 메뉴"),
    DRIVING("S1 주행 화면"),
    RISK_LIST("S2 위험 구간 목록"),
    TRIP_SUMMARY("S3 주행 후 요약"),
    SETTINGS("S4 설정"),
    ONBOARDING("S5 첫 실행 동의"),
    SIGNALS("신호 모니터"),
}

/** 간단한 화면 스택. 화면이 늘면 navigation-compose로 바꿔도 된다(길민종) */
@Composable
fun CruiseBreakerNav(container: AppContainer) {
    val stack = remember { mutableStateListOf(if (BuildConfig.DEBUG) Screen.DEV_MENU else Screen.DRIVING) }
    val go: (Screen) -> Unit = { stack.add(it) }
    val back: () -> Unit = { if (stack.size > 1) stack.removeAt(stack.lastIndex) }
    BackHandler(enabled = stack.size > 1) { back() }

    // S4·S5는 아직 ViewModel이 없다 → 화면 동작 확인용으로 여기서 상태를 들고 있는다(이민하 ViewModel로 교체)
    var settings by remember { mutableStateOf(SettingsUiState()) }
    var onboarding by remember { mutableStateOf(OnboardingUiState()) }
    var terms by remember { mutableStateOf<Terms?>(null) }
    val context = LocalContext.current
    val speedPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        onboarding = onboarding.copy(speedPermissionGranted = granted)
        container.signals.onPermissionsChanged()
    }

    Surface(Modifier.fillMaxSize()) {
        when (stack.last()) {
            Screen.DEV_MENU -> DevMenu(go)
            Screen.DRIVING -> {
                val vm = viewModel { DrivingViewModel(container.signals, container.announcer) }
                val state by vm.state.collectAsState()
                DrivingScreen(state)
            }
            Screen.SIGNALS -> SignalMonitorScreen(container.signals)
            // 아래 S2·S3은 아직 ViewModel이 없다 → 미리보기 데이터로 그린다
            Screen.RISK_LIST -> RiskListScreen(
                PreviewData.riskList,
                onSummaryClick = { go(Screen.TRIP_SUMMARY) },
                onSettingsClick = { go(Screen.SETTINGS) },
            )
            Screen.TRIP_SUMMARY -> TripSummaryScreen(
                PreviewData.tripSummary,
                onShowRisks = { go(Screen.RISK_LIST) },
                onDone = back,
            )
            Screen.SETTINGS -> SettingsScreen(
                settings,
                onVoiceEnabledChange = { settings = settings.copy(voiceEnabled = it) },
                onAnnounceDistanceChange = { settings = settings.copy(announceDistanceM = it) },
                onConsentClick = { go(Screen.ONBOARDING) },
                onBack = back,
            )
            Screen.ONBOARDING -> {
                val granted = context.checkSelfPermission(SPEED_PERMISSION) == PackageManager.PERMISSION_GRANTED
                OnboardingScreen(
                    onboarding.copy(speedPermissionGranted = onboarding.speedPermissionGranted || granted),
                    onAgreeLocation = { onboarding = onboarding.copy(locationAgreed = true) },
                    onAgreePrivacy = { onboarding = onboarding.copy(privacyAgreed = true) },
                    onRequestSpeedPermission = { speedPermission.launch(SPEED_PERMISSION) },
                    onShowTerms = { terms = it },
                    onStart = back,
                )
            }
        }
    }

    terms?.let { TermsDialog(it, onDismiss = { terms = null }) }
}

/** 약관 전문 자리. 문구는 장호연이 정리하면 바꾼다. 닫기는 동의가 아니다 */
@Composable
private fun TermsDialog(terms: Terms, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (terms == Terms.LOCATION) "위치정보 이용" else "개인정보 수집·이용") },
        text = { Text("[약관 전문 준비 중]") },
        confirmButton = { TextButton(onClick = onDismiss) { Text("닫기") } },
    )
}

@Composable
private fun DevMenu(go: (Screen) -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("크루즈브레이커 개발 메뉴 (디버그 빌드에만 보임)", style = MaterialTheme.typography.headlineMedium)
        Screen.entries.filter { it != Screen.DEV_MENU }.forEach { s ->
            Button(onClick = { go(s) }) { Text(s.title) }
        }
    }
}

private const val SPEED_PERMISSION = "android.car.permission.CAR_SPEED"
