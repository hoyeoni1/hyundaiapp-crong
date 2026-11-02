package com.hyundaiapp.cruisebreaker.ui

import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import com.hyundaiapp.cruisebreaker.RecordingService
import com.hyundaiapp.cruisebreaker.ui.components.rememberNotificationPermission
import com.hyundaiapp.cruisebreaker.ui.debug.SignalMonitorScreen
import com.hyundaiapp.cruisebreaker.ui.driving.DrivingScreen
import com.hyundaiapp.cruisebreaker.ui.driving.DrivingViewModel
import com.hyundaiapp.cruisebreaker.ui.onboarding.OnboardingScreen
import com.hyundaiapp.cruisebreaker.ui.onboarding.OnboardingViewModel
import com.hyundaiapp.cruisebreaker.ui.onboarding.Terms
import com.hyundaiapp.cruisebreaker.ui.risk.RiskListScreen
import com.hyundaiapp.cruisebreaker.ui.risk.RiskListViewModel
import com.hyundaiapp.cruisebreaker.ui.settings.SettingsScreen
import com.hyundaiapp.cruisebreaker.ui.settings.SettingsViewModel
import com.hyundaiapp.cruisebreaker.ui.summary.TripSummaryScreen
import com.hyundaiapp.cruisebreaker.ui.summary.TripSummaryViewModel
import com.hyundaiapp.cruisebreaker.ui.theme.CarColors
import com.hyundaiapp.cruisebreaker.ui.theme.CarDimens

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
    val stack = remember { mutableStateListOf(initialScreen(BuildConfig.DEBUG, container.prefs.onboarded)) }
    val go: (Screen) -> Unit = { stack.add(it) }
    val back: () -> Unit = { if (stack.size > 1) stack.removeAt(stack.lastIndex) }
    BackHandler(enabled = stack.size > 1) { back() }

    var terms by remember { mutableStateOf<Terms?>(null) }
    var confirmWithdraw by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val (notificationsGranted, requestNotifications) = rememberNotificationPermission()
    val onboardingVm = viewModel {
        OnboardingViewModel(
            container.prefs,
            speedPermissionGranted = { context.checkSelfPermission(SPEED_PERMISSION) == PackageManager.PERMISSION_GRANTED },
            onStarted = { RecordingService.startIfAllowed(context) },
        )
    }
    val speedPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        onboardingVm.onSpeedPermissionResult(granted)
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
            Screen.SIGNALS -> {
                // 신호 수신은 동의 후 기록 서비스가 시작한다. 개발 메뉴의 신호 모니터만 들어올 때 따로 켠다
                DisposableEffect(container) {
                    container.signals.start()
                    onDispose { if (!container.prefs.onboarded) container.signals.stop() }
                }
                SignalMonitorScreen(container.signals)
            }
            Screen.RISK_LIST -> {
                val vm = viewModel { RiskListViewModel(container.signals, container.risks) }
                // 화면에 들어올 때마다 새로 조회한다(방금 남긴 해제가 반영되게)
                LaunchedEffect(Unit) { vm.refresh() }
                val state by vm.state.collectAsState()
                RiskListScreen(
                    state,
                    onSummaryClick = { go(Screen.TRIP_SUMMARY) },
                    onSettingsClick = { go(Screen.SETTINGS) },
                )
            }
            Screen.TRIP_SUMMARY -> {
                val vm = viewModel { TripSummaryViewModel(container.tripStats) }
                val state by vm.state.collectAsState()
                TripSummaryScreen(state, onShowRisks = { go(Screen.RISK_LIST) }, onDone = back)
            }
            Screen.SETTINGS -> {
                val vm = viewModel { SettingsViewModel(container.prefs, onWithdrawConsent = container::withdrawConsent) }
                val state by vm.state.collectAsState()
                SettingsScreen(
                    state,
                    onVoiceEnabledChange = vm::setVoiceEnabled,
                    onAutoRecordEnabledChange = vm::setAutoRecordEnabled,
                    onConsentClick = { go(Screen.ONBOARDING) },
                    onBack = back,
                    onWithdrawConsent = { confirmWithdraw = true },
                )
                if (confirmWithdraw) {
                    WithdrawDialog(
                        onConfirm = {
                            confirmWithdraw = false
                            vm.withdrawConsent()
                            stack.clear()
                            stack.add(initialScreen(BuildConfig.DEBUG, container.prefs.onboarded))
                        },
                        onDismiss = { confirmWithdraw = false },
                    )
                }
            }
            Screen.ONBOARDING -> {
                LaunchedEffect(Unit) { onboardingVm.reload() }
                val state by onboardingVm.state.collectAsState()
                OnboardingScreen(
                    state,
                    onAgreeLocation = onboardingVm::agreeLocation,
                    onAgreePrivacy = onboardingVm::agreePrivacy,
                    onRequestSpeedPermission = { speedPermission.launch(SPEED_PERMISSION) },
                    onShowTerms = { terms = it },
                    notificationsGranted = notificationsGranted,
                    onRequestNotifications = requestNotifications,
                    onStart = {
                        if (onboardingVm.start()) {
                            if (stack.size > 1) back() else stack[0] = Screen.DRIVING
                        }
                    },
                )
            }
        }
    }

    terms?.let { TermsDialog(it, onDismiss = { terms = null }) }
}

/** 동의 철회 확인. 취소·바깥 누르기는 철회 아님 */
@Composable
private fun WithdrawDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CarColors.Surface,
        titleContentColor = CarColors.OnSurface,
        textContentColor = CarColors.OnSurface,
        title = { Text("동의를 철회할까요?") },
        text = { Text("주행 기록과 안내를 멈추고, 이 차에 저장된 동의와 기록 정보를 지워요. 다시 쓰려면 동의가 필요해요.") },
        confirmButton = { TextButton(onClick = onConfirm, modifier = Modifier.heightIn(min = CarDimens.TouchTargetSmall)) { Text("철회", color = CarColors.Warning) } },
        dismissButton = { TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = CarDimens.TouchTargetSmall)) { Text("취소", color = CarColors.Primary) } },
    )
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
